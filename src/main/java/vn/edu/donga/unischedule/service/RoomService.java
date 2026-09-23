package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.Classroom;
import vn.edu.donga.unischedule.model.Equipment;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.model.TimeSlot;
import vn.edu.donga.unischedule.model.RoomAvailability;
import vn.edu.donga.unischedule.model.Enums.ScheduleStatus;
import vn.edu.donga.unischedule.model.Enums.ResourceStatus;
import vn.edu.donga.unischedule.model.Enums.RoomStatus;
import vn.edu.donga.unischedule.model.Enums.RoomType;
import vn.edu.donga.unischedule.repository.RoomRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
import vn.edu.donga.unischedule.validation.Validator;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RoomService {
    private final RoomRepository roomRepository;
    private final ScheduleService scheduleService;
    private final ConflictService conflictService;

    public RoomService(RoomRepository roomRepository, ScheduleService scheduleService, ConflictService conflictService) {
        this.roomRepository = roomRepository;
        this.scheduleService = scheduleService;
        this.conflictService = conflictService;
    }

    public List<Classroom> findAll() {
        return roomRepository.findAll();
    }

    public long countRoomsWithTimetable(LocalDate date) {
        if (roomRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcRoomRepository jdbc)
            return jdbc.countScheduledRooms(date);
        return scheduleService.findAll().stream()
                .filter(entry -> entry.getStatus() == ScheduleStatus.PUBLISHED)
                .filter(entry -> !entry.getCourseSection().getSemester().getStartDate().isAfter(date)
                        && !entry.getCourseSection().getSemester().getEndDate().isBefore(date))
                .map(entry -> entry.getRoom().getId()).distinct().count();
    }

    public Classroom save(Classroom room) {
        Validator.required(room.getCode(), "Mã phòng");
        Validator.required(room.getName(), "Tên phòng");
        Validator.required(room.getBuilding(), "Tòa nhà");
        Validator.positive(room.getCapacity(), "Sức chứa");
        boolean duplicate = roomRepository.findAll().stream()
                .anyMatch(existing -> existing.getCode().equalsIgnoreCase(room.getCode())
                        && (room.getId() == null || !existing.getId().equals(room.getId())));
        if (duplicate) {
            throw new ValidationException("Mã phòng đã tồn tại trong cơ sở dữ liệu.");
        }
        return roomRepository.save(room);
    }

    public List<Equipment> findEquipmentByRoom(Long roomId) {
        return roomRepository.findEquipmentByRoom(roomId);
    }

    public List<Equipment> findAllEquipment() {
        return roomRepository.findAllEquipment();
    }

    public Equipment saveEquipment(Equipment equipment) {
        Validator.required(equipment.getCode(), "Mã thiết bị");
        Validator.required(equipment.getName(), "Tên thiết bị");
        Validator.positive(equipment.getQuantity(), "Số lượng");
        return roomRepository.saveEquipment(equipment);
    }

    public void toggleMaintenance(Classroom room) {
        room.setRoomStatus(room.getRoomStatus() == RoomStatus.MAINTENANCE
                ? RoomStatus.AVAILABLE
                : RoomStatus.MAINTENANCE);
        roomRepository.save(room);
    }

    public void updateEquipmentQuantity(Equipment equipment, int quantity) {
        Validator.positive(quantity, "Số lượng");
        equipment.setQuantity(quantity);
        roomRepository.saveEquipment(equipment);
    }

    public void markEquipmentBroken(Equipment equipment) {
        equipment.setStatus(ResourceStatus.BROKEN);
        equipment.setCondition("Đang chờ xử lý");
        roomRepository.saveEquipment(equipment);
    }

    public List<Classroom> searchAvailableRooms(LocalDate date, TimeSlot slot, String building, RoomType type,
                                                int minCapacity, String equipmentKeyword) {
        return searchRoomAvailability(date, slot, building, type, minCapacity, equipmentKeyword).stream()
                .filter(result -> result.status() == RoomAvailability.Status.FREE)
                .map(RoomAvailability::room)
                .toList();
    }

    public List<RoomAvailability> searchRoomAvailability(LocalDate date, TimeSlot slot, String building, RoomType type,
                                                         int minCapacity, String equipmentKeyword) {
        LocalDate weekStart = date == null ? LocalDate.now() : date;
        weekStart = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<Long, List<ScheduleEntry>> schedulesByRoom = scheduleService.findByWeek(weekStart).stream()
                .collect(Collectors.groupingBy(entry -> entry.getRoom().getId()));
        Set<Long> maintenanceRooms = roomRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcRoomRepository jdbc
                ? jdbc.maintenanceRoomIds(date) : Set.of();
        String equipmentSearch = equipmentKeyword == null ? "" : equipmentKeyword.trim().toLowerCase();
        Map<Long, List<Equipment>> equipmentByRoom = equipmentSearch.isBlank() ? Map.of()
                : roomRepository.findAllEquipment().stream()
                        .collect(Collectors.groupingBy(item -> item.getClassroom().getId()));
        return roomRepository.findAll().stream()
                .filter(room -> building == null || building.equals("Tất cả") || room.getBuilding().equals(building))
                .filter(room -> type == null || room.getRoomType() == type)
                .filter(room -> minCapacity <= 0 || room.getCapacity() >= minCapacity)
                .filter(room -> equipmentSearch.isBlank()
                        || equipmentByRoom.getOrDefault(room.getId(), List.of()).stream()
                        .anyMatch(item -> item.getStatus() == ResourceStatus.ACTIVE
                                && item.getName().toLowerCase().contains(equipmentSearch)))
                .map(room -> availability(room, date, slot, schedulesByRoom.getOrDefault(room.getId(), List.of()), maintenanceRooms))
                .toList();
    }

    private RoomAvailability availability(Classroom room, LocalDate date, TimeSlot slot, List<ScheduleEntry> schedules,
                                          Set<Long> maintenanceRooms) {
        boolean maintenance = room.getRoomStatus() == RoomStatus.MAINTENANCE
                || maintenanceRooms.contains(room.getId());
        if (maintenance) return new RoomAvailability(room, RoomAvailability.Status.MAINTENANCE, null, "");
        if (room.getRoomStatus() == RoomStatus.INACTIVE)
            return new RoomAvailability(room, RoomAvailability.Status.UNAVAILABLE, null, "");
        if (date == null || slot == null) return new RoomAvailability(room,
                room.getRoomStatus() == RoomStatus.AVAILABLE ? RoomAvailability.Status.FREE : RoomAvailability.Status.UNAVAILABLE,
                room.getRoomStatus() == RoomStatus.AVAILABLE ? 0 : null, "");
        int schoolDay = date.getDayOfWeek().getValue() + 1;
        ScheduleEntry probe = new ScheduleEntry(-1L, null, room, schoolDay, slot, slot, date, date, null, "");
        List<ScheduleEntry> matching = schedules.stream()
                .filter(entry -> entry.getStatus() != ScheduleStatus.CANCELLED)
                .filter(entry -> conflictService.timeOverlaps(probe, entry))
                .toList();
        if (matching.isEmpty()) return new RoomAvailability(room,
                room.getRoomStatus() == RoomStatus.AVAILABLE ? RoomAvailability.Status.FREE : RoomAvailability.Status.UNAVAILABLE,
                room.getRoomStatus() == RoomStatus.AVAILABLE ? 0 : null, "");
        int students = matching.stream().mapToInt(entry -> entry.getCourseSection().getStudentCount()).sum();
        String classes = matching.stream().map(entry -> entry.getCourseSection().getCode()).distinct()
                .collect(Collectors.joining(", "));
        return new RoomAvailability(room, RoomAvailability.Status.OCCUPIED, students, classes);
    }
}

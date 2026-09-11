package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.Classroom;
import vn.edu.donga.unischedule.model.Equipment;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.model.TimeSlot;
import vn.edu.donga.unischedule.model.Enums.ResourceStatus;
import vn.edu.donga.unischedule.model.Enums.RoomStatus;
import vn.edu.donga.unischedule.model.Enums.RoomType;
import vn.edu.donga.unischedule.repository.RoomRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
import vn.edu.donga.unischedule.validation.Validator;

import java.time.LocalDate;
import java.util.List;

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

    public Classroom save(Classroom room) {
        Validator.required(room.getCode(), "Mã phòng");
        Validator.required(room.getName(), "Tên phòng");
        Validator.required(room.getBuilding(), "Tòa nhà");
        Validator.positive(room.getCapacity(), "Sức chứa");
        boolean duplicate = roomRepository.findAll().stream()
                .anyMatch(existing -> existing.getCode().equalsIgnoreCase(room.getCode())
                        && (room.getId() == null || !existing.getId().equals(room.getId())));
        if (duplicate) {
            throw new ValidationException("Mã phòng đã tồn tại trong dữ liệu giả.");
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
        return roomRepository.findAll().stream()
                .filter(room -> room.getRoomStatus() == RoomStatus.AVAILABLE)
                .filter(room -> building == null || building.equals("Tất cả") || room.getBuilding().equals(building))
                .filter(room -> type == null || room.getRoomType() == type)
                .filter(room -> minCapacity <= 0 || room.getCapacity() >= minCapacity)
                .filter(room -> equipmentKeyword == null || equipmentKeyword.isBlank()
                        || roomRepository.findEquipmentByRoom(room.getId()).stream()
                        .anyMatch(item -> item.getStatus() == ResourceStatus.ACTIVE
                                && item.getName().toLowerCase().contains(equipmentKeyword.toLowerCase())))
                .filter(room -> date == null || slot == null || isRoomFree(room, date, slot))
                .toList();
    }

    private boolean isRoomFree(Classroom room, LocalDate date, TimeSlot slot) {
        int schoolDay = date.getDayOfWeek().getValue() + 1;
        ScheduleEntry probe = new ScheduleEntry(-1L, null, room, schoolDay, slot, slot, date, date, null, "");
        for (ScheduleEntry entry : scheduleService.findAll()) {
            if (entry.getRoom().getId().equals(room.getId()) && conflictService.timeOverlaps(probe, entry)) {
                return false;
            }
        }
        return true;
    }
}

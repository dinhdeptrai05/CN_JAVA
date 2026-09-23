package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.service.RoomService;
import vn.edu.donga.unischedule.util.TextUtils;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RoomController {
    private final RoomService service;

    public RoomController(RoomService service) { this.service = service; }

    public List<Classroom> findAll() { return service.findAll(); }
    public long countRoomsWithTimetable(LocalDate date) { return service.countRoomsWithTimetable(date); }

    public Classroom save(Classroom room) { return service.save(room); }

    public List<Equipment> findEquipmentByRoom(Long id) { return service.findEquipmentByRoom(id); }

    public List<Equipment> findAllEquipment() { return service.findAllEquipment(); }

    public Equipment saveEquipment(Equipment equipment) { return service.saveEquipment(equipment); }

    public void toggleMaintenance(Classroom room) { service.toggleMaintenance(room); }

    public List<Classroom> searchAvailableRooms(LocalDate date, TimeSlot slot, String building, RoomType type, int minCapacity, String equipmentKeyword) { return service.searchAvailableRooms(date, slot, building, type, minCapacity, equipmentKeyword); }
    public List<RoomAvailability> searchRoomAvailability(LocalDate date, TimeSlot slot, String building, RoomType type, int minCapacity, String equipmentKeyword) { return service.searchRoomAvailability(date, slot, building, type, minCapacity, equipmentKeyword); }

    public List<Classroom> search(String keyword, String building, String type, int minCapacity, String status) {
        Map<Long, String> equipmentByRoom = new HashMap<>();
        if(keyword != null && !keyword.isBlank()) {
            Map<Long, List<String>> names = new HashMap<>();
            for(Equipment item : service.findAllEquipment()) names.computeIfAbsent(item.getClassroom().getId(), ignored -> new java.util.ArrayList<>())
                    .add(item.getName() + " x" + item.getQuantity());
            names.forEach((roomId, items) -> equipmentByRoom.put(roomId, String.join(", ", items)));
        }
        return service.findAll().stream()
                .filter(room -> building == null || building.startsWith("Tất cả") || room.getBuilding().equals(building))
                .filter(room -> type == null || type.startsWith("Tất cả") || room.getRoomType().getDisplayName().equals(type))
                .filter(room -> status == null || status.startsWith("Tất cả") || room.getRoomStatus().getDisplayName().equals(status))
                .filter(room -> minCapacity <= 0 || room.getCapacity() >= minCapacity)
                .filter(room -> keyword == null || keyword.isBlank()
                        || TextUtils.containsIgnoreAccent(room.getCode(), keyword)
                        || TextUtils.containsIgnoreAccent(room.getName(), keyword)
                        || TextUtils.containsIgnoreAccent(equipmentByRoom.getOrDefault(room.getId(), ""), keyword))
                .toList();
    }

    public String equipmentSummary(Classroom room) {
        return service.findEquipmentByRoom(room.getId()).stream().limit(3)
                .map(item -> item.getName() + " x" + item.getQuantity()).collect(java.util.stream.Collectors.joining(", "));
    }
    public void updateEquipmentQuantity(Equipment equipment, String quantity) {
        service.updateEquipmentQuantity(equipment, Integer.parseInt(quantity.trim()));
    }
    public void markEquipmentBroken(Equipment equipment) { service.markEquipmentBroken(equipment); }
    public List<String> buildings() { return service.findAll().stream().map(Classroom::getBuilding).distinct().toList(); }
    public long count(RoomStatus status) { return service.findAll().stream().filter(room -> room.getRoomStatus() == status).count(); }
    public String equipmentDetails(Classroom room) {
        String text = service.findEquipmentByRoom(room.getId()).stream().map(item -> item.getName() + " x" + item.getQuantity())
                .collect(java.util.stream.Collectors.joining(", "));
        return text.isBlank() ? "Chưa khai báo" : text;
    }
}

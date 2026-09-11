package vn.edu.donga.unischedule.repository;

import vn.edu.donga.unischedule.model.Classroom;
import vn.edu.donga.unischedule.model.Equipment;

import java.util.List;

public interface RoomRepository extends Repository<Classroom> {
    List<Equipment> findEquipmentByRoom(Long roomId);

    List<Equipment> findAllEquipment();

    Equipment saveEquipment(Equipment equipment);
}

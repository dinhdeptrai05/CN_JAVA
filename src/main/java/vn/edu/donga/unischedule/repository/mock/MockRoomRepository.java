package vn.edu.donga.unischedule.repository.mock;

import vn.edu.donga.unischedule.model.Classroom;
import vn.edu.donga.unischedule.model.Equipment;
import vn.edu.donga.unischedule.repository.RoomRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MockRoomRepository implements RoomRepository {
    private final MockDataStore store;

    public MockRoomRepository(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<Classroom> findAll() {
        return new ArrayList<>(store.rooms());
    }

    @Override
    public Optional<Classroom> findById(Long id) {
        return store.rooms().stream().filter(room -> room.getId().equals(id)).findFirst();
    }

    @Override
    public Classroom save(Classroom entity) {
        if (entity.getId() == null) {
            entity.setId(store.nextRoomId());
            store.rooms().add(entity);
            return entity;
        }
        deleteById(entity.getId());
        store.rooms().add(entity);
        return entity;
    }

    @Override
    public boolean deleteById(Long id) {
        return store.rooms().removeIf(room -> room.getId().equals(id));
    }

    @Override
    public List<Equipment> findEquipmentByRoom(Long roomId) {
        return store.equipment().stream()
                .filter(item -> item.getClassroom().getId().equals(roomId))
                .toList();
    }

    @Override
    public List<Equipment> findAllEquipment() {
        return new ArrayList<>(store.equipment());
    }

    @Override
    public Equipment saveEquipment(Equipment equipment) {
        if (equipment.getId() == null) {
            equipment.setId(store.nextEquipmentId());
            store.equipment().add(equipment);
            return equipment;
        }
        store.equipment().removeIf(item -> item.getId().equals(equipment.getId()));
        store.equipment().add(equipment);
        return equipment;
    }
}

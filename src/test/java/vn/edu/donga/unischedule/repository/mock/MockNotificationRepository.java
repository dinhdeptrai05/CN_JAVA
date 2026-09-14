package vn.edu.donga.unischedule.repository.mock;

import vn.edu.donga.unischedule.model.Notification;
import vn.edu.donga.unischedule.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MockNotificationRepository implements NotificationRepository {
    private final MockDataStore store;

    public MockNotificationRepository(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<Notification> findAll() {
        return new ArrayList<>(store.notifications());
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return store.notifications().stream().filter(notification -> notification.getId().equals(id)).findFirst();
    }

    @Override
    public Notification save(Notification entity) {
        return entity;
    }

    @Override
    public boolean deleteById(Long id) {
        return store.notifications().removeIf(notification -> notification.getId().equals(id));
    }
}


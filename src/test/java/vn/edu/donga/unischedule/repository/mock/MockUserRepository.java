package vn.edu.donga.unischedule.repository.mock;

import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MockUserRepository implements UserRepository {
    private final MockDataStore store;

    public MockUserRepository(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.users());
    }

    @Override
    public Optional<User> findById(Long id) {
        return store.users().stream().filter(user -> user.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return store.users().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public User save(User entity) {
        if (entity.getId() == null) {
            entity.setId(store.nextUserId());
            store.users().add(entity);
            return entity;
        }
        deleteById(entity.getId());
        store.users().add(entity);
        return entity;
    }

    @Override
    public boolean deleteById(Long id) {
        return store.users().removeIf(user -> user.getId().equals(id));
    }
}


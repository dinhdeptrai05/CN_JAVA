package vn.edu.donga.unischedule.repository.mock;

import vn.edu.donga.unischedule.model.ChangeRequest;
import vn.edu.donga.unischedule.repository.RequestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MockRequestRepository implements RequestRepository {
    private final MockDataStore store;

    public MockRequestRepository(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<ChangeRequest> findAll() {
        return new ArrayList<>(store.requests());
    }

    @Override
    public Optional<ChangeRequest> findById(Long id) {
        return store.requests().stream().filter(request -> request.getId().equals(id)).findFirst();
    }

    @Override
    public ChangeRequest save(ChangeRequest entity) {
        if (entity.getId() == null) {
            entity.setId(store.nextRequestId());
            store.requests().add(entity);
            return entity;
        }
        deleteById(entity.getId());
        store.requests().add(entity);
        return entity;
    }

    @Override
    public boolean deleteById(Long id) {
        return store.requests().removeIf(request -> request.getId().equals(id));
    }
}

package vn.edu.donga.unischedule.repository.mock;

import vn.edu.donga.unischedule.model.CourseSection;
import vn.edu.donga.unischedule.repository.CourseSectionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MockCourseSectionRepository implements CourseSectionRepository {
    private final MockDataStore store;

    public MockCourseSectionRepository(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<CourseSection> findAll() {
        return new ArrayList<>(store.courseSections());
    }

    @Override
    public Optional<CourseSection> findById(Long id) {
        return store.courseSections().stream().filter(section -> section.getId().equals(id)).findFirst();
    }

    @Override
    public CourseSection save(CourseSection entity) {
        if (entity.getId() == null) {
            entity.setId(store.nextCourseSectionId());
            store.courseSections().add(entity);
            return entity;
        }
        deleteById(entity.getId());
        store.courseSections().add(entity);
        return entity;
    }

    @Override
    public boolean deleteById(Long id) {
        return store.courseSections().removeIf(section -> section.getId().equals(id));
    }
}

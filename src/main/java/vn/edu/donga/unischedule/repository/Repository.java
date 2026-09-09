package vn.edu.donga.unischedule.repository;

import java.util.List;
import java.util.Optional;

public interface Repository<T> {
    List<T> findAll();

    Optional<T> findById(Long id);

    T save(T entity);

    boolean deleteById(Long id);
}

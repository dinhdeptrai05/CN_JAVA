package vn.edu.donga.unischedule.repository;

import vn.edu.donga.unischedule.model.User;

import java.util.Optional;

public interface UserRepository extends Repository<User> {
    Optional<User> findByUsername(String username);
}

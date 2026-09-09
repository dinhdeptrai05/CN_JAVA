package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.validation.ValidationException;

public interface AuthService {
    User login(String username, String password) throws ValidationException;
}

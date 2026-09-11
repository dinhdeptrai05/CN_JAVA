package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.service.AuthService;
import vn.edu.donga.unischedule.util.TextUtils;
import java.time.LocalDate;
import java.util.List;

public final class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

    public User login(String username, String password) { return service.login(username, password); }
}


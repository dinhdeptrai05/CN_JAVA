package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.AuditEntry;
import vn.edu.donga.unischedule.service.AuditService;
import java.util.List;

public final class AuditController {
    private final AuditService service;
    public AuditController(AuditService service) { this.service = service; }
    public List<AuditEntry> findAll() { return service.findAll(); }
}

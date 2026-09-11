package vn.edu.donga.unischedule.model;

import java.time.LocalDateTime;

public record AuditEntry(LocalDateTime time, String user, String action, String target, String result) { }

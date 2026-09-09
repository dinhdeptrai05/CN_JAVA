package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.ConflictStatus;
import vn.edu.donga.unischedule.model.Enums.ConflictType;

public class Conflict {
    private String id;
    private ConflictType type;
    private ScheduleEntry firstSchedule;
    private ScheduleEntry secondSchedule;
    private String message;
    private ConflictStatus status;

    public Conflict(String id, ConflictType type, ScheduleEntry firstSchedule, ScheduleEntry secondSchedule,
                    String message, ConflictStatus status) {
        this.id = id;
        this.type = type;
        this.firstSchedule = firstSchedule;
        this.secondSchedule = secondSchedule;
        this.message = message;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public ConflictType getType() {
        return type;
    }

    public ScheduleEntry getFirstSchedule() {
        return firstSchedule;
    }

    public ScheduleEntry getSecondSchedule() {
        return secondSchedule;
    }

    public String getMessage() {
        return message;
    }

    public ConflictStatus getStatus() {
        return status;
    }

    public void setStatus(ConflictStatus status) {
        this.status = status;
    }
}

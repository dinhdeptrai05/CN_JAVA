package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.Priority;
import vn.edu.donga.unischedule.model.Enums.RequestStatus;
import vn.edu.donga.unischedule.model.Enums.RequestType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ChangeRequest {
    private Long id;
    private User requester;
    private RequestType type;
    private ScheduleEntry scheduleEntry;
    private Classroom desiredRoom;
    private LocalDate desiredDate;
    private TimeSlot desiredSlot;
    private String equipmentName;
    private int equipmentQuantity;
    private String reason;
    private Priority priority;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private String responseReason;

    public ChangeRequest(Long id, User requester, RequestType type, ScheduleEntry scheduleEntry,
                         Classroom desiredRoom, LocalDate desiredDate, TimeSlot desiredSlot,
                         String equipmentName, int equipmentQuantity, String reason,
                         Priority priority, RequestStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.requester = requester;
        this.type = type;
        this.scheduleEntry = scheduleEntry;
        this.desiredRoom = desiredRoom;
        this.desiredDate = desiredDate;
        this.desiredSlot = desiredSlot;
        this.equipmentName = equipmentName;
        this.equipmentQuantity = equipmentQuantity;
        this.reason = reason;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getRequester() {
        return requester;
    }

    public RequestType getType() {
        return type;
    }

    public ScheduleEntry getScheduleEntry() {
        return scheduleEntry;
    }

    public Classroom getDesiredRoom() {
        return desiredRoom;
    }

    public LocalDate getDesiredDate() {
        return desiredDate;
    }

    public TimeSlot getDesiredSlot() {
        return desiredSlot;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public int getEquipmentQuantity() {
        return equipmentQuantity;
    }

    public String getReason() {
        return reason;
    }

    public Priority getPriority() {
        return priority;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getResponseReason() {
        return responseReason;
    }

    public void setResponseReason(String responseReason) {
        this.responseReason = responseReason;
    }
}

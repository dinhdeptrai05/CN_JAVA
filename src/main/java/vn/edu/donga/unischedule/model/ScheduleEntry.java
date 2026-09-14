package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.ScheduleStatus;

import java.time.LocalDate;

public class ScheduleEntry {
    private Long id;
    private CourseSection courseSection;
    private Classroom room;
    private int dayOfWeek;
    private TimeSlot startSlot;
    private TimeSlot endSlot;
    private LocalDate startDate;
    private LocalDate endDate;
    private ScheduleStatus status;
    private String note;

    public ScheduleEntry(Long id, CourseSection courseSection, Classroom room, int dayOfWeek,
                         TimeSlot startSlot, TimeSlot endSlot, LocalDate startDate, LocalDate endDate,
                         ScheduleStatus status, String note) {
        this.id = id;
        this.courseSection = courseSection;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseSection getCourseSection() {
        return courseSection;
    }

    public void setCourseSection(CourseSection courseSection) {
        this.courseSection = courseSection;
    }

    public Classroom getRoom() {
        return room;
    }

    public void setRoom(Classroom room) {
        this.room = room;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public TimeSlot getStartSlot() {
        return startSlot;
    }

    public void setStartSlot(TimeSlot startSlot) {
        this.startSlot = startSlot;
    }

    public TimeSlot getEndSlot() {
        return endSlot;
    }

    public void setEndSlot(TimeSlot endSlot) {
        this.endSlot = endSlot;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override public boolean equals(Object other) {
        if(this==other)return true;
        if(other==null || getClass()!=other.getClass())return false;
        return getId()!=null && getId().equals(((ScheduleEntry)other).getId());
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}

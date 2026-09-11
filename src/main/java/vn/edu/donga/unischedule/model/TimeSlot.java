package vn.edu.donga.unischedule.model;

import java.time.LocalTime;

public class TimeSlot {
    private Long id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private int order;

    public TimeSlot(Long id, String name, LocalTime startTime, LocalTime endTime, int order) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return name + " (" + startTime + "-" + endTime + ")";
    }
}

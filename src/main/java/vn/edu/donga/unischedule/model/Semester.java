package vn.edu.donga.unischedule.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Semester {
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public Semester(Long id, String name, LocalDate startDate, LocalDate endDate, String status) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return name + " · " + startDate.format(SHORT_DATE);
    }

    @Override public boolean equals(Object other) {
        if(this==other)return true;
        if(other==null || getClass()!=other.getClass())return false;
        return getId()!=null && getId().equals(((Semester)other).getId());
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}

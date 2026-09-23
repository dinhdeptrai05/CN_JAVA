package vn.edu.donga.unischedule.model;

import java.time.LocalDate;
import java.util.List;

/** Activity that happened within a date window; yearly rows can include partial boundary years. */
public record HistorySnapshot(LocalDate from, LocalDate to, int activeUsers, List<Year> years) {
    public HistorySnapshot { years = List.copyOf(years); }
    public record Year(int year, long newUsers, long sections, long enrollments,
                       long cancelledEnrollments, long sessions, long requests,
                       long approvedRequests, long rejectedRequests, long pendingRequests,
                       long maintenance) { }
}

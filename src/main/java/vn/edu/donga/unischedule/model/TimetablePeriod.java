package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.Role;

import java.time.LocalDate;
import java.util.List;

/** A calendar-year term; multiple catalog semesters may share the same teaching period. */
public record TimetablePeriod(int year, int half) {
    public static final int SIMULATION_START_YEAR = 2026;
    public static final int STAFF_VISIBLE_YEARS = 5;
    public static final int USER_VISIBLE_YEARS = 3;
    public static TimetablePeriod of(LocalDate date) {
        return new TimetablePeriod(date.getYear(), date.getMonthValue() <= 6 ? 1 : 2);
    }

    public static TimetablePeriod of(Semester semester) {
        return of(semester.getStartDate());
    }

    public static boolean visibleTo(Semester semester, Role role, LocalDate today) {
        int visibleYears = role == Role.ADMIN || role == Role.ACADEMIC
                ? STAFF_VISIBLE_YEARS : USER_VISIBLE_YEARS;
        int year = semester.getStartDate().getYear();
        return year >= SIMULATION_START_YEAR && year < SIMULATION_START_YEAR + visibleYears;
    }

    public boolean includes(Semester semester) {
        return equals(of(semester));
    }

    public LocalDate firstDay(List<Semester> semesters) {
        return semesters.stream().filter(this::includes).map(Semester::getStartDate)
                .min(LocalDate::compareTo).orElseThrow();
    }

    public LocalDate lastDay(List<Semester> semesters) {
        return semesters.stream().filter(this::includes).map(Semester::getEndDate)
                .max(LocalDate::compareTo).orElseThrow();
    }

    @Override public String toString() {
        return half == 1 ? "Học kỳ 2" : "Học kỳ 1";
    }
}

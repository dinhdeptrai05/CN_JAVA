package vn.edu.donga.unischedule.simulation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Reproducible last-login dates bounded by account creation, departure and the seed date. */
public final class HistoricalLoginTimeline {
    private HistoricalLoginTimeline() { }

    public static LocalDate lastLogin(String username, LocalDate created, LocalDate left, LocalDate asOf, boolean student) {
        LocalDate earliest=created.plusDays(1);
        LocalDate latest=left==null?asOf:left.minusDays(1);
        if(latest.isBefore(earliest))return created;
        int bucket=Math.floorMod(Objects.hash(username,asOf),100);
        int offset=Math.floorMod(Objects.hash(asOf,username,"day"),Integer.MAX_VALUE);
        if(left!=null) {
            int span=(int)Math.min(180,ChronoUnit.DAYS.between(earliest,latest)+1);
            return latest.minusDays(offset%span);
        }
        int current=asOf.getYear(),year;
        if(student) year=bucket<12?created.getYear():bucket<26?current-2:bucket<48?current-1:current;
        else year=bucket<18?current-1:current;
        year=Math.max(created.getYear(),year);
        LocalDate start=LocalDate.of(year,1,1).isAfter(earliest)?LocalDate.of(year,1,1):earliest;
        LocalDate end=LocalDate.of(year,12,31).isBefore(latest)?LocalDate.of(year,12,31):latest;
        int span=(int)ChronoUnit.DAYS.between(start,end)+1;
        return start.plusDays(offset%span);
    }
}

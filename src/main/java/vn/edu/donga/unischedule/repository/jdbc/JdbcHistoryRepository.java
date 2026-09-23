package vn.edu.donga.unischedule.repository.jdbc;

import vn.edu.donga.unischedule.model.HistorySnapshot;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.repository.HistoryRepository;
import java.time.LocalDate;
import java.util.*;

/** Reads five years of activity without loading 10k+ notifications/audit entries into Swing. */
public final class JdbcHistoryRepository implements HistoryRepository {
    private final JdbcDatabase db;
    public JdbcHistoryRepository(JdbcDatabase db) {this.db=db;}
    @Override public HistorySnapshot load() {
        db.require(Role.ADMIN,Role.ACADEMIC,Role.LECTURER,Role.STUDENT);
        boolean staff=db.hasRole(Role.ADMIN)||db.hasRole(Role.ACADEMIC);
        LocalDate to=LocalDate.now(),from=to.minusYears(staff?5:3);
        return db.transaction(connection -> {
            var years=new TreeMap<Integer,long[]>();
            for(int year=from.getYear();year<=to.getYear();year++)years.put(year,new long[11]);
            accumulate(years,0,"users","created_at",from,to,null);
            accumulate(years,1,"course_sections","created_at",from,to,null);
            accumulate(years,2,"student_enrollments","enrolled_at",from,to,null);
            accumulate(years,3,"student_enrollments","enrolled_at",from,to,"status='CANCELLED'");
            accumulate(years,5,"change_requests","created_at",from,to,null);
            accumulate(years,6,"change_requests","created_at",from,to,"status='APPROVED'");
            accumulate(years,7,"change_requests","created_at",from,to,"status='REJECTED'");
            accumulate(years,8,"change_requests","created_at",from,to,"status='PENDING'");
            accumulate(years,9,"maintenance_records","start_date",from,to,null);
            // Expand weekly schedules into occurrences. Only days that have already happened count.
            String sql="WITH RECURSIVE lessons AS ("+
                    "SELECT DATE_ADD(start_date,INTERVAL MOD(CAST(day_of_week AS SIGNED)-2-WEEKDAY(start_date)+7,7) DAY) lesson_date,end_date FROM schedules WHERE status='PUBLISHED' AND start_date<=? AND end_date>=? "+
                    "UNION ALL SELECT DATE_ADD(lesson_date,INTERVAL 7 DAY),end_date FROM lessons WHERE DATE_ADD(lesson_date,INTERVAL 7 DAY)<=end_date) "+
                    "SELECT YEAR(lesson_date) yr,COUNT(*) total FROM lessons WHERE lesson_date BETWEEN ? AND ? GROUP BY YEAR(lesson_date)";
            for(var pair:db.query(sql,r->new long[]{r.getLong(1),r.getLong(2)},to,from,from,to))
                if(years.containsKey((int)pair[0]))years.get((int)pair[0])[4]=pair[1];
            int active=(int)db.scalar("SELECT COUNT(*) FROM users WHERE status='ACTIVE'");
            var rows=new ArrayList<HistorySnapshot.Year>();
            years.forEach((year,n)->rows.add(new HistorySnapshot.Year(year,n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7],n[8],n[9])));
            return new HistorySnapshot(from,to,active,rows);
        });
    }
    private void accumulate(Map<Integer,long[]> years,int index,String table,String column,LocalDate from,LocalDate to,String condition) {
        String sql="SELECT YEAR("+column+") yr,COUNT(*) total FROM `"+table+"` WHERE "+column+" >= ? AND "+column+" < DATE_ADD(?,INTERVAL 1 DAY)"+(condition==null?"":" AND "+condition)+" GROUP BY YEAR("+column+")";
        for(var pair:db.query(sql,r->new long[]{r.getLong(1),r.getLong(2)},from,to))
            if(years.containsKey((int)pair[0]))years.get((int)pair[0])[index]=pair[1];
    }
}

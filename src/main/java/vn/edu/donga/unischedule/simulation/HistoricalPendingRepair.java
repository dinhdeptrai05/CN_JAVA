package vn.edu.donga.unischedule.simulation;

import vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory;
import java.sql.*;
import java.util.*;

/** Fixes only tagged, still-pending seed proposals; existing teaching rows stay untouched. */
public final class HistoricalPendingRepair {
    private HistoricalPendingRepair() { }
    private record Pending(long id,long requester,long schedule,long room,long requestedRoom,int day,int first,int last,java.sql.Date from,java.sql.Date to) { }
    public static void main(String[] args) throws Exception {
        if(args.length!=0)throw new IllegalArgumentException("No options accepted");
        try(var c=new ConnectionFactory().open()) {
            c.setAutoCommit(false);
            try {
                var pending=new ArrayList<Pending>();
                String sql="SELECT r.id,r.requester_id,s.id,s.classroom_id,r.requested_room_id,s.day_of_week,s.start_slot_id,s.end_slot_id,s.start_date,s.end_date " +
                        "FROM change_requests r JOIN schedules s ON s.id=r.schedule_id WHERE r.status='PENDING' AND r.request_type='CHANGE_ROOM' " +
                        "AND EXISTS (SELECT 1 FROM audit_logs a WHERE a.entity_type='change_requests' AND a.entity_id=r.id AND a.action='SUBMIT_REQUEST' AND JSON_UNQUOTE(JSON_EXTRACT(a.details,'$.seed'))='HISTORY_5Y') ORDER BY r.id FOR UPDATE";
                try(var p=c.prepareStatement(sql);var rs=p.executeQuery()) {
                    while(rs.next())pending.add(new Pending(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getLong(5),rs.getInt(6),rs.getInt(7),rs.getInt(8),rs.getDate(9),rs.getDate(10)));
                }
                var chosen=new ArrayList<long[]>();int updated=0;
                for(var item:pending) {
                    String candidates="SELECT c.id FROM classrooms c JOIN classrooms old_room ON old_room.id=? JOIN schedules target_schedule ON target_schedule.id=? "+
                            "JOIN course_sections cs ON cs.id=target_schedule.course_section_id WHERE c.id<>? AND c.room_type=old_room.room_type AND c.capacity>=cs.student_count AND c.status='AVAILABLE' " +
                            "AND NOT EXISTS (SELECT 1 FROM schedules other WHERE other.id<>target_schedule.id AND other.status='PUBLISHED' AND other.classroom_id=c.id AND other.day_of_week=target_schedule.day_of_week "+
                            "AND other.start_slot_id<=target_schedule.end_slot_id AND other.end_slot_id>=target_schedule.start_slot_id AND other.start_date<=target_schedule.end_date AND other.end_date>=target_schedule.start_date) " +
                            "ORDER BY CASE WHEN c.id=? THEN -1 WHEN c.building IN ('Tòa E','Tòa F','Tòa G','Tòa H') THEN 0 ELSE 1 END,c.id";
                    Long candidate=null;
                    try(var p=c.prepareStatement(candidates)) {
                        p.setLong(1,item.room);p.setLong(2,item.schedule);p.setLong(3,item.room);
                        p.setLong(4,item.requestedRoom);
                        try(var rs=p.executeQuery()) {
                            while(rs.next()) {
                                long id=rs.getLong(1);
                                if(chosen.stream().anyMatch(other->other[0]==id&&other[1]==item.day&&other[2]<=item.last&&other[3]>=item.first
                                        &&other[4]<=item.to.getTime()&&other[5]>=item.from.getTime()))continue;
                                candidate=id;break;
                            }
                        }
                    }
                    if(candidate==null)throw new IllegalStateException("No conflict-free room for pending request "+item.id);
                    chosen.add(new long[]{candidate,item.day,item.first,item.last,item.from.getTime(),item.to.getTime()});
                    if(candidate==item.requestedRoom)continue;
                    try(var p=c.prepareStatement("UPDATE change_requests SET requested_room_id=? WHERE id=? AND requester_id=? AND status='PENDING' AND request_type='CHANGE_ROOM' AND EXISTS (SELECT 1 FROM audit_logs a WHERE a.entity_type='change_requests' AND a.entity_id=change_requests.id AND a.action='SUBMIT_REQUEST' AND JSON_UNQUOTE(JSON_EXTRACT(a.details,'$.seed'))='HISTORY_5Y')")) {
                        p.setLong(1,candidate);p.setLong(2,item.id);p.setLong(3,item.requester);
                        if(p.executeUpdate()!=1)throw new IllegalStateException("Request changed while repairing: "+item.id);
                    }
                    updated++;
                }
                if(updated>0)try(var p=c.prepareStatement("INSERT INTO audit_logs(action,entity_type,details,created_at) VALUES ('REPAIR_HISTORY_PENDING_ROOMS','change_requests',JSON_OBJECT('seed','HISTORY_5Y','count',?),UTC_TIMESTAMP())")) {p.setInt(1,updated);p.executeUpdate();}
                c.commit();System.out.println("Repaired "+updated+" pending seed room proposals; no schedule or existing non-seed request changed.");
            }catch(Exception ex){c.rollback();throw ex;}
        }
    }
}

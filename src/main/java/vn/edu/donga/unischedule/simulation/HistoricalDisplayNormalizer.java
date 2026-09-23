package vn.edu.donga.unischedule.simulation;

import vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory;
import java.sql.*;
import java.util.*;
import java.time.LocalDate;

/** One-time, transactional cleanup of human-facing text from the first history seed. */
public final class HistoricalDisplayNormalizer {
    public static final String MARKER="NORMALIZE_HISTORY_DISPLAY_V1";
    private HistoricalDisplayNormalizer() { }

    public static void main(String[] args) throws Exception {
        if(args.length!=0)throw new IllegalArgumentException("No options accepted");
        try(var c=new ConnectionFactory().open()) {
            boolean changed=apply(c);
            System.out.println(changed?"Normalized historical display text; relationships and non-seed rows unchanged.":"ALREADY_NORMALIZED: no rows changed.");
        }
    }

    public static boolean apply(Connection c) throws Exception {
        c.setAutoCommit(false);
        try {
            if(count(c,"SELECT COUNT(*) FROM audit_logs WHERE action=?",HistoricalSeedImporter.MARKER)!=1)
                throw new IllegalStateException("The completed five-year seed marker is required");
            if(count(c,"SELECT COUNT(*) FROM audit_logs WHERE action=?",MARKER)>0) {c.rollback();return false;}
            // Confirm the exact legacy seed before changing any display field.
            if(count(c,"SELECT COUNT(*) FROM users WHERE username LIKE 'hist5\\_%'")!=536 ||
                    count(c,"SELECT COUNT(*) FROM classrooms WHERE code LIKE 'HS5-%'")!=20 ||
                    count(c,"SELECT COUNT(*) FROM courses WHERE code LIKE 'HS5-%'")!=24 ||
                    count(c,"SELECT COUNT(*) FROM semesters WHERE code LIKE 'HS5-%'")!=11)
                throw new IllegalStateException("Legacy seed differs from the expected 2026-09-23 import; no changes made");

            execute(c,"UPDATE notifications SET title=REPLACE(title,'[Mẫu 5N] ',''), content=REPLACE(content,'. Đây là dữ liệu seed lịch sử.','.') WHERE title LIKE '[Mẫu 5N]%' AND user_id IN (SELECT entity_id FROM audit_logs WHERE action='CREATE_USER' AND entity_type='users' AND JSON_UNQUOTE(JSON_EXTRACT(details,'$.seed'))='HISTORY_5Y')");
            execute(c,"UPDATE schedules SET note=SUBSTRING(note,14) WHERE note LIKE '[HISTORY_5Y] %' AND id IN (SELECT entity_id FROM audit_logs WHERE entity_type='schedules' AND JSON_UNQUOTE(JSON_EXTRACT(details,'$.seed'))='HISTORY_5Y')");
            execute(c,"UPDATE change_requests SET reason=SUBSTRING(reason,14), review_reason=CASE WHEN review_reason LIKE '[HISTORY_5Y] %' THEN SUBSTRING(review_reason,14) ELSE review_reason END WHERE reason LIKE '[HISTORY_5Y] %' AND id IN (SELECT entity_id FROM audit_logs WHERE entity_type='change_requests' AND JSON_UNQUOTE(JSON_EXTRACT(details,'$.seed'))='HISTORY_5Y')");
            execute(c,"UPDATE maintenance_records SET description=SUBSTRING(description,14) WHERE description LIKE '[HISTORY_5Y] %' AND id IN (SELECT entity_id FROM audit_logs WHERE entity_type='maintenance_records' AND JSON_UNQUOTE(JSON_EXTRACT(details,'$.seed'))='HISTORY_5Y')");
            execute(c,"UPDATE classroom_equipment ce JOIN classrooms room ON room.id=ce.classroom_id SET ce.condition_note=REPLACE(ce.condition_note,' - dữ liệu mẫu',''),ce.updated_at=ce.updated_at WHERE room.code LIKE 'HS5-%' AND ce.condition_note LIKE '%dữ liệu mẫu%'");

            for(var row:rows(c,"SELECT id,code FROM courses WHERE code LIKE 'HS5-C%' ORDER BY id")) {
                String old=(String)row.get("code");int department=old.charAt(5)-'1',subject=old.charAt(6)-'1';
                update(c,"UPDATE courses SET code=?,name=REPLACE(name,' [Mẫu 5N]','') WHERE id=? AND code=?",HistoricalDisplayNames.courseCode(department,subject),row.get("id"),old);
            }
            for(var row:rows(c,"SELECT cs.id,cs.code,c.code AS course_code FROM course_sections cs JOIN courses c ON c.id=cs.course_id WHERE cs.code LIKE 'HS5-%' ORDER BY cs.id")) {
                String old=(String)row.get("code");String[] parts=old.substring(4).split("-");
                int year=Integer.parseInt(parts[0].substring(0,4)),half=Integer.parseInt(parts[0].substring(4)),index=Integer.parseInt(parts[1]);
                update(c,"UPDATE course_sections SET code=? WHERE id=? AND code=?",HistoricalDisplayNames.sectionCode((String)row.get("course_code"),year,half,index),row.get("id"),old);
            }
            for(var row:rows(c,"SELECT id,code FROM semesters WHERE code LIKE 'HS5-%' ORDER BY id")) {
                String old=(String)row.get("code");String[] parts=old.substring(4).split("-");int year=Integer.parseInt(parts[0]),half=Integer.parseInt(parts[1]);
                update(c,"UPDATE semesters SET code=?,name=? WHERE id=? AND code=?",HistoricalDisplayNames.semesterCode(year,half),HistoricalDisplayNames.semesterName(year,half),row.get("id"),old);
            }
            for(var row:rows(c,"SELECT id,code FROM equipment WHERE code LIKE 'HS5-E%' ORDER BY id")) {
                String old=(String)row.get("code");String[] parts=old.substring(5).split("-");int room=Integer.parseInt(parts[0]),equipment=Integer.parseInt(parts[1])+1;
                update(c,"UPDATE equipment SET code=?,category=REPLACE(category,' [Mẫu 5N]','') WHERE id=? AND code=?",HistoricalDisplayNames.equipmentCode(HistoricalDisplayNames.roomCode(room),equipment),row.get("id"),old);
            }
            for(var row:rows(c,"SELECT id,code FROM classrooms WHERE code LIKE 'HS5-R%' ORDER BY id")) {
                String old=(String)row.get("code");int room=Integer.parseInt(old.substring(5));String code=HistoricalDisplayNames.roomCode(room);
                update(c,"UPDATE classrooms SET code=?,name=?,building=?,description='Phòng học phục vụ giảng dạy và thực hành' WHERE id=? AND code=?",code,"Phòng "+code,HistoricalDisplayNames.building(room),row.get("id"),old);
            }
            for(var row:rows(c,"SELECT u.id,u.username,u.created_at,d.code AS department_code FROM users u LEFT JOIN departments d ON d.id=u.department_id WHERE u.username LIKE 'hist5\\_%' ORDER BY u.id")) {
                String old=(String)row.get("username");int year=Integer.parseInt(row.get("created_at").toString().substring(0,4));
                String login=HistoricalDisplayNames.username(old,year);boolean student=old.startsWith("hist5_sv"),lecturer=old.startsWith("hist5_gv");
                String classCode=student?HistoricalDisplayNames.classCode((String)row.get("department_code"),year):null;
                update(c,"UPDATE users SET username=?,full_name=REPLACE(full_name,'[Mẫu 5N] ',''),email=CASE WHEN email LIKE '%@example.invalid' THEN ? ELSE email END,"+
                        "student_code=CASE WHEN student_code LIKE 'HS5-%' THEN ? ELSE student_code END,"+
                        "lecturer_code=CASE WHEN lecturer_code LIKE 'HS5-%' THEN ? ELSE lecturer_code END,"+
                        "class_code=CASE WHEN class_code LIKE 'HS5-%' THEN ? ELSE class_code END,updated_at=updated_at WHERE id=? AND username=?",
                        login,HistoricalDisplayNames.email(login,student),student?login.toUpperCase(Locale.ROOT):null,lecturer?login.toUpperCase(Locale.ROOT):null,classCode,row.get("id"),old);
            }
            update(c,"INSERT INTO audit_logs(action,entity_type,details,created_at) VALUES (?, 'database', JSON_OBJECT('seed','HISTORY_5Y','purpose','display normalization'), UTC_TIMESTAMP())",MARKER);
            c.commit();return true;
        } catch(Exception ex) {c.rollback();throw ex;}
        finally {c.setAutoCommit(true);}
    }

    private static long count(Connection c,String sql,Object... values) throws SQLException {
        try(var p=c.prepareStatement(sql)) {for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);try(var r=p.executeQuery()){r.next();return r.getLong(1);}}
    }
    private static void execute(Connection c,String sql) throws SQLException {try(var p=c.prepareStatement(sql)){p.executeUpdate();}}
    private static void update(Connection c,String sql,Object... values) throws SQLException {
        try(var p=c.prepareStatement(sql)) {for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);if(p.executeUpdate()!=1)throw new SQLException("Display row changed during normalization");}
    }
    private static List<Map<String,Object>> rows(Connection c,String sql) throws SQLException {
        var out=new ArrayList<Map<String,Object>>();try(var p=c.prepareStatement(sql);var r=p.executeQuery()) {var meta=r.getMetaData();while(r.next()){var row=new HashMap<String,Object>();for(int i=1;i<=meta.getColumnCount();i++)row.put(meta.getColumnLabel(i),r.getObject(i));out.add(row);}}return out;
    }
}

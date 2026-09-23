package vn.edu.donga.unischedule.repository.jdbc;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.repository.RoomRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
import java.util.*;
import java.time.LocalDate;
public final class JdbcRoomRepository implements RoomRepository {
    private final JdbcDatabase db;
    public JdbcRoomRepository(JdbcDatabase db) { this.db=db; }
    public List<Classroom> findAll() { return db.query("SELECT * FROM classrooms ORDER BY code",r->new Classroom(r.getLong("id"),r.getString("code"),r.getString("name"),r.getString("building"),r.getInt("floor"),r.getInt("capacity"),RoomType.valueOf(r.getString("room_type")),RoomStatus.valueOf(r.getString("status")),r.getString("description"))); }
    public Optional<Classroom> findById(Long id) { return findAll().stream().filter(x->x.getId().equals(id)).findFirst(); }
    public Classroom save(Classroom room) {
        long id=db.transaction(c->{
            db.require(Role.ADMIN); db.lockScheduling(); Long key=room.getId();
            if(key!=null && (room.getRoomStatus()==RoomStatus.MAINTENANCE || room.getRoomStatus()==RoomStatus.INACTIVE || db.scalar("SELECT COUNT(*) FROM schedules s JOIN course_sections cs ON cs.id=s.course_section_id WHERE s.classroom_id=? AND s.status<>'CANCELLED' AND s.end_date>=UTC_DATE() AND cs.student_count>?",key,room.getCapacity())>0)) {
                if(db.scalar("SELECT COUNT(*) FROM schedules WHERE classroom_id=? AND status<>'CANCELLED' AND end_date>=UTC_DATE()",key)>0) throw new ValidationException("Phòng có lịch học đang hoạt động; cần chuyển hoặc hủy lịch trước.");
            }
            if(key==null) key=db.insert("INSERT INTO classrooms(code,name,building,floor,capacity,room_type,status,description) VALUES (?,?,?,?,?,?,?,?)",room.getCode(),room.getName(),room.getBuilding(),room.getFloor(),room.getCapacity(),room.getRoomType(),room.getRoomStatus(),room.getDescription());
            else db.update("UPDATE classrooms SET code=?,name=?,building=?,floor=?,capacity=?,room_type=?,status=?,description=? WHERE id=?",room.getCode(),room.getName(),room.getBuilding(),room.getFloor(),room.getCapacity(),room.getRoomType(),room.getRoomStatus(),room.getDescription(),key);
            if(room.getRoomStatus()==RoomStatus.MAINTENANCE && db.scalar("SELECT COUNT(*) FROM maintenance_records WHERE classroom_id=? AND status IN ('REPORTED','IN_PROGRESS')",key)==0)
                db.update("INSERT INTO maintenance_records(classroom_id,reported_by,description,start_date,status) VALUES (?,?,?,UTC_DATE(),'IN_PROGRESS')",key,db.actor().getId(),"Bảo trì phòng học");
            if(room.getRoomStatus()==RoomStatus.AVAILABLE) db.update("UPDATE maintenance_records SET status='COMPLETED',end_date=UTC_DATE() WHERE classroom_id=? AND classroom_equipment_id IS NULL AND status IN ('REPORTED','IN_PROGRESS')",key);
            db.audit("SAVE_ROOM","classrooms",key); return key;
        }); room.setId(id); return room;
    }
    public boolean deleteById(Long id) { var room=findById(id); if(room.isEmpty()) return false; room.get().setRoomStatus(RoomStatus.INACTIVE); save(room.get()); return true; }
    public List<Equipment> findAllEquipment() {
        var rooms=findAll();
        var roomById=new HashMap<Long,Classroom>();for(var room:rooms)roomById.put(room.getId(),room);
        return db.query("SELECT e.*,ce.id placement_id,ce.classroom_id,ce.quantity,ce.condition_note,ce.condition_status FROM equipment e JOIN classroom_equipment ce ON ce.equipment_id=e.id ORDER BY e.code,ce.id",r->{
            ResourceStatus status=switch(r.getString("condition_status")) { case "DAMAGED"->ResourceStatus.BROKEN; case "MAINTENANCE"->ResourceStatus.MAINTENANCE; case "UNAVAILABLE"->ResourceStatus.INACTIVE; default->ResourceStatus.ACTIVE; };
            var item=new Equipment(r.getLong("id"),r.getString("code"),r.getString("name"),r.getString("category"),r.getInt("quantity"),r.getString("condition_note"),Objects.requireNonNull(roomById.get(r.getLong("classroom_id"))),status);
            item.setPlacementId(r.getLong("placement_id")); return item;
        });
    }
    public List<Equipment> findEquipmentByRoom(Long id) { return findAllEquipment().stream().filter(e->e.getClassroom().getId().equals(id)).toList(); }
    public Equipment saveEquipment(Equipment e) {
        if(e.getClassroom()==null) throw new ValidationException("Vui lòng chọn phòng cho thiết bị.");
        long[] ids=db.transaction(c->{ db.require(Role.ADMIN); db.lockScheduling();
            Long id=e.getId();
            if(id==null) id=db.insert("INSERT INTO equipment(code,name,category,status) VALUES (?,?,?,?)",e.getCode(),e.getName(),e.getCategory(),ResourceStatus.ACTIVE);
            else db.update("UPDATE equipment SET code=?,name=?,category=? WHERE id=?",e.getCode(),e.getName(),e.getCategory(),id);
            String condition=switch(e.getStatus()) {case ACTIVE->"GOOD"; case BROKEN->"DAMAGED"; case MAINTENANCE->"MAINTENANCE"; case INACTIVE->"UNAVAILABLE";};
            Long placement=e.getPlacementId();
            if(placement==null) placement=db.insert("INSERT INTO classroom_equipment(classroom_id,equipment_id,quantity,condition_status,condition_note) VALUES (?,?,?,?,?)",e.getClassroom().getId(),id,e.getQuantity(),condition,e.getCondition());
            else db.update("UPDATE classroom_equipment SET classroom_id=?,quantity=?,condition_status=?,condition_note=? WHERE id=? AND equipment_id=?",e.getClassroom().getId(),e.getQuantity(),condition,e.getCondition(),placement,id);
            if(e.getStatus()==ResourceStatus.BROKEN || e.getStatus()==ResourceStatus.MAINTENANCE) {
                if(db.scalar("SELECT COUNT(*) FROM maintenance_records WHERE classroom_equipment_id=? AND status IN ('REPORTED','IN_PROGRESS')",placement)==0) db.update("INSERT INTO maintenance_records(classroom_equipment_id,reported_by,description,start_date,status) VALUES (?,?,?,UTC_DATE(),'REPORTED')",placement,db.actor().getId(),e.getCondition());
            } else if(e.getStatus()==ResourceStatus.ACTIVE) db.update("UPDATE maintenance_records SET status='COMPLETED',end_date=UTC_DATE() WHERE classroom_equipment_id=? AND status IN ('REPORTED','IN_PROGRESS')",placement);
            db.audit("SAVE_EQUIPMENT","classroom_equipment",placement); return new long[]{id,placement};
        }); e.setId(ids[0]); e.setPlacementId(ids[1]); return e;
    }
    public boolean hasMaintenance(Long roomId,LocalDate date) { return date!=null && db.scalar("SELECT COUNT(*) FROM maintenance_records WHERE classroom_id=? AND status IN ('REPORTED','IN_PROGRESS') AND start_date<=? AND (end_date IS NULL OR end_date>=?)",roomId,date,date)>0; }
    public Set<Long> maintenanceRoomIds(LocalDate date) {
        if(date==null)return Set.of();
        return new HashSet<>(db.query("SELECT DISTINCT classroom_id FROM maintenance_records WHERE classroom_id IS NOT NULL AND status IN ('REPORTED','IN_PROGRESS') AND start_date<=? AND (end_date IS NULL OR end_date>=?)",r->r.getLong(1),date,date));
    }
    public long countScheduledRooms(LocalDate date) {
        return db.scalar("SELECT COUNT(DISTINCT s.classroom_id) FROM schedules s JOIN course_sections cs ON cs.id=s.course_section_id JOIN semesters sm ON sm.id=cs.semester_id WHERE s.status='PUBLISHED' AND sm.start_date<=? AND sm.end_date>=?",date,date);
    }
}

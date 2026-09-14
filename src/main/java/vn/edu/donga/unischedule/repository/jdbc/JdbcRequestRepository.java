package vn.edu.donga.unischedule.repository.jdbc;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.repository.RequestRepository;
import vn.edu.donga.unischedule.validation.*;
import java.util.*;
import java.time.*;
public final class JdbcRequestRepository implements RequestRepository {
    private final JdbcDatabase db;
    public JdbcRequestRepository(JdbcDatabase db) { this.db=db; }
    public List<ChangeRequest> findAll() {
        var users=new JdbcUserRepository(db).findAll();var schedules=new JdbcScheduleRepository(db).findAll();var rooms=new JdbcRoomRepository(db).findAll();var slots=new JdbcCatalogRepository(db).getTimeSlots();
        return db.query("SELECT * FROM change_requests ORDER BY created_at DESC",r->{
            long uid=r.getLong("requester_id"),sid=r.getLong("schedule_id"),rid=r.getLong("requested_room_id"),slot=r.getLong("requested_start_slot_id");
            var req=new ChangeRequest(r.getLong("id"),users.stream().filter(x->x.getId()==uid).findFirst().orElseThrow(),RequestType.valueOf(r.getString("request_type")),schedules.stream().filter(x->x.getId()==sid).findFirst().orElse(null),rooms.stream().filter(x->x.getId()==rid).findFirst().orElse(null),r.getDate("requested_date")==null?null:r.getDate("requested_date").toLocalDate(),slots.stream().filter(x->x.getId()==slot).findFirst().orElse(null),r.getString("equipment_name"),r.getInt("quantity"),r.getString("reason"),Priority.valueOf(r.getString("priority")),RequestStatus.valueOf(r.getString("status")),r.getTimestamp("created_at").toLocalDateTime());
            req.setResponseReason(r.getString("review_reason"));return req;
        });
    }
    public Optional<ChangeRequest> findById(Long id) { return findAll().stream().filter(x->x.getId().equals(id)).findFirst(); }
    public ChangeRequest save(ChangeRequest r) {
        if(r.getId()!=null) throw new ValidationException("Dùng chức năng duyệt hoặc từ chối để xử lý yêu cầu.");
        long id=db.transaction(c->{db.require(Role.LECTURER);
            if(!r.getRequester().getId().equals(db.actor().getId()) || r.getStatus()!=RequestStatus.PENDING) throw new ValidationException("Người gửi hoặc trạng thái yêu cầu không hợp lệ.");
            Validator.reason(r.getReason(),"Lý do");validateTarget(r);
            Long equipment=null;
            if(r.getType()==RequestType.BORROW_EQUIPMENT || r.getType()==RequestType.REPORT_DAMAGE) {
                var matches=db.query("SELECT e.id FROM equipment e JOIN classroom_equipment ce ON ce.equipment_id=e.id WHERE e.name=? AND ce.classroom_id=?",rs->rs.getLong(1),r.getEquipmentName(),r.getDesiredRoom().getId());
                if(matches.size()!=1) throw new ValidationException("Tên thiết bị phải khớp một thiết bị trong phòng đã chọn.");equipment=matches.get(0);
                if(r.getEquipmentQuantity()<=0) throw new ValidationException("Số lượng thiết bị phải lớn hơn 0.");
            }
            long key=db.insert("INSERT INTO change_requests(requester_id,schedule_id,requested_room_id,requested_equipment_id,requested_date,requested_day,requested_start_slot_id,equipment_name,quantity,request_type,reason,priority,status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",r.getRequester().getId(),r.getScheduleEntry()==null?null:r.getScheduleEntry().getId(),r.getDesiredRoom()==null?null:r.getDesiredRoom().getId(),equipment,r.getDesiredDate(),r.getDesiredDate()==null?null:r.getDesiredDate().getDayOfWeek().getValue()+1,r.getDesiredSlot()==null?null:r.getDesiredSlot().getId(),r.getEquipmentName(),r.getEquipmentQuantity(),r.getType(),r.getReason(),r.getPriority(),RequestStatus.PENDING);
            db.audit("CREATE_REQUEST","change_requests",key);return key;
        });r.setId(id);return r;
    }
    private void validateTarget(ChangeRequest r) {
        if(r.getType()==RequestType.CHANGE_ROOM || r.getType()==RequestType.CHANGE_SCHEDULE) {
            if(r.getScheduleEntry()==null) throw new ValidationException("Chọn lịch cần thay đổi.");
            var schedule=new JdbcScheduleRepository(db).findById(r.getScheduleEntry().getId()).orElseThrow();
            if(!schedule.getCourseSection().getLecturer().getId().equals(r.getRequester().getId()) || schedule.getStatus()==ScheduleStatus.CANCELLED) throw new ValidationException("Chỉ được yêu cầu thay đổi lịch đang dạy của mình.");
        }
        if(r.getType()!=RequestType.CHANGE_SCHEDULE && r.getDesiredRoom()==null) throw new ValidationException("Vui lòng chọn phòng.");
        if((r.getType()==RequestType.CHANGE_SCHEDULE || r.getType()==RequestType.USE_ROOM || r.getType()==RequestType.BORROW_EQUIPMENT) && (r.getDesiredDate()==null || r.getDesiredSlot()==null)) throw new ValidationException("Vui lòng chọn ngày và ca.");
    }
    public boolean canProcess(ChangeRequest r,User actor) {
        return r.getStatus()==RequestStatus.PENDING && ((actor.getRole()==Role.ACADEMIC && (r.getType()==RequestType.CHANGE_ROOM || r.getType()==RequestType.CHANGE_SCHEDULE || r.getType()==RequestType.USE_ROOM)) || (actor.getRole()==Role.ADMIN && (r.getType()==RequestType.BORROW_EQUIPMENT || r.getType()==RequestType.REPORT_DAMAGE)));
    }
    public void process(ChangeRequest original,User actor,boolean approved,String reason) {
        db.transaction(c->{
            db.require(Role.ADMIN,Role.ACADEMIC);if(!db.actor().getId().equals(actor.getId())) throw new ValidationException("Phiên đăng nhập không khớp.");
            db.lockScheduling();db.query("SELECT id FROM change_requests WHERE id=? FOR UPDATE",r->r.getLong(1),original.getId());
            var r=findById(original.getId()).orElseThrow();
            if(!canProcess(r,new JdbcUserRepository(db).findById(db.actor().getId()).orElseThrow())) throw new ValidationException("Yêu cầu đã xử lý hoặc không thuộc quyền duyệt.");
            if(!approved) Validator.reason(reason,"Lý do từ chối");
            if(approved) {
                validateTarget(r);
                if(r.getType()==RequestType.CHANGE_ROOM || r.getType()==RequestType.CHANGE_SCHEDULE) {
                    var old=r.getScheduleEntry();var slot=r.getType()==RequestType.CHANGE_SCHEDULE?r.getDesiredSlot():old.getStartSlot();
                    int endOrder=slot.getOrder()+old.getEndSlot().getOrder()-old.getStartSlot().getOrder();
                    var end=new JdbcCatalogRepository(db).getTimeSlots().stream().filter(x->x.getOrder()==endOrder).findFirst().orElseThrow(()->new ValidationException("Ca mới vượt quá số ca trong ngày."));
                    var changed=new ScheduleEntry(old.getId(),old.getCourseSection(),r.getDesiredRoom()==null?old.getRoom():r.getDesiredRoom(),r.getType()==RequestType.CHANGE_SCHEDULE?r.getDesiredDate().getDayOfWeek().getValue()+1:old.getDayOfWeek(),slot,end,old.getStartDate(),old.getEndDate(),old.getStatus(),old.getNote());
                    new JdbcScheduleRepository(db).persist(changed);
                    db.update("INSERT INTO notifications(user_id,title,content,type,target_screen) SELECT student_id,?,?, 'SCHEDULE','timetable' FROM student_enrollments WHERE course_section_id=? AND status='ACTIVE'","Thời khóa biểu đã cập nhật","Lịch lớp "+old.getCourseSection().getCode()+" đã thay đổi.",old.getCourseSection().getId());
                } else if(r.getType()==RequestType.USE_ROOM) {
                    // A one-day reservation uses an existing assigned section so it participates in all conflict checks.
                    if(r.getScheduleEntry()==null) throw new ValidationException("Yêu cầu mượn phòng cần chọn lịch của lớp để xác định lớp và giảng viên.");
                    var old=r.getScheduleEntry();
                    if(!old.getCourseSection().getLecturer().getId().equals(r.getRequester().getId())) throw new ValidationException("Lịch không thuộc người gửi.");
                    new JdbcScheduleRepository(db).persist(new ScheduleEntry(null,old.getCourseSection(),r.getDesiredRoom(),r.getDesiredDate().getDayOfWeek().getValue()+1,r.getDesiredSlot(),r.getDesiredSlot(),r.getDesiredDate(),r.getDesiredDate(),ScheduleStatus.PUBLISHED,"Mượn phòng theo yêu cầu "+r.getId()));
                } else {
                    var items=new JdbcRoomRepository(db).findEquipmentByRoom(r.getDesiredRoom().getId());
                    var equipment=items.stream().filter(x->x.getName().equals(r.getEquipmentName())).findFirst().orElseThrow(()->new ValidationException("Không tìm thấy thiết bị trong phòng."));
                    if(r.getType()==RequestType.REPORT_DAMAGE) {
                        equipment.setStatus(ResourceStatus.BROKEN);equipment.setCondition(r.getReason());new JdbcRoomRepository(db).saveEquipment(equipment);
                    } else {
                        long reserved=db.scalar("SELECT COALESCE(SUM(quantity),0) FROM change_requests WHERE request_type='BORROW_EQUIPMENT' AND status='APPROVED' AND requested_room_id=? AND equipment_name=? AND requested_date=? AND requested_start_slot_id=?",r.getDesiredRoom().getId(),r.getEquipmentName(),r.getDesiredDate(),r.getDesiredSlot().getId());
                        if(equipment.getStatus()!=ResourceStatus.ACTIVE || equipment.getQuantity()<reserved+r.getEquipmentQuantity()) throw new ValidationException("Thiết bị không đủ số lượng khả dụng tại ca được chọn.");
                    }
                }
            }
            String response=approved?"Đã duyệt yêu cầu.":reason.trim();
            db.update("UPDATE change_requests SET status=?,reviewed_by=?,review_reason=?,reviewed_at=UTC_TIMESTAMP() WHERE id=?",approved?RequestStatus.APPROVED:RequestStatus.REJECTED,actor.getId(),response,r.getId());
            db.update("INSERT INTO notifications(user_id,title,content,type,reference_type,reference_id,target_screen) VALUES (?,?,?,'REQUEST','change_requests',?,'requests')",r.getRequester().getId(),approved?"Yêu cầu đã được duyệt":"Yêu cầu bị từ chối",response,r.getId());
            db.audit(approved?"APPROVE_REQUEST":"REJECT_REQUEST","change_requests",r.getId());return null;
        }); original.setStatus(approved?RequestStatus.APPROVED:RequestStatus.REJECTED);original.setResponseReason(approved?"Đã duyệt yêu cầu.":reason.trim());
    }
    public boolean deleteById(Long id) { throw new ValidationException("Yêu cầu được giữ lại để tra cứu lịch sử."); }
}

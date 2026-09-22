package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.ChangeRequest;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.RequestStatus;
import vn.edu.donga.unischedule.model.Enums.RequestType;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.ScheduleStatus;
import vn.edu.donga.unischedule.repository.RequestRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
import vn.edu.donga.unischedule.validation.Validator;

import java.util.List;
import java.time.LocalDate;

public class RequestService {
    private final RequestRepository requestRepository;
    private final ScheduleService scheduleService;

    public RequestService(RequestRepository requestRepository, ScheduleService scheduleService) {
        this.requestRepository = requestRepository;
        this.scheduleService = scheduleService;
    }

    public List<ChangeRequest> findForUser(User user) {
        if (user.getRole() == Role.LECTURER) {
            return requestRepository.findAll().stream()
                    .filter(request -> request.getRequester().getId().equals(user.getId()))
                    .toList();
        }
        if (user.getRole() == Role.ADMIN) {
            if (requestRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcRequestRepository) return requestRepository.findAll().stream().filter(r -> r.getType()==RequestType.BORROW_EQUIPMENT || r.getType()==RequestType.REPORT_DAMAGE).toList();
            return requestRepository.findAll().stream()
                    .filter(request -> request.getType() == RequestType.BORROW_EQUIPMENT
                            || request.getType() == RequestType.REPORT_DAMAGE)
                    .toList();
        }
        if (user.getRole() == Role.ACADEMIC) {
            if (requestRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcRequestRepository) return requestRepository.findAll().stream().filter(r -> r.getType()==RequestType.CHANGE_SCHEDULE || r.getType()==RequestType.CHANGE_ROOM || r.getType()==RequestType.USE_ROOM).toList();
            return requestRepository.findAll().stream()
                    .filter(request -> request.getType() == RequestType.CHANGE_SCHEDULE
                            || request.getType() == RequestType.CHANGE_ROOM
                            || request.getType() == RequestType.USE_ROOM)
                    .toList();
        }
        return List.of();
    }

    public ChangeRequest create(ChangeRequest request) {
        Validator.reason(request.getReason(), "Lý do yêu cầu");
        if (request.getType() == RequestType.USE_ROOM) {
            ScheduleEntry source = request.getScheduleEntry();
            if (source == null || request.getDesiredRoom() == null || request.getDesiredDate() == null || request.getDesiredSlot() == null)
                throw new ValidationException("Chọn lớp đang dạy, ngày, ca và phòng để đăng ký học bù.");
            if (!source.getCourseSection().getLecturer().getId().equals(request.getRequester().getId()))
                throw new ValidationException("Chỉ được đăng ký học bù cho lớp mình đang dạy.");
            LocalDate date = request.getDesiredDate();
            if (date.isBefore(LocalDate.now()) || date.isBefore(source.getCourseSection().getSemester().getStartDate())
                    || date.isAfter(source.getCourseSection().getSemester().getEndDate()))
                throw new ValidationException("Ngày học bù phải thuộc học kỳ và không được ở quá khứ.");
        }
        return requestRepository.save(request);
    }

    public void approve(ChangeRequest request, User actor) {
        if (requestRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcRequestRepository jdbc) { jdbc.process(request,actor,true,null); return; }
        ensureCanProcess(request, actor);
        if (request.getType() == RequestType.USE_ROOM) {
            ScheduleEntry original = request.getScheduleEntry();
            if (original == null || !original.getCourseSection().getLecturer().getId().equals(request.getRequester().getId())
                    || request.getDesiredDate() == null || request.getDesiredSlot() == null || request.getDesiredRoom() == null)
                throw new ValidationException("Yêu cầu học bù thiếu lớp, ngày, ca hoặc phòng hợp lệ.");
            LocalDate date = request.getDesiredDate();
            if (date.isBefore(LocalDate.now()) || date.isBefore(original.getCourseSection().getSemester().getStartDate())
                    || date.isAfter(original.getCourseSection().getSemester().getEndDate()))
                throw new ValidationException("Ngày học bù không thuộc học kỳ hoặc đã qua.");
            scheduleService.save(new ScheduleEntry(null, original.getCourseSection(), request.getDesiredRoom(),
                    date.getDayOfWeek().getValue() + 1, request.getDesiredSlot(), request.getDesiredSlot(),
                    date, date, ScheduleStatus.PUBLISHED, "Học bù theo yêu cầu " + request.getId()));
        }
        request.setStatus(RequestStatus.APPROVED);
        request.setResponseReason("Đã duyệt trong chế độ demo.");
        requestRepository.save(request);
    }

    public void reject(ChangeRequest request, User actor, String reason) {
        if (requestRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcRequestRepository jdbc) { jdbc.process(request,actor,false,reason); return; }
        ensureCanProcess(request, actor);
        Validator.reason(reason, "Lý do từ chối");
        request.setStatus(RequestStatus.REJECTED);
        request.setResponseReason(reason);
        requestRepository.save(request);
    }

    public boolean canProcess(ChangeRequest request, User actor) {
        if (requestRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcRequestRepository jdbc) return jdbc.canProcess(request,actor);
        if (request.getStatus() != RequestStatus.PENDING) {
            return false;
        }
        if (actor.getRole() == Role.ADMIN) {
            return request.getType() == RequestType.BORROW_EQUIPMENT
                    || request.getType() == RequestType.REPORT_DAMAGE;
        }
        return actor.getRole() == Role.ACADEMIC && (request.getType() == RequestType.CHANGE_SCHEDULE
                || request.getType() == RequestType.CHANGE_ROOM || request.getType() == RequestType.USE_ROOM);
    }

    private void ensureCanProcess(ChangeRequest request, User actor) {
        if (!canProcess(request, actor)) {
            throw new ValidationException("Tài khoản hiện tại không có quyền xử lý yêu cầu này.");
        }
    }
}

package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.ChangeRequest;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.RequestStatus;
import vn.edu.donga.unischedule.model.Enums.RequestType;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.repository.RequestRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
import vn.edu.donga.unischedule.validation.Validator;

import java.util.List;

public class RequestService {
    private final RequestRepository requestRepository;

    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
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
                    .filter(request -> request.getType() == RequestType.CHANGE_ROOM
                            || request.getType() == RequestType.BORROW_EQUIPMENT
                            || request.getType() == RequestType.REPORT_DAMAGE
                            || request.getType() == RequestType.USE_ROOM)
                    .toList();
        }
        if (user.getRole() == Role.ACADEMIC) {
            if (requestRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcRequestRepository) return requestRepository.findAll().stream().filter(r -> r.getType()==RequestType.CHANGE_SCHEDULE || r.getType()==RequestType.CHANGE_ROOM || r.getType()==RequestType.USE_ROOM).toList();
            return requestRepository.findAll().stream()
                    .filter(request -> request.getType() == RequestType.CHANGE_SCHEDULE)
                    .toList();
        }
        return List.of();
    }

    public ChangeRequest create(ChangeRequest request) {
        Validator.reason(request.getReason(), "Lý do yêu cầu");
        return requestRepository.save(request);
    }

    public void approve(ChangeRequest request, User actor) {
        if (requestRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcRequestRepository jdbc) { jdbc.process(request,actor,true,null); return; }
        ensureCanProcess(request, actor);
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
            return request.getType() == RequestType.CHANGE_ROOM
                    || request.getType() == RequestType.BORROW_EQUIPMENT
                    || request.getType() == RequestType.REPORT_DAMAGE
                    || request.getType() == RequestType.USE_ROOM;
        }
        return actor.getRole() == Role.ACADEMIC && request.getType() == RequestType.CHANGE_SCHEDULE;
    }

    private void ensureCanProcess(ChangeRequest request, User actor) {
        if (!canProcess(request, actor)) {
            throw new ValidationException("Tài khoản hiện tại không có quyền xử lý yêu cầu này.");
        }
    }
}

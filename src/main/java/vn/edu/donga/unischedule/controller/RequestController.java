package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.service.RequestService;
import vn.edu.donga.unischedule.util.TextUtils;
import java.time.LocalDate;
import java.util.List;

public final class RequestController {
    private final RequestService service;

    public RequestController(RequestService service) { this.service = service; }

    public List<ChangeRequest> findForUser(User user) { return service.findForUser(user); }

    public ChangeRequest create(ChangeRequest request) { return service.create(request); }

    public void approve(ChangeRequest request, User actor) { service.approve(request, actor); }

    public void reject(ChangeRequest request, User actor, String reason) { service.reject(request, actor, reason); }

    public boolean canProcess(ChangeRequest request, User actor) { return service.canProcess(request, actor); }

    public List<ChangeRequest> search(User user, String keyword, String type, RequestStatus tabStatus) {
        return service.findForUser(user).stream()
                .filter(request -> tabStatus == null || request.getStatus() == tabStatus)
                .filter(request -> type == null || type.startsWith("Tất cả") || request.getType().getDisplayName().equals(type))
                .filter(request -> keyword == null || keyword.isBlank()
                        || TextUtils.containsIgnoreAccent(request.getRequester().getFullName(), keyword)
                        || TextUtils.containsIgnoreAccent(request.getReason(), keyword)
                        || TextUtils.containsIgnoreAccent(request.getType().getDisplayName(), keyword))
                .toList();
    }
    public void requestRoom(User user, Classroom room, String date, TimeSlot slot) {
        create(FormController.request(user, RequestType.USE_ROOM, null, room, date, slot, "", "0",
                "Yêu cầu sử dụng phòng trống theo kết quả tra cứu.", Priority.NORMAL));
    }
}

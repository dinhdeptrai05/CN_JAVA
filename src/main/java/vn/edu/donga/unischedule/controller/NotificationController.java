package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.service.NotificationService;
import vn.edu.donga.unischedule.util.TextUtils;
import java.time.LocalDate;
import java.util.List;

public final class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) { this.service = service; }

    public List<Notification> findForUser(User user) { return service.findForUser(user); }

    public long unreadCount(User user) { return service.unreadCount(user); }

    public void markRead(Notification notification) { service.markRead(notification); }

    public void markAllRead(User user) { service.markAllRead(user); }

    public List<Notification> search(User user, String keyword, String type, String read) {
        return service.findForUser(user).stream()
                .filter(notification -> type == null || type.startsWith("Tất cả") || notification.getType().getDisplayName().equals(type))
                .filter(notification -> read == null || read.equals("Tất cả")
                        || (read.equals("Chưa đọc") && !notification.isRead())
                        || (read.equals("Đã đọc") && notification.isRead()))
                .filter(notification -> keyword == null || keyword.isBlank()
                        || TextUtils.containsIgnoreAccent(notification.getTitle(), keyword)
                        || TextUtils.containsIgnoreAccent(notification.getContent(), keyword))
                .toList();
    }
}

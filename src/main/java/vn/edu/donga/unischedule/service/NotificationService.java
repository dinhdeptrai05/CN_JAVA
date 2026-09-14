package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.Notification;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.repository.NotificationRepository;

import java.util.List;

public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> findForUser(User user) {
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.ACADEMIC) {
            return notificationRepository.findAll().stream()
                    .filter(notification -> notification.getUser().getId().equals(user.getId()))
                    .toList();
        }
        return notificationRepository.findAll().stream()
                .filter(notification -> notification.getUser().getId().equals(user.getId()))
                .toList();
    }

    public long unreadCount(User user) {
        return findForUser(user).stream().filter(notification -> !notification.isRead()).count();
    }

    public void markRead(Notification notification) {
        boolean previous=notification.isRead();notification.setRead(true);
        try { notificationRepository.save(notification); } catch(RuntimeException ex) {notification.setRead(previous);throw ex;}
    }

    public void markAllRead(User user) {
        findForUser(user).forEach(this::markRead);
    }
}

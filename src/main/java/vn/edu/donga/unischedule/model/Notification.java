package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.NotificationType;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private User user;
    private NotificationType type;
    private String title;
    private String content;
    private boolean read;
    private LocalDateTime createdAt;
    private String targetScreen;

    public Notification(Long id, User user, NotificationType type, String title, String content,
                        boolean read, LocalDateTime createdAt, String targetScreen) {
        this.id = id;
        this.user = user;
        this.type = type;
        this.title = title;
        this.content = content;
        this.read = read;
        this.createdAt = createdAt;
        this.targetScreen = targetScreen;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getTargetScreen() {
        return targetScreen;
    }
}

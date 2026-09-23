package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.util.DateUtils;
import java.time.LocalDate;
import java.util.*;

public final class DashboardController {
    public enum Tone { PRIMARY, SUCCESS, WARNING, DANGER, INFO }
    public record Metric(String icon, String title, String value, String description, Tone tone) { }
    public record BuildingUsage(String building, long used, int total, int percent) { }
    private final AppServices services;
    private volatile List<ScheduleEntry> cachedWeek;
    private volatile LocalDate cachedWeekStart;
    private volatile long cachedWeekAt;
    private volatile List<Conflict> cachedConflicts;
    private volatile long cachedConflictsAt;

    public DashboardController(AppServices services) { this.services = services; }

    public List<Metric> metrics(User user, String department) {
        List<Metric> stats = new ArrayList<>();

        long available = services.rooms().findAll().stream().filter(room -> room.getRoomStatus() == RoomStatus.AVAILABLE).count();
        long inUse = usedRoomIds().size();
        long maintenance = services.rooms().findAll().stream().filter(room -> room.getRoomStatus() == RoomStatus.MAINTENANCE).count();
        long pending = services.requests().findForUser(user).stream().filter(request -> request.getStatus() == RequestStatus.PENDING).count();
        if (user.getRole() == Role.ADMIN) {
            var users=services.users().findAll();
            long activeUsers=users.stream().filter(account->account.getStatus()==UserStatus.ACTIVE).count();
            stats.add(new Metric("ND", "Đang hoạt động", String.valueOf(activeUsers), "Trong "+users.size()+" tài khoản", Tone.PRIMARY));
            stats.add(new Metric("TR", "Phòng sẵn sàng", String.valueOf(available), "Có thể xếp lịch", Tone.SUCCESS));
            stats.add(new Metric("SD", "Có lịch tuần này", String.valueOf(inUse), "Phòng được xếp lịch", Tone.WARNING));
            stats.add(new Metric("BT", "Bảo trì", String.valueOf(maintenance), "Cần theo dõi", Tone.DANGER));
        } else if (user.getRole() == Role.ACADEMIC) {
            long totalRooms = services.rooms().findAll().size();
            long weekClasses = weeklySchedules().stream()
                    .filter(entry -> department.equals("Tất cả khoa") || entry.getCourseSection().getCourse().getDepartment().getName().equals(department))
                    .map(entry -> entry.getCourseSection().getId()).distinct().count();
            stats.add(new Metric("LH", "Tổng số lớp tuần này", String.valueOf(weekClasses), "Lớp có lịch trong tuần", Tone.PRIMARY));
            stats.add(new Metric("PH", "Tỷ lệ sử dụng phòng", (totalRooms == 0 ? 0 : inUse * 100 / totalRooms) + "%", inUse + "/" + totalRooms + " phòng đang dùng", Tone.INFO));
            stats.add(new Metric("YC", "Đổi lịch chờ duyệt", String.valueOf(pending), "Yêu cầu đang chờ", Tone.WARNING));
            stats.add(new Metric("XD", "Cảnh báo xung đột", String.valueOf(weeklyConflicts().stream().filter(c -> c.getStatus() != ConflictStatus.RESOLVED).count()), "Trong tuần này", Tone.DANGER));
        } else {
            List<ScheduleEntry> week = services.schedules().findByWeekForUser(DateUtils.currentWeekMonday(), user);
            int today = DateUtils.toSchoolDay(LocalDate.now());
            long todayCount = week.stream().filter(entry -> entry.getDayOfWeek() == today).count();
            String next = week.stream().findFirst()
                    .map(entry -> entry.getCourseSection().getCourse().getName() + " tại " + entry.getRoom().getCode())
                    .orElse("Chưa có lịch");
            stats.add(new Metric("HN", "Hôm nay", String.valueOf(todayCount), "Buổi học/dạy", Tone.PRIMARY));
            stats.add(new Metric("TT", "Buổi tiếp theo", todayCount > 0 ? "Có lịch" : "Trống", next, Tone.SUCCESS));
            stats.add(new Metric("TB", "Thông báo mới", String.valueOf(services.notifications().unreadCount(user)), "Cần đọc", Tone.WARNING));
            stats.add(new Metric("TU", "Trong tuần", String.valueOf(week.size()), "Lịch liên quan", Tone.PRIMARY));
        }
    
        return List.copyOf(stats);
    }

    public List<ScheduleEntry> today(User user, String department) {
        int day = DateUtils.toSchoolDay(LocalDate.now());
        List<ScheduleEntry> week = user.getRole() == Role.ADMIN || user.getRole() == Role.ACADEMIC
                ? weeklySchedules() : services.schedules().findByWeekForUser(DateUtils.currentWeekMonday(), user);
        return week.stream()
                .filter(entry -> entry.getDayOfWeek() == day)
                .filter(entry -> department.equals("Tất cả khoa") || entry.getCourseSection().getCourse().getDepartment().getName().equals(department))
                .limit(12)
                .toList();
    }
    public List<BuildingUsage> usage() {
        List<Classroom> all = services.rooms().findAll();
        var usedIds=usedRoomIds();
        return all.stream().map(Classroom::getBuilding).distinct().map(building -> {
            var rooms = all.stream().filter(room -> room.getBuilding().equals(building)).toList();
            long used = rooms.stream().filter(room -> usedIds.contains(room.getId())).count();
            return new BuildingUsage(building, used, rooms.size(), rooms.isEmpty() ? 0 : (int) (used * 100 / rooms.size()));
        }).toList();
    }
    private java.util.Set<Long> usedRoomIds() {
        return weeklySchedules().stream()
                .filter(s -> s.getStatus() == ScheduleStatus.PUBLISHED)
                .map(s -> s.getRoom().getId())
                .collect(java.util.stream.Collectors.toSet());
    }
    private List<ScheduleEntry> weeklySchedules() {
        LocalDate monday = DateUtils.currentWeekMonday();
        long now = System.currentTimeMillis();
        List<ScheduleEntry> current = cachedWeek;
        if (current != null && monday.equals(cachedWeekStart) && now - cachedWeekAt < 15_000) return current;
        synchronized (this) {
            current = cachedWeek;
            if (current != null && monday.equals(cachedWeekStart) && now - cachedWeekAt < 15_000) return current;
            current = services.schedules().findByWeek(monday);
            cachedWeekStart = monday;
            cachedWeekAt = System.currentTimeMillis();
            cachedWeek = current;
            return current;
        }
    }
    private List<Conflict> weeklyConflicts() {
        long now = System.currentTimeMillis();
        List<Conflict> current = cachedConflicts;
        if (current != null && now - cachedConflictsAt < 15_000) return current;
        synchronized (this) {
            current = cachedConflicts;
            if (current != null && now - cachedConflictsAt < 15_000) return current;
            current = services.conflicts().findConflictsInWeek(DateUtils.currentWeekMonday());
            cachedConflictsAt = System.currentTimeMillis();
            cachedConflicts = current;
            return current;
        }
    }
    public List<Conflict> urgentConflicts() {
        return weeklyConflicts().stream().filter(c -> c.getStatus() != ConflictStatus.RESOLVED).limit(2).toList();
    }
    public List<ChangeRequest> recentRequests(User user) {
        return services.requests().findForUser(user).stream().sorted(Comparator.comparing(ChangeRequest::getCreatedAt).reversed()).limit(3).toList();
    }
    public List<Notification> recentNotifications(User user) {
        return services.notifications().findForUser(user).stream().limit(5).toList();
    }
}

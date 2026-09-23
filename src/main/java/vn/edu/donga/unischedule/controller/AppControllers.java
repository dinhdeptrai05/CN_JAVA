package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.service.AppServices;

/** Composition of controllers sharing one application model. No Swing dependencies. */
public final class AppControllers {
    private final AuditController audit;
    private final HistoryController history;
    public HistoryController history() {return history;}
    public AuditController audit() { return audit; }
    private final DashboardController dashboard;
    private final ReportController reports;
    public ReportController reports() { return reports; }
    private final AuthController auth;
    private final CatalogController catalog;
    private final ScheduleController schedules;
    private final ConflictController conflicts;
    private final RoomController rooms;
    private final RequestController requests;
    private final UserController users;
    private final CourseSectionController courseSections;
    private final NotificationController notifications;

    public AppControllers(AppServices services) {
        audit = new AuditController(services.audit());
        history = new HistoryController(services.history());
        dashboard = new DashboardController(services);
        reports = new ReportController(services.reports());
        auth = new AuthController(services.auth());
        catalog = new CatalogController(services.catalog());
        schedules = new ScheduleController(services.schedules());
        conflicts = new ConflictController(services.conflicts());
        rooms = new RoomController(services.rooms());
        requests = new RequestController(services.requests());
        users = new UserController(services.users());
        courseSections = new CourseSectionController(services.courseSections());
        notifications = new NotificationController(services.notifications());
    }
    public AuthController auth() { return auth; }
    public CatalogController catalog() { return catalog; }
    public ScheduleController schedules() { return schedules; }
    public ConflictController conflicts() { return conflicts; }
    public RoomController rooms() { return rooms; }
    public RequestController requests() { return requests; }
    public UserController users() { return users; }
    public CourseSectionController courseSections() { return courseSections; }
    public NotificationController notifications() { return notifications; }
    public DashboardController dashboard() { return dashboard; }
}

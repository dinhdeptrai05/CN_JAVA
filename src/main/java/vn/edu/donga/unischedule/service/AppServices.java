package vn.edu.donga.unischedule.service;


public class AppServices {
    private AuditService auditService = new AuditService();
    public AuditService audit() { return auditService; }
    private final CatalogService catalogService;
    private final AuthService authService;
    private final ScheduleService scheduleService;
    private final ConflictService conflictService;
    private final RoomService roomService;
    private final RequestService requestService;
    private final UserService userService;
    private final CourseSectionService courseSectionService;
    private final NotificationService notificationService;
    private ReportService reportService;
    private HistoryService historyService = new HistoryService(() -> new vn.edu.donga.unischedule.model.HistorySnapshot(java.time.LocalDate.now(),java.time.LocalDate.now(),0,java.util.List.of()));
    public HistoryService history() { return historyService; }
    public ReportService reports() { return reportService; }

    public AppServices(CatalogService catalogService, AuthService authService, ScheduleService scheduleService,
                        ConflictService conflictService, RoomService roomService, RequestService requestService,
                        UserService userService, CourseSectionService courseSectionService,
                        NotificationService notificationService) {
        this.catalogService = catalogService;
        this.authService = authService;
        this.scheduleService = scheduleService;
        this.conflictService = conflictService;
        this.roomService = roomService;
        this.requestService = requestService;
        this.userService = userService;
        this.courseSectionService = courseSectionService;
        this.notificationService = notificationService;
        this.reportService = new ReportService(user -> new vn.edu.donga.unischedule.model.Report.Source(scheduleService.findAll(),roomService.findAll(),catalogService.getTimeSlots(),courseSectionService.findAll(),requestService.findForUser(user)));
    }

    public static AppServices createJdbc() {
        var db=new vn.edu.donga.unischedule.repository.jdbc.JdbcDatabase(new vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory());
        db.scalar("SELECT COUNT(*) FROM roles");
        var catalog=new CatalogService(new vn.edu.donga.unischedule.repository.jdbc.JdbcCatalogRepository(db));
        var users=new vn.edu.donga.unischedule.repository.jdbc.JdbcUserRepository(db);
        var schedules=new vn.edu.donga.unischedule.repository.jdbc.JdbcScheduleRepository(db);
        var conflicts=new ConflictService(schedules);
        var scheduleService=new ScheduleService(schedules,conflicts);
        var services=new AppServices(catalog,new JdbcAuthService(db),scheduleService,conflicts,
            new RoomService(new vn.edu.donga.unischedule.repository.jdbc.JdbcRoomRepository(db),scheduleService,conflicts),
            new RequestService(new vn.edu.donga.unischedule.repository.jdbc.JdbcRequestRepository(db),scheduleService),new UserService(users,catalog),
            new CourseSectionService(new vn.edu.donga.unischedule.repository.jdbc.JdbcCourseSectionRepository(db)),
            new NotificationService(new vn.edu.donga.unischedule.repository.jdbc.JdbcNotificationRepository(db)));
        services.auditService=new AuditService(db);
        services.reportService=new ReportService(new vn.edu.donga.unischedule.repository.jdbc.JdbcReportRepository(db));
        services.historyService=new HistoryService(new vn.edu.donga.unischedule.repository.jdbc.JdbcHistoryRepository(db));return services;
    }

    public CatalogService catalog() {
        return catalogService;
    }

    public AuthService auth() {
        return authService;
    }

    public ScheduleService schedules() {
        return scheduleService;
    }

    public ConflictService conflicts() {
        return conflictService;
    }

    public RoomService rooms() {
        return roomService;
    }

    public RequestService requests() {
        return requestService;
    }

    public UserService users() {
        return userService;
    }

    public CourseSectionService courseSections() {
        return courseSectionService;
    }

    public NotificationService notifications() {
        return notificationService;
    }
}

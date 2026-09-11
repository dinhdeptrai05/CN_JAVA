package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.repository.mock.MockCourseSectionRepository;
import vn.edu.donga.unischedule.repository.mock.MockDataStore;
import vn.edu.donga.unischedule.repository.mock.MockNotificationRepository;
import vn.edu.donga.unischedule.repository.mock.MockRequestRepository;
import vn.edu.donga.unischedule.repository.mock.MockRoomRepository;
import vn.edu.donga.unischedule.repository.mock.MockScheduleRepository;
import vn.edu.donga.unischedule.repository.mock.MockUserRepository;

public class AppServices {
    private final AuditService auditService = new AuditService();
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

    private AppServices(CatalogService catalogService, AuthService authService, ScheduleService scheduleService,
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
    }

    public static AppServices createDemo() {
        MockDataStore store = new MockDataStore();
        CatalogService catalog = new CatalogService(new vn.edu.donga.unischedule.repository.mock.MockCatalogRepository(store));
        MockUserRepository userRepository = new MockUserRepository(store);
        MockScheduleRepository scheduleRepository = new MockScheduleRepository(store);
        MockRoomRepository roomRepository = new MockRoomRepository(store);
        ConflictService conflict = new ConflictService(scheduleRepository);
        ScheduleService schedule = new ScheduleService(scheduleRepository, conflict);
        RoomService room = new RoomService(roomRepository, schedule, conflict);
        RequestService request = new RequestService(new MockRequestRepository(store));
        UserService user = new UserService(userRepository, catalog);
        CourseSectionService section = new CourseSectionService(new MockCourseSectionRepository(store));
        NotificationService notification = new NotificationService(new MockNotificationRepository(store));
        return new AppServices(catalog, new MockAuthService(userRepository), schedule, conflict, room,
                request, user, section, notification);
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

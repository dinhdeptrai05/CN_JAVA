package vn.edu.donga.unischedule.service;
import vn.edu.donga.unischedule.repository.mock.MockCourseSectionRepository;
import vn.edu.donga.unischedule.repository.mock.MockDataStore;
import vn.edu.donga.unischedule.repository.mock.MockNotificationRepository;
import vn.edu.donga.unischedule.repository.mock.MockRequestRepository;
import vn.edu.donga.unischedule.repository.mock.MockRoomRepository;
import vn.edu.donga.unischedule.repository.mock.MockScheduleRepository;
import vn.edu.donga.unischedule.repository.mock.MockUserRepository;
public final class TestFixtures {
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


}


package vn.edu.donga.unischedule;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MvcArchitectureTest {
    private static final Path ROOT = Path.of("src/main/java/vn/edu/donga/unischedule");

    @Test void viewsDependOnControllersRatherThanServicesOrRepositories() throws Exception {
        for (Path file : sources("ui")) {
            String source = Files.readString(file);
            for (String dependency : List.of(".service.", ".repository.", "AppServices", "Validator."))
                assertFalse(source.contains(dependency), file + " crosses MVC boundary: " + dependency);
            assertFalse(source.matches("(?s).*new\\s+(User|Classroom|Equipment|CourseSection|ScheduleEntry|ChangeRequest)\\s*\\(.*"),
                    file + " must submit form input to a controller");
            for (String setter : List.of("setPassword", "setFullName", "setEmail", "setPhone", "setLecturer", "setQuantity", "setStatus", "setId"))
                assertFalse(source.contains("." + setter + "("), file + " mutates domain state: " + setter);
        }
    }

    @Test void controllersAndModelAreIndependentOfSwing() throws Exception {
        for (String layer : List.of("controller", "model", "service", "repository", "validation")) {
            for (Path file : sources(layer)) {
                String source = Files.readString(file);
                for (String dependency : List.of("javax.swing", "java.awt", "unischedule.ui."))
                    assertFalse(source.contains(dependency), file + " depends on a View: " + dependency);
                if (!layer.equals("controller")) assertFalse(source.contains("unischedule.controller."), file + " reverses model dependency");
            }
        }
    }

    @Test void controllersNeverExposeServiceInstances() throws Exception {
        for (Class<?> type : List.of(vn.edu.donga.unischedule.controller.AppControllers.class,
                vn.edu.donga.unischedule.controller.UserController.class, vn.edu.donga.unischedule.controller.RoomController.class,
                vn.edu.donga.unischedule.controller.ScheduleController.class)) {
            for (var method : type.getDeclaredMethods()) if (java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                assertFalse(method.getReturnType().getPackageName().contains(".service"), method.toString());
        }
    }

    private List<Path> sources(String layer) throws Exception {
        try (var files = Files.walk(ROOT.resolve(layer))) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }
}

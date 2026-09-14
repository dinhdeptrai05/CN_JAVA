package vn.edu.donga.unischedule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import vn.edu.donga.unischedule.util.*;
import vn.edu.donga.unischedule.validation.ValidationException;
import static org.junit.jupiter.api.Assertions.*;
class ProfileImagesTest {
    @TempDir Path directory;
    @Test void rejectsInvalidAndOversizeImages() throws Exception {
        Path invalid=directory.resolve("invalid.png");Files.writeString(invalid,"not an image");assertThrows(ValidationException.class,()->ProfileImages.read(invalid));
        Path large=directory.resolve("large.jpg");Files.write(large,new byte[ProfileImages.MAX_BYTES+1]);assertThrows(ValidationException.class,()->ProfileImages.read(large));
    }
    @Test void passwordHashesAreSaltedAndVerified() {
        String a=PasswordHasher.hash("password"),b=PasswordHasher.hash("password");assertNotEquals(a,b);assertTrue(PasswordHasher.verify("password",a));assertFalse(PasswordHasher.verify("wrong",a));assertFalse(PasswordHasher.verify("password","password"));
    }
}

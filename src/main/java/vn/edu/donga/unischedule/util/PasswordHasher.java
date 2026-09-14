package vn.edu.donga.unischedule.util;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public final class PasswordHasher {
    private static final int ITERATIONS = 600000;
    private PasswordHasher() { }
    public static boolean isHash(String value) { return value != null && value.startsWith("pbkdf2-sha256$"); }
    public static String hash(String password) {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        return "pbkdf2-sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }
    public static boolean verify(String password, String encoded) {
        if (!isHash(encoded) || password == null) return false;
        try {
            String[] parts = encoded.split("\\$");
            int rounds = Integer.parseInt(parts[1]);
            if (rounds < 100000 || rounds > 2000000) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(parts[3]), derive(password, Base64.getDecoder().decode(parts[2]), rounds));
        } catch (RuntimeException ex) { return false; }
    }
    private static byte[] derive(String password, byte[] salt, int rounds) {
        var spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException ex) { throw new IllegalStateException(ex); }
        finally { spec.clearPassword(); }
    }
}

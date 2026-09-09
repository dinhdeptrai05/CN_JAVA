package vn.edu.donga.unischedule.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextUtils {
    private TextUtils() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.toLowerCase(Locale.ROOT);
    }

    public static boolean containsIgnoreAccent(String source, String keyword) {
        return normalize(source).contains(normalize(keyword));
    }
}

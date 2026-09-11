package vn.edu.donga.unischedule.validation;

import java.util.regex.Pattern;

public final class Validator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    private Validator() {
    }

    public static void required(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " không được để trống.");
        }
    }

    public static void positive(int value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " phải là số nguyên dương.");
        }
    }

    public static void username(String value) {
        required(value, "Username");
        if (value.contains(" ")) {
            throw new ValidationException("Username không được chứa khoảng trắng.");
        }
    }

    public static void email(String value) {
        if (value != null && !value.isBlank() && !EMAIL_PATTERN.matcher(value).matches()) {
            throw new ValidationException("Email không đúng định dạng.");
        }
    }

    public static void reason(String value, String fieldName) {
        required(value, fieldName);
        if (value.trim().length() < 10) {
            throw new ValidationException(fieldName + " phải có ít nhất 10 ký tự.");
        }
    }
}

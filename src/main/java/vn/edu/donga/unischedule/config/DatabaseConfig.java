package vn.edu.donga.unischedule.config;

public final class DatabaseConfig {
    public static final String URL = "jdbc:mysql://localhost:3306/unischedule?useSSL=false&serverTimezone=UTC";
    public static final String USERNAME = "unischedule_app";
    public static final String PASSWORD_ENV = "UNISCHEDULE_DB_PASSWORD";

    private DatabaseConfig() {
    }
}

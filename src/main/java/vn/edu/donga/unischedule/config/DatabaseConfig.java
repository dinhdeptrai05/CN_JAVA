package vn.edu.donga.unischedule.config;

public final class DatabaseConfig {
    private static final java.util.Properties SETTINGS = new java.util.Properties();
    static {
        try (var in = DatabaseConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) SETTINGS.load(in);
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }
    public static String get(String key, String environment, String fallback) {
        return System.getProperty(key, System.getenv().getOrDefault(environment, SETTINGS.getProperty(key, fallback)));
    }
    public static String url() { return get("db.url", "UNISCHEDULE_DB_URL", "jdbc:mysql://localhost:3306/unischedule?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=15000"); }
    public static String username() { return get("db.username", "UNISCHEDULE_DB_USER", "unischedule_app"); }
    public static String password() { return get("db.password", "UNISCHEDULE_DB_PASSWORD", ""); }

    private DatabaseConfig() {
    }
}

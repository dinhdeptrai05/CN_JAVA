package vn.edu.donga.unischedule.repository.jdbc;
import java.sql.*;
import vn.edu.donga.unischedule.config.DatabaseConfig;
public class ConnectionFactory {
    public Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(DatabaseConfig.url(), DatabaseConfig.username(), DatabaseConfig.password());
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        try (var statement = connection.prepareStatement("SET time_zone = '+00:00'")) { statement.execute(); }
        return connection;
    }
}

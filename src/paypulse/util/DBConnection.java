package paypulse.util;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Shared by all three modules. Reads db.url, db.user and db.password from db.properties
 * (in the project folder). Never commit db.properties.
 */
public class DBConnection {

    private static final String CONFIG_FILE = System.getProperty("paypulse.config", "db.properties");
    private static Properties props;

    private DBConnection() { }

    private static synchronized Properties load() throws SQLException {
        if (props == null) {
            Properties p = new Properties();
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                p.load(reader);
            } catch (IOException e) {
                throw new SQLException("Cannot read " + CONFIG_FILE
                        + ". Copy db.properties.example to db.properties first.", e);
            }
            props = p;
        }
        return props;
    }

    /** Callers must close the connection (use try-with-resources). */
    public static Connection getConnection() throws SQLException {
        Properties p = load();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Add MySQL Connector/J to project dependencies.", e);
        }

        Properties info = new Properties();

        if (p.getProperty("db.user") != null) {
            info.setProperty("user", p.getProperty("db.user"));
        }

        if (p.getProperty("db.password") != null) {
            info.setProperty("password", p.getProperty("db.password"));
        }

        return DriverManager.getConnection(p.getProperty("db.url"), info);
    }

    /** Run this class to test your database setup. */
    public static void main(String[] args) {
        try (Connection c = getConnection()) {
            System.out.println("Connected to MySQL!");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}

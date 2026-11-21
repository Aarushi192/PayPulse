package paypulse.ui;

import paypulse.repository.UserRepository;
import paypulse.service.DailyLimitReset;
import paypulse.service.UserService;
import paypulse.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Person A's entry point. Connects to MySQL, loads users, starts the daily-limit reset thread
 * and opens the accounts menu. If MySQL is not available it runs in memory-only mode.
 * (Person C's final Main will call the same pieces.)
 */
public class AccountsApp {

    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;

    public static void main(String[] args) throws InterruptedException {
        UserService service;
        try (Connection con = DBConnection.getConnection()) {
            System.out.println("Connected to the database.");
            service = new UserService(new UserRepository());
            int loaded = service.loadFromDatabase();
            System.out.println("Loaded " + loaded + " user(s) from the database.");
        } catch (SQLException | IllegalStateException e) {
            System.out.println("Database not available (" + e.getMessage() + ").");
            System.out.println("Running in MEMORY-ONLY mode: data will be lost when you exit.");
            service = new UserService();
        }

        Thread resetThread = new Thread(new DailyLimitReset(service, ONE_DAY_MILLIS), "daily-reset");
        resetThread.setDaemon(true);
        resetThread.start();

        new AccountMenu(service, new Scanner(System.in)).run();

        resetThread.interrupt();                           // ask the reset thread to stop
        resetThread.join(1000);                            // wait up to 1 second for it
    }
}

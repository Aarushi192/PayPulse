package paypulse;

import paypulse.exception.AccountLockedException;
import paypulse.exception.InvalidPinException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.User;
import paypulse.repository.FraudFlagRepository;
import paypulse.repository.UserRepository;
import paypulse.service.DailyLimitReset;
import paypulse.service.FraudHub;
import paypulse.service.FraudMonitor;
import paypulse.service.UserService;
import paypulse.ui.AccountMenu;
import paypulse.ui.AdminMenu;
import paypulse.ui.PaymentMenu;
import paypulse.util.DBConnection;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * PayPulse: the one program that joins Person A (accounts), Person B (payments) and Person C (fraud).
 * Run this class for the final demo.
 */
public class Main {

    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String RULES_FILE = "rules.txt";
    private static final String FRAUD_LOG = "fraud_log.txt";

    public static void main(String[] args) throws InterruptedException {
        // 1. Users: from MySQL if it is available, otherwise in memory only
        UserService service = startUserService();

        // 2. Fraud setup: rules file, log file
        if (new File(RULES_FILE).exists()) {
            try {
                FraudHub.SERVICE.loadThresholds(RULES_FILE);
            } catch (Exception e) {
                System.out.println("Could not load " + RULES_FILE + " (" + e.getMessage() + "). Using default rules.");
            }
        }
        FraudHub.SERVICE.setLogPath(FRAUD_LOG);
        checkFraudTable();

        // 3. Background threads: daily-limit reset (Person A) and fraud monitor (Person C)
        Thread resetThread = new Thread(new DailyLimitReset(service, ONE_DAY_MILLIS), "daily-reset");
        resetThread.setDaemon(true);
        resetThread.start();
        Thread monitorThread = new Thread(
                new FraudMonitor(FraudHub.INBOX, FraudHub.SERVICE, false), "fraud-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();

        // 4. Main menu
        Scanner sc = new Scanner(System.in);
        try {
            while (true) {
                System.out.println("\n=========== PayPulse ===========");
                System.out.println("Every transaction, in sync.");
                System.out.println("1. Accounts (register, accounts menu)");
                System.out.println("2. Payments (log in and send money)");
                System.out.println("3. Admin (fraud alerts and reports)");
                System.out.println("4. Exit");
                System.out.print("Choose: ");
                switch (sc.nextLine().trim()) {
                    case "1" -> new AccountMenu(service, sc).run();
                    case "2" -> payments(service, sc);
                    case "3" -> admin(sc);
                    case "4" -> {
                        System.out.println("Goodbye!");
                        resetThread.interrupt();
                        monitorThread.interrupt();
                        resetThread.join(1000);
                        monitorThread.join(1000);
                        return;
                    }
                    default -> System.out.println("Please enter 1, 2, 3 or 4.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Goodbye!");
        }
    }

    private static UserService startUserService() {
        try (Connection con = DBConnection.getConnection()) {
            System.out.println("Connected to the database.");
            UserService service = new UserService(new UserRepository());
            System.out.println("Loaded " + service.loadFromDatabase() + " user(s) from the database.");
            return service;
        } catch (SQLException | IllegalStateException e) {
            System.out.println("Database not available (" + e.getMessage() + ").");
            System.out.println("Running in MEMORY-ONLY mode: data will be lost when you exit.");
            return new UserService();
        }
    }

    private static void checkFraudTable() {
        try {
            System.out.println("Fraud flags stored in the database: " + new FraudFlagRepository().count());
        } catch (SQLException e) {
            System.out.println("Fraud table check skipped: " + e.getMessage());
        }
    }

    private static void payments(UserService service, Scanner sc) {
        while (true) {
            System.out.print("UPI ID (blank to go back): ");
            String upi = sc.nextLine().trim();
            if (upi.isEmpty()) {
                return;
            }
            System.out.print("PIN: ");
            String pin = sc.nextLine().trim();
            try {
                User user = service.login(upi, pin);
                new PaymentMenu(service, user, sc).run();
                return;
            } catch (InvalidUPIException | InvalidPinException | AccountLockedException e) {
                System.out.println("Login failed: " + e.getMessage());
            }
        }
    }

    private static void admin(Scanner sc) {
        System.out.print("Admin password: ");
        if (sc.nextLine().trim().equals(ADMIN_PASSWORD)) {
            new AdminMenu(FraudHub.SERVICE, sc).run();
        } else {
            System.out.println("Wrong password.");
        }
    }
}

package paypulse.ui;

import paypulse.exception.AccountLockedException;
import paypulse.exception.InvalidPinException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.User;
import paypulse.service.CsvUserImporter;
import paypulse.service.UserService;
import paypulse.util.ValidationUtil;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Console menus for Person A: register, login, account details, CSV import, reports. */
public class AccountMenu {

    private final UserService service;
    private final Scanner sc;

    public AccountMenu(UserService service, Scanner sc) {
        this.service = service;
        this.sc = sc;
    }

    // ---------------- Main menu ----------------
    public void run() {
        try {
            while (true) {
                System.out.println("\n=== PayPulse: Accounts ===");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Import users from CSV");
                System.out.println("4. Reports");
                System.out.println("5. Exit");
                String choice = ask("Choose: ");
                switch (choice) {
                    case "1" -> register();
                    case "2" -> login();
                    case "3" -> importCsv();
                    case "4" -> reports();
                    case "5" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Please enter a number from 1 to 5.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Goodbye!");
        }
    }

    // ---------------- Register (with input validation loop) ----------------
    private void register() {
        System.out.println("\n--- Register ---");
        while (true) {
            try {
                String upi = ask("UPI ID (e.g. ravi@upi): ");
                String name = askName();
                String pin = readPin("Choose a 4-digit PIN: ");
                User user = service.registerNew(upi, name, pin);
                System.out.println("Registered " + user.getName() + " as " + user.getMaskedUpi());
                return;
            } catch (InvalidUPIException | InvalidPinException | IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());
                if (!askYesNo("Try again? (y/n): ")) {
                    return;
                }
            } catch (IllegalStateException e) {
                System.out.println("Database problem: " + e.getMessage());
                return;
            }
        }
    }

    // ---------------- Login ----------------
    private void login() {
        System.out.println("\n--- Login ---");
        try {
            String upi = ask("UPI ID: ");
            String pin = readPin("PIN: ");
            User user = service.login(upi, pin);
            System.out.println("Login successful. Welcome, " + user.getName() + "!");
            accountMenu(user);
        } catch (InvalidUPIException e) {
            System.out.println("Login failed: " + e.getMessage());
        } catch (InvalidPinException e) {
            System.out.println("Login failed: " + e.getMessage());
        } catch (AccountLockedException e) {
            System.out.println("LOCKED: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("Database problem: " + e.getMessage());
        }
    }

    // ---------------- After login ----------------
    private void accountMenu(User user) {
        while (true) {
            System.out.println("\n--- My Account (" + user.getMaskedUpi() + ") ---");
            System.out.println("1. Account details");
            System.out.println("2. Add money");
            System.out.println("3. Add contact");
            System.out.println("4. View contacts");
            System.out.println("5. Logout");
            String choice = ask("Choose: ");
            switch (choice) {
                case "1" -> showDetails(user);
                case "2" -> addMoney(user);
                case "3" -> addContact(user);
                case "4" -> System.out.println("Contacts: " + user.getContacts());
                case "5" -> {
                    System.out.println("Logged out.");
                    return;
                }
                default -> System.out.println("Please enter a number from 1 to 5.");
            }
        }
    }

    private void showDetails(User user) {
        System.out.println("UPI ID      : " + user.getMaskedUpi());
        System.out.println("Name        : " + user.getName());
        System.out.println("Account type: " + user.getUserType());
        System.out.printf("Balance     : %.2f%n", user.getBalance());
        System.out.printf("Spent today : %.2f of %.2f%n", user.getDailySpent(), user.getDailyLimit());
        System.out.println("Locked      : " + user.isLocked());
    }

    private void addMoney(User user) {
        try {
            double amount = ValidationUtil.parseAmount(ask("Amount to add: "));
            service.addMoney(user, amount);
            System.out.printf("Added %.2f. New balance: %.2f%n", amount, user.getBalance());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid amount: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("Database problem: " + e.getMessage());
        }
    }

    private void addContact(User user) {
        String contact = ask("Contact's UPI ID: ");
        if (service.addContact(user.getUpiId(), contact)) {
            System.out.println("Contact saved.");
        } else {
            System.out.println("Could not add: that UPI ID is not registered (or it is your own).");
        }
    }

    // ---------------- CSV import ----------------
    private void importCsv() {
        System.out.println("\n--- Import users from CSV ---");
        String path = ask("CSV file path (press Enter for users.csv): ");
        if (path.isEmpty()) {
            path = "users.csv";
        }
        try {
            CsvUserImporter.ImportResult result = CsvUserImporter.read(path, service.getUpiIds());
            int added = service.importUsers(result);
            result.printSummary();
            System.out.println("Added to the system: " + added);
        } catch (IOException e) {
            System.out.println("Could not read file '" + path + "': " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("Database problem: " + e.getMessage());
        }
    }

    // ---------------- Reports (streams) ----------------
    private void reports() {
        System.out.println("\n--- Reports ---");
        System.out.println("Users (A to Z):");
        for (User u : service.getUsersSortedByName()) {
            System.out.println("  " + u);
        }
        System.out.println("Top 3 richest:");
        List<User> top = service.getRichestUsers(3);
        top.forEach(u -> System.out.printf("  %s  %.2f%n", u.getName(), u.getBalance()));
        System.out.println("Locked accounts: "
                + service.getLockedAccounts().stream().map(User::getName).toList());
        System.out.printf("Total money in the system: %.2f%n", service.getTotalMoney());
        System.out.println("Recent logins: " + service.getRecentLogins());
    }
    private String askName() {
        while (true) {
            String raw = ask("Full name: ");
            try {
                return ValidationUtil.validateName(raw);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid name: " + e.getMessage());
            }
        }
    }

    // ---------------- Input helpers ----------------
    private String ask(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private boolean askYesNo(String prompt) {
        return ask(prompt).equalsIgnoreCase("y");
    }

    /** try / multi-catch / finally for PIN input. */
    private String readPin(String prompt) throws InvalidPinException {
        String raw = null;
        try {
            raw = ask(prompt);
            Integer.parseInt(raw);                         // may throw NumberFormatException
            return ValidationUtil.parsePin(raw);
        } catch (NumberFormatException | NullPointerException e) {
            throw new InvalidPinException("PIN must contain digits only.", e);
        } finally {
            raw = null;                                    // clear the input buffer
        }
    }
}

package paypulse.ui;

import paypulse.exception.AccountLockedException;
import paypulse.exception.InvalidPinException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.MerchantUser;
import paypulse.model.RegularUser;
import paypulse.model.User;
import paypulse.util.ValidationUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Person A's Phase 1 console test. Run this class to check your part. */

public class AccountsDemo {
    private static final Map<String, User> users = new HashMap<>();

    public static void main(String[] args) {
        runAutomatedChecks();
        runMenu();
    }

    // ---------- Automated checks (polymorphism, strings, arrays, lockout) ----------
    private static void runAutomatedChecks() {
        System.out.println("=== Polymorphism ===");
        List<User> list = new ArrayList<>();
        list.add(new RegularUser("ravi@upi", "Ravi Kumar", "1234", 5000));
        list.add(new MerchantUser("shop@upi", "Sharma Store", "4321", 20000));
        for (User u : list) {
            System.out.println(u + " | daily limit = " + u.getDailyLimit());
        }
        System.out.println("Total users created: " + User.getTotalUsers());

        System.out.println("\n=== String pool ===");
        String a = "ravi@upi";
        String b = "ravi@upi";
        String c = new String("ravi@upi");
        System.out.println("a == b: " + (a == b) + ", a == c: " + (a == c)
                + ", a.equals(c): " + a.equals(c) + ", a == c.intern(): " + (a == c.intern()));

        System.out.println("\n=== Balance snapshots (users x days) ===");
        double[][] snaps = {
                {1000, 1200, 900, 1500},
                {5000, 4800, 5200, 5100},
                {250, 300, 100, 400}
        };
        System.out.printf("%-8s %8s %8s %8s %8s %10s%n", "User", "Day1", "Day2", "Day3", "Day4", "Total");
        for (int i = 0; i < snaps.length; i++) {
            double total = 0;
            System.out.printf("%-8s", "User" + (i + 1));
            for (double v : snaps[i]) {
                System.out.printf(" %8.0f", v);
                total += v;
            }
            System.out.printf(" %10.0f%n", total);
        }

        System.out.println("\n=== Lockout after 3 wrong PINs ===");
        User ravi = list.get(0);
        for (int i = 1; i <= 4; i++) {
            try {
                ravi.login("0000");
            } catch (InvalidPinException | AccountLockedException e) {
                System.out.println("Attempt " + i + ": " + e.getMessage());
            }
        }
        System.out.println("Locked? " + ravi.isLocked());
    }

    // ---------- Console menu ----------
    private static void runMenu() {
        Scanner sc = new Scanner(System.in);
        try {
            while (true) {
                System.out.println("\n=== PayPulse Accounts ===");
                System.out.println("1. Register\n2. Login\n3. Exit");
                System.out.print("Choose: ");
                String choice = sc.nextLine().trim();
                switch (choice) {
                    case "1" -> register(sc);
                    case "2" -> login(sc);
                    case "3" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Please enter 1, 2 or 3.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Goodbye!");
        }
    }

    private static void register(Scanner sc) {
        String upi = askUpi(sc);
        if (users.containsKey(upi)) {
            System.out.println("That UPI ID is already registered.");
            return;
        }
        System.out.print("Full name: ");
        String name = ValidationUtil.cleanName(sc.nextLine());
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        String pin = askPin(sc);
        users.put(upi, new RegularUser(upi, name, pin));
        System.out.println("Registered " + name + " as " + upi);
    }

    private static void login(Scanner sc) {
        System.out.print("UPI ID: ");
        User user = users.get(sc.nextLine().trim());
        if (user == null) {
            System.out.println("No such user.");
            return;
        }
        try {
            String pin = readPin(sc, "PIN: ");
            if (user.login(pin)) {
                System.out.println("Login successful!");
                System.out.println("UPI ID : " + user.getMaskedUpi());
                System.out.println("Name   : " + user.getName());
                System.out.println("Balance: " + user.getBalance());
            }
        } catch (InvalidPinException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (AccountLockedException e) {
            System.out.println("LOCKED: " + e.getMessage());
        }
    }

    // ---------- Input validation loops ----------
    private static String askUpi(Scanner sc) {
        while (true) {
            System.out.print("UPI ID (e.g. ravi@upi): ");
            String upi = sc.nextLine().trim();
            try {
                ValidationUtil.validateUpi(upi);
                return upi;
            } catch (InvalidUPIException e) {
                System.out.println("Invalid: " + e.getMessage());
            }
        }
    }

    private static String askPin(Scanner sc) {
        while (true) {
            try {
                return readPin(sc, "Set 4-digit PIN: ");
            } catch (InvalidPinException e) {
                System.out.println("Invalid: " + e.getMessage());
            }
        }
    }

    /** try / multi-catch / finally for PIN input. */
    private static String readPin(Scanner sc, String prompt) throws InvalidPinException {
        String raw = null;
        try {
            System.out.print(prompt);
            raw = sc.nextLine().trim();
            Integer.parseInt(raw);                         // may throw NumberFormatException
            return ValidationUtil.parsePin(raw);
        } catch (NumberFormatException | NullPointerException e) {
            throw new InvalidPinException("PIN must contain digits only.", e);
        } finally {
            raw = null;                                    // clear the input buffer
        }
    }
}

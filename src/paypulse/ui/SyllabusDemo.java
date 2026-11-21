package paypulse.ui;

import paypulse.exception.AccountLockedException;
import paypulse.exception.InvalidPinException;
import paypulse.model.MerchantUser;
import paypulse.model.RegularUser;
import paypulse.model.User;
import paypulse.repository.UserRepository;
import paypulse.service.UserService;
import paypulse.util.ValidationUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Viva helper: runs one demo for each syllabus topic of Person A (Phases 1, 4 and 5). */
public class SyllabusDemo {

    public static void main(String[] args) {
        polymorphism();
        stringPool();
        balanceTable();
        autoboxing();
        lockout();
        lambdasAndStreams();
        sqlInjection();
    }

    private static void polymorphism() {
        System.out.println("=== Inheritance and polymorphism ===");
        List<User> list = new ArrayList<>();
        list.add(new RegularUser("ravi@upi", "Ravi Kumar", "1234", 5000));
        list.add(new MerchantUser("shop@upi", "Sharma Store", "4321", 20000));
        for (User u : list) {
            System.out.println(u + " | daily limit = " + u.getDailyLimit());
        }
        System.out.println("Total users created (static counter): " + User.getTotalUsers());
    }

    private static void stringPool() {
        System.out.println("\n=== String pool ===");
        String a = "ravi@upi";
        String b = "ravi@upi";
        String c = new String("ravi@upi");
        System.out.println("a == b: " + (a == b) + ", a == c: " + (a == c)
                + ", a.equals(c): " + a.equals(c) + ", a == c.intern(): " + (a == c.intern()));
    }

    private static void balanceTable() {
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
    }

    private static void autoboxing() {
        System.out.println("\n=== Wrappers and autoboxing: failed login attempts ===");
        Map<String, Integer> failed = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            failed.put("ravi@upi", failed.getOrDefault("ravi@upi", 0) + 1);   // box and unbox
        }
        Integer boxed = Integer.valueOf("1234");                               // valueOf
        int pinNumber = Integer.parseInt("0042");                              // parseInt
        System.out.println("Failed attempts map: " + failed);
        System.out.println("valueOf(\"1234\") = " + boxed + ", parseInt(\"0042\") = " + pinNumber);
    }

    private static void lockout() {
        System.out.println("\n=== Lockout after 3 wrong PINs ===");
        User ravi = new RegularUser("ravi@upi", "Ravi Kumar", "1234", 5000);
        for (int i = 1; i <= 4; i++) {
            try {
                ravi.login("0000");
            } catch (InvalidPinException | AccountLockedException e) {
                System.out.println("Attempt " + i + ": " + e.getMessage());
            }
        }
        System.out.println("Locked? " + ravi.isLocked());
    }

    private static void lambdasAndStreams() {
        System.out.println("\n=== Lambdas and streams ===");
        System.out.println("Validation rules as Predicate<String>:");
        for (String s : new String[]{"ravi@upi", "ravi", "ab@upi", "ravi@UPI"}) {
            System.out.println("  " + s + " valid? " + ValidationUtil.IS_VALID_UPI.test(s));
        }

        UserService service = new UserService();
        service.register(new RegularUser("ravi@upi", "Ravi Kumar", "1234", 5000));
        service.register(new RegularUser("anita@upi", "Anita Sharma", "1111", 12000));
        service.register(new MerchantUser("shop@upi", "Sharma Store", "4321", 90000));
        User locked = new RegularUser("mohan@upi", "Mohan Das", "2222", 700);
        locked.lock();
        service.register(locked);

        System.out.println("Names (User::getName, sorted): " + service.getUserNames());
        System.out.println("Top 2 richest: "
                + service.getRichestUsers(2).stream().map(User::getName).toList());
        System.out.println("Locked accounts: "
                + service.getLockedAccounts().stream().map(User::getName).toList());
        System.out.printf("Total money in the system: %.2f%n", service.getTotalMoney());
    }

    private static void sqlInjection() {
        System.out.println("\n=== Statement vs PreparedStatement (needs MySQL) ===");
        try {
            UserRepository repo = new UserRepository();
            if (repo.findById("ravi@upi").isEmpty()) {
                repo.save(new RegularUser("ravi@upi", "Ravi Kumar", "1234", 5000));
            }
            String attack = "' OR '1'='1";
            System.out.println("Attacker types the PIN: " + attack);

            System.out.println("Unsafe Statement login:");
            Optional<User> bad = repo.loginUnsafe("ravi@upi", attack);
            System.out.println("  Result: " + (bad.isPresent() ? "LOGGED IN without the real PIN!" : "rejected"));

            System.out.println("Safe PreparedStatement login:");
            Optional<User> good = repo.loginSafe("ravi@upi", attack);
            System.out.println("  Result: " + (good.isPresent() ? "logged in" : "rejected (attack failed)"));
        } catch (SQLException e) {
            System.out.println("Skipped: database not available (" + e.getMessage() + ")");
        }
    }
}

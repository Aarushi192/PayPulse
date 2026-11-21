package paypulse.ui;

import paypulse.model.User;
import paypulse.service.UserService;

import java.util.concurrent.Callable;

/** 5 test cases for Person A: input, expected result, actual result. */
public class AccountTests {

    private static int passed = 0;
    private static int total = 0;

    public static void main(String[] args) {
        UserService service = new UserService();         // memory-only, no database needed

        System.out.println("=== Person A: Account Test Cases ===");
        System.out.printf("%-3s %-26s %-30s %-26s %-26s %s%n",
                "No", "Test", "Input", "Expected", "Actual", "Result");
        System.out.println("-".repeat(125));

        check(1, "Valid registration", "ravi@upi / Ravi Kumar / 1234", "REGISTERED",
                () -> {
                    service.registerNew("ravi@upi", "Ravi Kumar", "1234");
                    return "REGISTERED";
                });

        check(2, "UPI ID without '@'", "ravi", "InvalidUPIException",
                () -> {
                    service.registerNew("ravi", "Ravi", "1234");
                    return "REGISTERED";
                });

        check(3, "PIN with letters", "pin = 12ab", "InvalidPinException",
                () -> {
                    service.registerNew("sita@upi", "Sita", "12ab");
                    return "REGISTERED";
                });

        check(4, "3 wrong PINs lock account", "ravi@upi, PIN 0000 x3", "AccountLockedException",
                () -> {
                    for (int i = 1; i <= 3; i++) {
                        try {
                            service.login("ravi@upi", "0000");
                        } catch (paypulse.exception.InvalidPinException e) {
                            // attempts 1 and 2 only warn; attempt 3 throws AccountLockedException
                        }
                    }
                    return "NOT LOCKED";
                });

        check(5, "Duplicate UPI ID", "ravi@upi again", "InvalidUPIException",
                () -> {
                    service.registerNew("ravi@upi", "Another Ravi", "4321");
                    return "REGISTERED";
                });

        User ravi = service.getUser("ravi@upi");
        System.out.println("-".repeat(125));
        System.out.println("Passed " + passed + " of " + total
                + " | ravi@upi locked after test 4: " + (ravi != null && ravi.isLocked()));
    }

    private static void check(int no, String name, String input, String expected,
                              Callable<String> action) {
        String actual;
        try {
            actual = action.call();
        } catch (Exception e) {
            actual = e.getClass().getSimpleName();
        }
        total++;
        boolean ok = expected.equals(actual);
        if (ok) {
            passed++;
        }
        System.out.printf("%-3d %-26s %-30s %-26s %-26s %s%n",
                no, name, input, expected, actual, ok ? "PASS" : "FAIL");
    }
}

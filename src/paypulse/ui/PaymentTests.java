package paypulse.ui;

import paypulse.model.BillPayment;
import paypulse.model.RegularUser;
import paypulse.model.Transaction;
import paypulse.model.User;
import paypulse.service.PaymentCommandParser;
import paypulse.service.UserService;

import java.util.concurrent.Callable;

/** 5 test cases for Person B: input, expected result, actual result. */
public class PaymentTests {

    private static int passed = 0;
    private static int total = 0;

    public static void main(String[] args) {
        UserService service = new UserService();         // memory-only, no database needed
        service.register(new RegularUser("ravi@upi", "Ravi", "1234", 100000));
        service.register(new RegularUser("priya@upi", "Priya", "4321", 0));
        service.register(new RegularUser("sita@upi", "Sita", "1111", 100));
        User ravi = service.getUser("ravi@upi");

        System.out.println("=== Person B: Payment Test Cases ===");
        System.out.printf("%-3s %-24s %-28s %-30s %-30s %s%n",
                "No", "Test", "Input", "Expected", "Actual", "Result");
        System.out.println("-".repeat(128));

        check(1, "Valid payment", "send 500 to priya@upi Lunch", "SUCCESS",
                () -> send(service, "ravi@upi", "send 500 to priya@upi Lunch"));

        check(2, "Amount is not a number", "send abc to priya@upi", "InvalidAmountException",
                () -> send(service, "ravi@upi", "send abc to priya@upi"));

        check(3, "Not enough balance", "sita sends 5000, has 100", "InsufficientBalanceException",
                () -> send(service, "sita@upi", "send 5000 to priya@upi"));

        check(4, "Bill fee is 1%", "bill 200 from ravi", "FEE 2.00, PAID 202.00",
                () -> {
                    double before = ravi.getBalance();
                    Transaction bill = new BillPayment("ravi@upi", "priya@upi", 200, "Electricity");
                    bill.process(ravi, service.getUser("priya@upi"));
                    return String.format("FEE %.2f, PAID %.2f", bill.calculateFee(), before - ravi.getBalance());
                });

        check(5, "Over the daily limit", "send 60000, limit 50000", "DailyLimitExceededException",
                () -> send(service, "ravi@upi", "send 60000 to priya@upi"));

        System.out.println("-".repeat(128));
        System.out.println("Passed " + passed + " of " + total
                + " | ravi balance after tests: " + ravi.getBalance());
    }

    /** Reads the command, carries it out, and returns the final status as text. */
    private static String send(UserService service, String senderUpi, String command) throws Exception {
        Transaction t = PaymentCommandParser.parse(senderUpi, command);
        t.process(service.getUser(senderUpi), service.getUser(t.getReceiverUpi()));
        return t.getStatus().name();
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
        System.out.printf("%-3d %-24s %-28s %-30s %-30s %s%n",
                no, name, input, expected, actual, ok ? "PASS" : "FAIL");
    }
}

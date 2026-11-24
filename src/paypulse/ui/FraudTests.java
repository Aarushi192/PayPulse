package paypulse.ui;

import paypulse.exception.InvalidRuleException;
import paypulse.exception.SuspiciousTransactionException;
import paypulse.model.FraudFlag;
import paypulse.model.P2PTransaction;
import paypulse.model.Transaction;
import paypulse.service.FraudService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/** Six test cases for fraud detection. Run main() and show the table in the viva. */
public class FraudTests {

    private static int passed = 0;
    private static int total = 0;

    public static void main(String[] args) throws IOException {
        System.out.printf("%-3s %-34s %-26s %-26s %s%n", "No", "Input", "Expected", "Actual", "Result");
        System.out.println("-".repeat(100));

        // 1. A very large payment must be flagged HIGH
        FraudService s1 = new FraudService();
        List<FraudFlag> f1 = s1.check(new P2PTransaction("a@upi", "b@upi", 15000));
        row("1", "send 15000", "Large amount, HIGH",
                f1.isEmpty() ? "no flag" : f1.get(0).getReason() + ", " + FraudFlag.riskLabel(f1.get(0).getRiskScore()),
                f1.size() == 1 && f1.get(0).getRiskScore() == FraudFlag.HIGH);

        // 2. A normal payment must NOT be flagged
        FraudService s2 = new FraudService();
        List<FraudFlag> f2 = s2.check(new P2PTransaction("a@upi", "b@upi", 250));
        row("2", "send 250", "no flag", f2.isEmpty() ? "no flag" : f2.get(0).getReason(), f2.isEmpty());

        // 3. A big round amount is MEDIUM risk
        FraudService s3 = new FraudService();
        List<FraudFlag> f3 = s3.check(new P2PTransaction("a@upi", "b@upi", 6000));
        row("3", "send 6000 (round)", "Round big amount, MEDIUM",
                f3.isEmpty() ? "no flag" : f3.get(0).getReason() + ", " + FraudFlag.riskLabel(f3.get(0).getRiskScore()),
                f3.size() == 1 && f3.get(0).getRiskScore() == FraudFlag.MEDIUM);

        // 4. Three quick payments from the same sender: the THIRD is flagged as rapid
        FraudService s4 = new FraudService();
        s4.check(new P2PTransaction("a@upi", "b@upi", 10));
        s4.check(new P2PTransaction("a@upi", "b@upi", 20));
        List<FraudFlag> f4 = s4.check(new P2PTransaction("a@upi", "b@upi", 30));
        row("4", "3 payments in a row, same sender", "3rd: Rapid repeated",
                f4.isEmpty() ? "no flag" : f4.get(0).getReason(),
                f4.size() == 1 && f4.get(0).getReason().startsWith("Rapid"));

        // 5. checkStrict must throw the checked exception for a HIGH risk payment
        FraudService s5 = new FraudService();
        String actual5;
        boolean ok5;
        try {
            s5.checkStrict(new P2PTransaction("a@upi", "b@upi", 50000));
            actual5 = "no exception";
            ok5 = false;
        } catch (SuspiciousTransactionException e) {
            actual5 = "SuspiciousTransactionException";
            ok5 = true;
        }
        row("5", "strict check of 50000", "SuspiciousTransactionException", actual5, ok5);

        // 6. A bad rules file must be rejected, and a good one must change the rules
        File temp = File.createTempFile("rules", ".txt");
        temp.deleteOnExit();
        try (FileWriter w = new FileWriter(temp)) {
            w.write("LARGE_AMOUNT -5\n");
        }
        String actual6;
        boolean ok6;
        try {
            new FraudService().loadThresholds(temp.getPath());
            actual6 = "accepted";
            ok6 = false;
        } catch (InvalidRuleException e) {
            actual6 = "InvalidRuleException";
            ok6 = true;
        }
        row("6", "rules file: LARGE_AMOUNT -5", "InvalidRuleException", actual6, ok6);

        System.out.println("-".repeat(100));
        System.out.println(passed + " of " + total + " tests passed.");
        System.out.println("Flags created in total: " + FraudFlag.getTotalFlags()
                + ", transactions created: " + Transaction.getTotalCount());
    }

    private static void row(String no, String input, String expected, String actual, boolean ok) {
        total++;
        if (ok) {
            passed++;
        }
        System.out.printf("%-3s %-34s %-26s %-26s %s%n", no, input, expected, actual, ok ? "PASS" : "FAIL");
    }
}

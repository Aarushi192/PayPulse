package paypulse.ui;

import paypulse.exception.InvalidRuleException;
import paypulse.exception.ReportExportException;
import paypulse.model.FlaggedAccountsReport;
import paypulse.model.FraudFlag;
import paypulse.model.Report;
import paypulse.model.TopSendersReport;
import paypulse.repository.FraudFlagRepository;
import paypulse.service.FraudService;
import paypulse.util.FlagFilter;
import paypulse.util.JaggedFlagTable;
import paypulse.util.ReportGenerator;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Console admin menu (Person C): fraud alerts, accounts, reports, exports and database actions. */
public class AdminMenu {

    private static final String RULES_FILE = "rules.txt";
    private static final int PAGE_SIZE = 3;

    private final FraudService fraud;
    private final Scanner sc;
    private final FraudFlagRepository repository = new FraudFlagRepository();

    public AdminMenu(FraudService fraud, Scanner sc) {
        this.fraud = fraud;
        this.sc = sc;
    }

    public void run() {
        try {
            while (true) {
                System.out.println("\n=== PayPulse: Admin (fraud) ===");
                System.out.println("1. Flags by priority (highest risk first)");
                System.out.println("2. Flagged accounts");
                System.out.println("3. Flags per user (stream + jagged table)");
                System.out.println("4. Top senders");
                System.out.println("5. Recent alerts");
                System.out.println("6. Search flags with a filter");
                System.out.println("7. Browse flags page by page");
                System.out.println("8. Clear resolved (low risk) flags");
                System.out.println("9. Show / reload fraud rules from rules.txt");
                System.out.println("10. Export reports to a file");
                System.out.println("11. Save flags to the database");
                System.out.println("12. Database report (GROUP BY)");
                System.out.println("13. Back");
                switch (ask("Choose: ")) {
                    case "1" -> showFlags();
                    case "2" -> showFlaggedAccounts();
                    case "3" -> showFlagsPerUser();
                    case "4" -> showTopSenders();
                    case "5" -> showRecentAlerts();
                    case "6" -> searchFlags();
                    case "7" -> browsePages();
                    case "8" -> clearResolved();
                    case "9" -> rules();
                    case "10" -> exportReports();
                    case "11" -> saveToDatabase();
                    case "12" -> databaseReport();
                    case "13" -> {
                        return;
                    }
                    default -> System.out.println("Please enter a number from 1 to 13.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed.");
        }
    }

    private void showFlags() {
        List<FraudFlag> flags = fraud.getFlagsReadOnly();
        if (flags.isEmpty()) {
            System.out.println("No fraud flags yet.");
            return;
        }
        for (FraudFlag f : flags) {
            System.out.println(f + "   sender=" + fraud.senderOf(f.getTransactionId()));
        }
    }

    private void showFlaggedAccounts() {
        new FlaggedAccountsReport(fraud.getFlagsReadOnly(), fraud.getFlaggedAccounts()).print();
        System.out.println("Flags per account: " + fraud.getFraudCounts());
    }

    private void showFlagsPerUser() {
        System.out.println("Counted with a stream: " + fraud.flagsPerUserStream());
        System.out.println();
        System.out.println(new JaggedFlagTable(fraud.flagsByUser()).render());
    }

    private void showTopSenders() {
        int n = askNumber("How many senders (default 3): ", 3);
        new TopSendersReport(fraud.getCheckedTransactions(), n).print();
    }

    private void showRecentAlerts() {
        List<String> alerts = fraud.getRecentAlerts();
        if (alerts.isEmpty()) {
            System.out.println("No alerts yet.");
            return;
        }
        System.out.println("Latest alerts (newest first):");
        for (String a : alerts) {
            System.out.println(" - " + a);
        }
    }

    private void searchFlags() {
        String text = ask("Filter (risk>=2, risk=3, reason:large): ");
        try {
            List<FraudFlag> found = fraud.getFlagsReadOnly().stream()
                    .filter(FlagFilter.parse(text))              // the Predicate made from the text
                    .toList();
            if (found.isEmpty()) {
                System.out.println("No flags match.");
            }
            found.forEach(System.out::println);                  // method reference
        } catch (InvalidRuleException e) {                       // unchecked, but we still handle it nicely
            System.out.println("Filter error: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  caused by: " + e.getCause().getClass().getSimpleName());
            }
        }
    }

    private void browsePages() {
        int page = 1;
        while (true) {
            List<FraudFlag> items = fraud.page(page, PAGE_SIZE);
            if (items.isEmpty()) {
                System.out.println(page == 1 ? "No flags yet." : "No more flags.");
                return;
            }
            System.out.println("--- Page " + page + " ---");
            items.forEach(System.out::println);
            String next = ask("n = next page, anything else = stop: ");
            if (!next.equalsIgnoreCase("n")) {
                return;
            }
            page++;
        }
    }

    private void clearResolved() {
        int max = askNumber("Remove flags with risk up to (1=LOW, 2=MEDIUM, default 1): ", 1);
        System.out.println("Removed " + fraud.clearResolved(max) + " flag(s).");
    }

    private void rules() {
        System.out.println("Current rules: " + fraud.describeRules());
        if (!ask("Reload from " + RULES_FILE + "? (y/n): ").equalsIgnoreCase("y")) {
            return;
        }
        if (!new File(RULES_FILE).exists()) {
            System.out.println(RULES_FILE + " not found in the project folder.");
            return;
        }
        try {
            fraud.loadThresholds(RULES_FILE);
            System.out.println("Loaded. Now: " + fraud.describeRules());
        } catch (IOException e) {
            System.out.println("Could not read the file: " + e.getMessage());
        } catch (InvalidRuleException e) {
            System.out.println("Rules file is wrong: " + e.getMessage());
        }
    }

    private void exportReports() {
        String base = ask("File name without extension (default fraud_report): ");
        if (base.isEmpty()) {
            base = "fraud_report";
        }
        // One generic helper holds both reports (ReportGenerator<Report>)
        ReportGenerator<Report> generator = new ReportGenerator<>();
        generator.add(new FlaggedAccountsReport(fraud.getFlagsReadOnly(), fraud.getFlaggedAccounts()));
        generator.add(new TopSendersReport(fraud.getCheckedTransactions(), 5));
        System.out.println("Reports ready: " + generator.size());
        try {
            new FlaggedAccountsReport(fraud.getFlagsReadOnly(), fraud.getFlaggedAccounts())
                    .export(base + "_flags.txt");
            new TopSendersReport(fraud.getCheckedTransactions(), 5).export(base + "_top_senders.csv");
            System.out.println("Saved " + base + "_flags.txt and " + base + "_top_senders.csv");
        } catch (ReportExportException e) {
            System.out.println("Export failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  caused by: " + e.getCause().getMessage());
            }
        }
    }

    private void saveToDatabase() {
        int saved = 0;
        try {
            for (FraudFlag f : fraud.flagsByPriority()) {
                if (f.getId() == 0) {                            // not saved yet
                    repository.save(f);
                    saved++;
                }
            }
            System.out.println("Saved " + saved + " new flag(s). Rows in database: " + repository.count());
        } catch (SQLException e) {
            System.out.println("Database problem: " + e.getMessage());
            System.out.println("(The transaction must exist in the transactions table first.)");
        }
    }

    private void databaseReport() {
        try {
            System.out.println("Flags per sender: " + repository.flagCountPerSender());
            for (Map.Entry<Integer, Integer> e : repository.flagCountPerRisk().entrySet()) {
                System.out.println("Risk " + FraudFlag.riskLabel(e.getKey()) + ": " + e.getValue());
            }
        } catch (SQLException e) {
            System.out.println("Database not available: " + e.getMessage());
        }
    }

    private int askNumber(String prompt, int fallback) {
        String raw = ask(prompt);
        if (raw.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);                        // String -> int (wrapper class method)
        } catch (NumberFormatException e) {
            System.out.println("Not a number, using " + fallback);
            return fallback;
        }
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }
}

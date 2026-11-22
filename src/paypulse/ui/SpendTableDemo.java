package paypulse.ui;

import paypulse.service.MonthlySpendTable;

import java.util.List;

public class SpendTableDemo {
    public static void main(String[] args) {
        MonthlySpendTable table = new MonthlySpendTable(
                List.of("bescom@upi", "priya@upi", "ravi@upi"));

        table.record("ravi@upi", 1, 1200);
        table.record("ravi@upi", 2, 800);
        table.record("ravi@upi", 2, 450.50);
        table.record("ravi@upi", 11, 3000);
        table.record("priya@upi", 3, 500);
        table.record("priya@upi", 11, 250);
        table.record("bescom@upi", 12, 90);

        System.out.println(table.render());
        System.out.println();
        System.out.println("Unknown user accepted?  " + table.record("sita@upi", 1, 100));
        System.out.println("Month 13 accepted?      " + table.record("ravi@upi", 13, 100));
    }
}

package paypulse.ui;

import paypulse.model.BillPayment;
import paypulse.model.P2PTransaction;
import paypulse.model.RegularUser;
import paypulse.model.Transaction;
import paypulse.model.User;
import paypulse.service.BackupService;
import paypulse.service.StatementExporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Person B, file I/O demo. Writes into the statements/ and backups/ folders of the project. */
public class FileDemo {

    public static void main(String[] args) throws Exception {
        User ravi = new RegularUser("ravi@upi", "Ravi", "1234", 6000);
        User priya = new RegularUser("priya@upi", "Priya", "4321", 0);
        User bescom = new RegularUser("bescom@upi", "Bescom", "1111", 0);
        List<Transaction> list = new ArrayList<>();
        list.add(run(new P2PTransaction("ravi@upi", "priya@upi", 1500, "Rent, share \"May\""), ravi, priya));
        list.add(run(new BillPayment("ravi@upi", "bescom@upi", 2000, "Electricity"), ravi, bescom));
        list.add(run(new P2PTransaction("ravi@upi", "priya@upi", 9000, "Too much"), ravi, priya));

        StatementExporter exporter = new StatementExporter();
        File csv = new File("statements", "demo_statement.csv");
        File txt = new File("statements", "demo_statement.txt");

        System.out.println("=== 1. Write files ===");
        System.out.println("CSV rows written: " + exporter.exportCsv(list, csv));
        exporter.exportText(list, txt);
        exporter.appendLine(new File("statements", "demo_log.txt"), "demo run finished");
        System.out.println(csv.getPath() + " exists: " + csv.exists() + ", size " + csv.length() + " bytes");

        System.out.println("\n=== 2. Read the CSV back ===");
        for (String[] row : exporter.readCsv(csv)) {
            System.out.println("  id=" + row[0] + " " + row[1] + " " + row[4] + " " + row[6] + " note=[" + row[8] + "]");
        }

        System.out.println("\n=== 3. Backup copy (byte streams) ===");
        BackupService backups = new BackupService(new File("backups"));
        File copy = backups.backup(csv);
        System.out.println("Backup created: " + copy.getPath() + " (" + copy.length() + " bytes)");
        backups.backup(csv);
        backups.backup(csv);
        System.out.println("Backups now: " + backups.listBackups().length);
        System.out.println("Deleted old ones: " + backups.deleteOldBackups(1) + ", left: " + backups.listBackups().length);
    }

    private static Transaction run(Transaction t, User from, User to) {
        try {
            t.process(from, to);
        } catch (Exception e) {
            // marked FAILED by process()
        }
        return t;
    }
}
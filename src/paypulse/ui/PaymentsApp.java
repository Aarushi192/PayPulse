package paypulse.ui;

import paypulse.exception.AccountLockedException;
import paypulse.exception.InvalidPinException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.RegularUser;
import paypulse.model.User;
import paypulse.service.UserService;

import java.util.NoSuchElementException;
import java.util.Scanner;


public class PaymentsApp {
    public static void main(String[] args) {
        UserService service = new UserService();
        service.register(new RegularUser("ravi@upi", "Ravi", "1234", 5000));
        service.register(new RegularUser("priya@upi", "Priya", "4321", 1000));
        service.register(new RegularUser("bescom@upi", "Bescom", "1111", 0));

        Scanner sc = new Scanner(System.in);
        try {
            while (true) {
                System.out.print("UPI ID (blank to quit): ");
                String upi = sc.nextLine().trim();
                if (upi.isEmpty()) {
                    System.out.println("Goodbye!");
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
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Goodbye!");
        }
    }
}
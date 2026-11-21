package paypulse.ui;

import paypulse.exception.AccountLockedException;
import paypulse.exception.InvalidPinException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.MerchantUser;
import paypulse.model.RegularUser;
import paypulse.model.User;
import paypulse.service.UserService;

/** Phase 2 test: HashMap, HashSet, ArrayList, Deque, Iterator, Comparable, Comparator. */
public class CollectionsDemo {

    public static void main(String[] args) throws Exception {
        UserService service = new UserService();

        service.register(new RegularUser("ravi@upi", "Ravi Kumar", "1234", 5000));
        service.register(new RegularUser("anita@upi", "anita sharma", "1111", 12000));
        service.register(new MerchantUser("shop@upi", "Sharma Store", "4321", 90000));
        service.register(new RegularUser("zero@upi", "Zero Balance", "0000", 0));
        service.register(new RegularUser("mohan@upi", "Mohan Das", "2222", 700));

        System.out.println("Duplicate register works? "
                + service.register(new RegularUser("ravi@upi", "Fake Ravi", "9999")));

        try {
            service.registerNew("bad-id", "Test", "1234");
        } catch (InvalidUPIException | InvalidPinException e) {
            System.out.println("registerNew rejected: " + e.getMessage());
        }
        User sita = service.registerNew("sita@upi", "  sITA   rAM ", "5678");
        System.out.println("registerNew accepted: " + sita);

        System.out.println("\n=== Recent logins (Deque, max 5, newest first) ===");
        String[][] attempts = {
                {"ravi@upi", "1234"}, {"anita@upi", "1111"}, {"shop@upi", "4321"},
                {"mohan@upi", "2222"}, {"sita@upi", "5678"}, {"ravi@upi", "1234"},
                {"anita@upi", "wrong"}, {"ghost@upi", "1234"}
        };
        for (String[] a : attempts) {
            try {
                service.login(a[0], a[1]);
                System.out.println("Login OK   : " + a[0]);
            } catch (InvalidUPIException | InvalidPinException | AccountLockedException e) {
                System.out.println("Login FAIL : " + a[0] + " -> " + e.getMessage());
            }
        }
        System.out.println("Recent: " + service.getRecentLogins());

        System.out.println("\n=== Saved contacts (ArrayList) ===");
        service.addContact("ravi@upi", "anita@upi");
        service.addContact("ravi@upi", "shop@upi");
        service.addContact("ravi@upi", "anita@upi");        // duplicate ignored
        System.out.println("Ravi's contacts: " + service.getUser("ravi@upi").getContacts());
        System.out.println("Add unknown contact: " + service.addContact("ravi@upi", "nobody@upi"));

        System.out.println("\n=== Sorted by name (Comparable + Collections.sort) ===");
        service.getUsersSortedByName().forEach(System.out::println);

        System.out.println("\n=== TreeSet (auto sorted by name) ===");
        for (User u : service.getUsersTreeSet()) {
            System.out.println(u.getName());
        }

        System.out.println("\n=== Sorted by balance, richest first (Comparator) ===");
        service.getUsersSortedByBalance().forEach(System.out::println);
        System.out.println("Richest (Collections.max): " + service.getRichest().getName());

        System.out.println("\n=== Iterator cleanup ===");
        System.out.println("Users before: " + service.getAllUsers().size());
        System.out.println("Removed inactive: " + service.removeInactiveUsers());
        System.out.println("Users after : " + service.getAllUsers().size());
    }
}

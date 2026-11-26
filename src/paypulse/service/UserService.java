package paypulse.service;

import paypulse.exception.InvalidPinException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.RegularUser;
import paypulse.model.User;
import paypulse.repository.UserRepository;
import paypulse.util.ValidationUtil;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Person A's service. Users live in memory (collections) and, when a repository is given,
 * every change is also saved to MySQL. With no repository it runs in memory-only mode.
 */
public class UserService {

    private static final int MAX_RECENT_LOGINS = 5;

    /** Comparator by balance (lambda / method reference style). */
    public static final Comparator<User> BY_BALANCE = Comparator.comparingDouble(User::getBalance);

    private final Map<String, User> users = new HashMap<>();        // upiId -> User
    private final Set<String> upiIds = new HashSet<>();             // fast duplicate check
    private final Deque<String> recentLogins = new ArrayDeque<>();  // newest first
    private final UserRepository repository;                        // null = memory-only

    public UserService() {
        this(null);
    }

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public boolean isMemoryOnly() {
        return repository == null;
    }

    // ---------------- Startup ----------------
    /** Loads every user from MySQL into memory. Returns how many were loaded. */
    public synchronized int loadFromDatabase() {
        if (repository == null) {
            return 0;
        }
        try {
            int count = 0;
            for (User u : repository.findAll()) {
                if (register(u)) {
                    count++;
                }
            }
            return count;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load users from the database", e);
        }
    }

    // ---------------- Register / login ----------------
    /** Adds a ready-made user to memory only. Returns false if the UPI ID is taken. */
    public synchronized boolean register(User user) {
        if (!upiIds.add(user.getUpiId())) {
            return false;
        }
        users.put(user.getUpiId(), user);
        return true;
    }

    /** Validates raw input from the menu, then registers (and saves) a RegularUser. */
    public synchronized User registerNew(String upiId, String rawName, String rawPin)
            throws InvalidUPIException, InvalidPinException {
        ValidationUtil.validateUpi(upiId);
        String pin = ValidationUtil.parsePin(rawPin);
        String name = ValidationUtil.validateName(rawName);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (upiIds.contains(upiId)) {
            throw new InvalidUPIException("UPI ID already registered: " + upiId);
        }
        User user = new RegularUser(upiId, name, pin);
        register(user);
        try {
            saveNew(user);
        } catch (IllegalStateException e) {
            users.remove(upiId);                  // keep memory and database consistent
            upiIds.remove(upiId);
            throw e;
        }
        return user;
    }

    /**
     * Checks the PIN. Failed attempts and the lock are saved even when login fails,
     * so a locked account stays locked after a restart.
     */
    public synchronized User login(String upiId, String pin)
            throws InvalidUPIException, InvalidPinException {
        User user = users.get(upiId);
        if (user == null) {
            throw new InvalidUPIException("No such user: " + upiId);
        }
        try {
            user.login(pin);                      // may throw InvalidPinException / AccountLockedException
        } finally {
            saveChanges(user);
        }
        recentLogins.remove(upiId);               // avoid duplicates
        recentLogins.addFirst(upiId);             // newest at the front
        if (recentLogins.size() > MAX_RECENT_LOGINS) {
            recentLogins.pollLast();              // drop the oldest
        }
        return user;
    }

    // ---------------- Wallet helpers ----------------
    public synchronized void addMoney(User user, double amount) {
        user.deposit(amount);
        saveChanges(user);
    }

    /** Called by the DailyLimitReset thread. */
    public void resetAllDailyLimits() {
        for (User u : getAllUsers()) {
            u.resetDailySpent();
            saveChanges(u);
        }
    }

    /** Saves a user after Person B's transfer changed the balance. */
    public void saveUser(User user) {
        saveChanges(user);
    }

    // ---------------- Lookups ----------------
    public synchronized User getUser(String upiId) {
        return users.get(upiId);
    }

    /** A copy, so callers (like the reset thread) never touch the live map. */
    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized Set<String> getUpiIds() {
        return new HashSet<>(upiIds);
    }

    public synchronized List<String> getRecentLogins() {
        return new ArrayList<>(recentLogins);
    }

    public synchronized boolean addContact(String ownerUpi, String contactUpi) {
        User owner = users.get(ownerUpi);
        if (owner == null || ownerUpi.equals(contactUpi) || !upiIds.contains(contactUpi)) {
            return false;
        }
        owner.addContact(contactUpi);
        return true;
    }

    // ---------------- Sorting (Comparable / Comparator) ----------------
    public synchronized List<User> getUsersSortedByName() {
        List<User> list = new ArrayList<>(users.values());
        Collections.sort(list);                   // uses User.compareTo
        return list;
    }

    public synchronized List<User> getUsersSortedByBalance() {
        List<User> list = new ArrayList<>(users.values());
        list.sort(BY_BALANCE.reversed());         // richest first
        return list;
    }

    public synchronized TreeSet<User> getUsersTreeSet() {
        return new TreeSet<>(users.values());     // sorted by name automatically
    }

    public synchronized User getRichest() {
        if (users.isEmpty()) {
            return null;
        }
        return Collections.max(users.values(), BY_BALANCE);
    }

    // ---------------- Streams ----------------
    public List<User> getRichestUsers(int n) {
        return getAllUsers().stream()
                .sorted(BY_BALANCE.reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    public List<User> getLockedAccounts() {
        return getAllUsers().stream()
                .filter(User::isLocked)
                .collect(Collectors.toList());
    }

    public double getTotalMoney() {
        return getAllUsers().stream()
                .mapToDouble(User::getBalance)
                .sum();
    }

    public List<String> getUserNames() {
        return getAllUsers().stream()
                .map(User::getName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    // ---------------- Cleanup with Iterator ----------------
    /** Removes users with zero balance who are not in the recent-logins deque. */
    public synchronized int removeInactiveUsers() {
        int removed = 0;
        Iterator<Map.Entry<String, User>> it = users.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, User> entry = it.next();
            User u = entry.getValue();
            if (u.getBalance() == 0 && !recentLogins.contains(entry.getKey())) {
                if (repository != null) {
                    try {
                        repository.delete(entry.getKey());
                    } catch (SQLException e) {
                        System.out.println("Could not delete " + entry.getKey()
                                + " from the database: " + e.getMessage());
                        continue;                 // keep the user, memory must match the database
                    }
                }
                it.remove();                      // safe removal while looping
                upiIds.remove(entry.getKey());
                removed++;
            }
        }
        return removed;
    }

    // ---------------- CSV import ----------------
    /** Registers (and saves) every valid user from an import. Returns how many were added. */
    public synchronized int importUsers(CsvUserImporter.ImportResult result) {
        int added = 0;
        for (User u : result.getValid().values()) {
            if (register(u)) {
                try {
                    saveNew(u);
                } catch (IllegalStateException e) {
                    users.remove(u.getUpiId());
                    upiIds.remove(u.getUpiId());
                    throw e;
                }
                added++;
            }
        }
        return added;
    }

    // ---------------- Database helpers ----------------
    private void saveNew(User user) {
        if (repository == null) {
            return;
        }
        try {
            repository.save(user);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save user " + user.getUpiId(), e);
        }
    }

    private void saveChanges(User user) {
        if (repository == null) {
            return;
        }
        try {
            repository.update(user);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update user " + user.getUpiId(), e);
        }
    }
}

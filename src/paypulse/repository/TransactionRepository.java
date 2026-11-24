package paypulse.repository;

import paypulse.model.BillPayment;
import paypulse.model.P2PTransaction;
import paypulse.model.Transaction;
import paypulse.model.TransactionStatus;
import paypulse.model.User;
import paypulse.util.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Person B, JDBC: CRUD for the transactions table, plus an all-or-nothing payment save. */
public class TransactionRepository implements Repository<Transaction, Integer> {

    private static final String COLUMNS =
            "id, sender_upi, receiver_upi, amount, fee, note, txn_type, status, created_at";
    private static final String INSERT =
            "INSERT INTO transactions (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    // ---------------- Create ----------------
    @Override
    public void save(Transaction t) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            insert(con, t);
        }
    }

    /** Fills the 9 ? marks of INSERT from the payment and runs it on the given connection. */
    private void insert(Connection con, Transaction t) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT)) {
            bind(ps, t);
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Insert affected " + rows + " rows for payment " + t.getId());
            }
        }
    }

    private void bind(PreparedStatement ps, Transaction t) throws SQLException {
        ps.setInt(1, t.getId());
        ps.setString(2, t.getSenderUpi());
        ps.setString(3, t.getReceiverUpi());
        ps.setDouble(4, t.getAmount());
        ps.setDouble(5, t.calculateFee());
        ps.setString(6, t.getNote());
        ps.setString(7, t.getTxnType());
        ps.setString(8, t.getStatus().name());
        ps.setTimestamp(9, Timestamp.valueOf(t.getTimestamp()));
    }

    /** Saves many payments with one batch, inside one database transaction. */
    public void saveAll(List<Transaction> list) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(INSERT)) {
                for (Transaction t : list) {
                    bind(ps, t);
                    ps.addBatch();                 // collect the rows, send them together
                }
                ps.executeBatch();
                con.commit();
            } catch (SQLException e) {
                con.rollback();                    // none of the rows are kept
                throw e;
            }
        }
    }

    // ---------------- Read ----------------
    @Override
    public Optional<Transaction> findById(Integer id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM transactions WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Transaction> findAll() throws SQLException {
        return query("SELECT " + COLUMNS + " FROM transactions ORDER BY id");
    }

    /** Every payment the user sent or received, oldest first. */
    public List<Transaction> findByUser(String upiId) throws SQLException {
        return query("SELECT " + COLUMNS + " FROM transactions WHERE sender_upi = ? OR receiver_upi = ? ORDER BY id",
                upiId, upiId);
    }

    private List<Transaction> query(String sql, String... params) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** Turns one row back into a P2PTransaction or BillPayment. */
    private Transaction mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String sender = rs.getString("sender_upi");
        String receiver = rs.getString("receiver_upi");
        double amount = rs.getDouble("amount");
        String note = rs.getString("note");
        TransactionStatus status = TransactionStatus.valueOf(rs.getString("status"));
        java.time.LocalDateTime time = rs.getTimestamp("created_at").toLocalDateTime();
        if ("BILL".equals(rs.getString("txn_type"))) {
            return new BillPayment(id, sender, receiver, amount, note, time, status);
        }
        return new P2PTransaction(id, sender, receiver, amount, note, time, status);
    }

    // ---------------- Update ----------------
    /** Only the status and note can change after a payment exists. */
    @Override
    public boolean update(Transaction t) throws SQLException {
        String sql = "UPDATE transactions SET status = ?, note = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, t.getStatus().name());
            ps.setString(2, t.getNote());
            ps.setInt(3, t.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------- Delete ----------------
    @Override
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------- Commit / rollback ----------------
    /**
     * Saves a finished payment: both balances AND the payment row in ONE database transaction.
     * Either all of it is stored (commit) or none of it (rollback).
     * Call it after t.process(...) has run, so the User objects already hold the new balances.
     */
    public void savePayment(Transaction t, User sender, User receiver) throws SQLException {
        Connection con = DBConnection.getConnection();
        try {
            con.setAutoCommit(false);                       // start a transaction
            if (t.getStatus() == TransactionStatus.SUCCESS) {
                updateBalance(con, sender);
                updateBalance(con, receiver);
            }
            insert(con, t);
            con.commit();                                   // make everything permanent
        } catch (SQLException e) {
            con.rollback();                                 // undo every step above
            throw e;
        } finally {
            con.setAutoCommit(true);
            con.close();
        }
    }

    private void updateBalance(Connection con, User user) throws SQLException {
        String sql = "UPDATE users SET balance = ?, daily_spent = ? WHERE upi_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, user.getBalance());
            ps.setDouble(2, user.getDailySpent());
            ps.setString(3, user.getUpiId());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("User not found in database: " + user.getUpiId());
            }
        }
    }

    // ---------------- Start-up helper ----------------
    /** Reads the biggest stored ID so new payments continue after it. Returns how many rows exist. */
    public int syncIdCounter() throws SQLException {
        int count = 0;
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id), 0) AS max_id, COUNT(*) AS n FROM transactions")) {
            if (rs.next()) {
                Transaction.ensureNextIdAbove(rs.getInt("max_id"));
                count = rs.getInt("n");
            }
        }
        return count;
    }

    // ---------------- Metadata ----------------
    /** Lists the table's columns using DatabaseMetaData and ResultSetMetaData. */
    public List<String> describeTable() throws SQLException {
        List<String> lines = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            DatabaseMetaData db = con.getMetaData();
            lines.add("Database : " + db.getDatabaseProductName() + " " + db.getDatabaseProductVersion());
            lines.add("Driver   : " + db.getDriverName());
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM transactions LIMIT 0")) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    lines.add(String.format("Column %d : %-13s %s", i, meta.getColumnName(i), meta.getColumnTypeName(i)));
                }
            }
        }
        return lines;
    }
}
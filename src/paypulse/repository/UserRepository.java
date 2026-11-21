package paypulse.repository;

import paypulse.model.MerchantUser;
import paypulse.model.RegularUser;
import paypulse.model.User;
import paypulse.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD for the users table using PreparedStatement and ResultSet. */
public class UserRepository implements Repository<User, String> {

    private static final String COLUMNS =
            "upi_id, name, pin_hash, balance, failed_attempts, locked, daily_spent, user_type";

    @Override
    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getUpiId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getPinHash());
            ps.setDouble(4, user.getBalance());
            ps.setInt(5, user.getFailedAttempts());
            ps.setBoolean(6, user.isLocked());
            ps.setDouble(7, user.getDailySpent());
            ps.setString(8, user.getUserType());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Insert affected " + rows + " rows for " + user.getUpiId());
            }
        }
    }

    @Override
    public Optional<User> findById(String upiId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE upi_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, upiId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<User> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY name";
        List<User> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, pin_hash = ?, balance = ?, failed_attempts = ?, "
                + "locked = ?, daily_spent = ?, user_type = ? WHERE upi_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getPinHash());
            ps.setDouble(3, user.getBalance());
            ps.setInt(4, user.getFailedAttempts());
            ps.setBoolean(5, user.isLocked());
            ps.setDouble(6, user.getDailySpent());
            ps.setString(7, user.getUserType());
            ps.setString(8, user.getUpiId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(String upiId) throws SQLException {
        String sql = "DELETE FROM users WHERE upi_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, upiId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Turns one row into a RegularUser or MerchantUser depending on user_type. */
    private User mapRow(ResultSet rs) throws SQLException {
        String upi = rs.getString("upi_id");
        String name = rs.getString("name");
        String pin = rs.getString("pin_hash");
        double balance = rs.getDouble("balance");
        int failed = rs.getInt("failed_attempts");
        boolean locked = rs.getBoolean("locked");
        double spent = rs.getDouble("daily_spent");
        String type = rs.getString("user_type");

        if ("MERCHANT".equalsIgnoreCase(type)) {
            return new MerchantUser(upi, name, pin, balance, failed, locked, spent);
        }
        return new RegularUser(upi, name, pin, balance, failed, locked, spent);
    }

    // ------------------------------------------------------------------
    // SQL injection demo. NEVER write real login code like loginUnsafe().
    // ------------------------------------------------------------------

    /** UNSAFE: joins user input into the SQL text. Only for the viva demo. */
    public Optional<User> loginUnsafe(String upiId, String pin) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE upi_id = '" + upiId
                + "' AND pin_hash = '" + pin + "'";
        System.out.println("  SQL sent: " + sql);
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
        }
    }

    /** SAFE: values are bound as parameters, so input can never change the SQL. */
    public Optional<User> loginSafe(String upiId, String pin) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE upi_id = ? AND pin_hash = ?";
        System.out.println("  SQL sent: " + sql + "   [parameters bound separately]");
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, upiId);
            ps.setString(2, pin);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }
}

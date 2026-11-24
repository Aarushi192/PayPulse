package paypulse.repository;

import paypulse.model.FraudFlag;
import paypulse.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** CRUD and report queries for the fraud_flags table (PreparedStatement + ResultSet). */
public class FraudFlagRepository implements Repository<FraudFlag, Integer> {

    private static final String COLUMNS = "id, transaction_id, reason, risk_score, flagged_at";

    // ---------------- CRUD ----------------

    @Override
    public void save(FraudFlag flag) throws SQLException {
        String sql = "INSERT INTO fraud_flags (transaction_id, reason, risk_score, flagged_at) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, flag.getTransactionId());
            ps.setString(2, flag.getReason());
            ps.setInt(3, flag.getRiskScore());
            ps.setTimestamp(4, Timestamp.valueOf(flag.getFlaggedAt()));
            int rows = ps.executeUpdate();                      // number of rows inserted
            if (rows != 1) {
                throw new SQLException("Insert affected " + rows + " rows for txn " + flag.getTransactionId());
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {      // the AUTO_INCREMENT id MySQL created
                if (keys.next()) {
                    flag.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public Optional<FraudFlag> findById(Integer id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM fraud_flags WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<FraudFlag> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM fraud_flags ORDER BY risk_score DESC, id";
        List<FraudFlag> list = new ArrayList<>();
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
    public boolean update(FraudFlag flag) throws SQLException {
        String sql = "UPDATE fraud_flags SET reason = ?, risk_score = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, flag.getReason());
            ps.setInt(2, flag.getRiskScore());
            ps.setInt(3, flag.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM fraud_flags WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------- Report queries ----------------

    /** Startup check: how many flags are stored? Fails with SQLException if the table is missing. */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM fraud_flags";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Flags between two date-times (the two ? are the date-range parameters). */
    public List<FraudFlag> findBetween(LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM fraud_flags WHERE flagged_at BETWEEN ? AND ? "
                + "ORDER BY risk_score DESC, id";
        List<FraudFlag> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** GROUP BY sender: how many flags each sender has. Biggest first. */
    public Map<String, Integer> flagCountPerSender() throws SQLException {
        String sql = "SELECT t.sender_upi, COUNT(*) AS flag_count "
                + "FROM fraud_flags f JOIN transactions t ON f.transaction_id = t.id "
                + "GROUP BY t.sender_upi ORDER BY flag_count DESC, t.sender_upi";
        Map<String, Integer> result = new LinkedHashMap<>();          // keeps the SQL order
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("sender_upi"), rs.getInt("flag_count"));
            }
        }
        return result;
    }

    /** GROUP BY risk score: how many LOW / MEDIUM / HIGH flags exist. */
    public Map<Integer, Integer> flagCountPerRisk() throws SQLException {
        String sql = "SELECT risk_score, COUNT(*) AS flag_count FROM fraud_flags "
                + "GROUP BY risk_score ORDER BY risk_score DESC";
        Map<Integer, Integer> result = new LinkedHashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getInt("risk_score"), rs.getInt("flag_count"));
            }
        }
        return result;
    }

    /** Turns one row of the ResultSet into a FraudFlag object. */
    private FraudFlag mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("flagged_at");
        LocalDateTime when = ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
        return new FraudFlag(rs.getInt("id"), rs.getInt("transaction_id"),
                rs.getString("reason"), rs.getInt("risk_score"), when);
    }
}

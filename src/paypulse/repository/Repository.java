package paypulse.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic repository shared by all modules.
 * T = the entity (User, Transaction, FraudFlag), ID = type of its primary key
 * (String for users, Integer for transactions and fraud flags).
 */
public interface Repository<T, ID> {
    void save(T entity) throws SQLException;

    Optional<T> findById(ID id) throws SQLException;

    List<T> findAll() throws SQLException;

    boolean update(T entity) throws SQLException;

    boolean delete(ID id) throws SQLException;
}

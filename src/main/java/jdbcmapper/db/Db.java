package jdbcmapper.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public final class Db {
    private final String url;

    public Db(String url) {
        this.url = url;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public void update(String sql, Object... params) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            bind(ps, params);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB update failed: " + sql, e);
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            bind(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
                return out;
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB query failed: " + sql, e);
        }
    }

    public long queryForLong(String sql, Object... params) {
        List<Long> rows = query(sql, rs -> rs.getLong(1), params);
        if (rows.isEmpty()) {
            throw new RuntimeException("Expected 1 row, got 0");
        }
        return rows.getFirst();
    }

    public void inTransaction(ThrowingConsumer<Connection> work) {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                work.accept(con);
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed", e);
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}


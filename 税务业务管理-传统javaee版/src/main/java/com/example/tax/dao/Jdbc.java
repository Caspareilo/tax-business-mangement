package com.example.tax.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Small JDBC helper. Every SQL statement uses PreparedStatement parameters;
 * callers pass only whitelisted SQL fragments (sort columns etc.).
 */
public final class Jdbc {

    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private Jdbc() {
    }

    public static <T> List<T> query(Connection connection, String sql, RowMapper<T> mapper,
                                    Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        }
    }

    public static <T> T queryOne(Connection connection, String sql, RowMapper<T> mapper,
                                 Object... params) throws SQLException {
        List<T> rows = query(connection, sql, mapper, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static long queryCount(Connection connection, String sql, Object... params)
            throws SQLException {
        Long value = queryOne(connection, sql, rs -> rs.getLong(1), params);
        return value == null ? 0L : value;
    }

    public static int update(Connection connection, String sql, Object... params)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        }
    }

    public static long insert(Connection connection, String sql, Object... params)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, params);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("插入记录后未取得主键");
    }

    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param == null) {
                statement.setNull(i + 1, java.sql.Types.VARCHAR);
            } else if (param instanceof java.sql.Date) {
                statement.setDate(i + 1, (java.sql.Date) param);
            } else if (param instanceof java.sql.Timestamp) {
                statement.setTimestamp(i + 1, (java.sql.Timestamp) param);
            } else if (param instanceof java.math.BigDecimal) {
                statement.setBigDecimal(i + 1, (java.math.BigDecimal) param);
            } else if (param instanceof Integer) {
                statement.setInt(i + 1, (Integer) param);
            } else if (param instanceof Long) {
                statement.setLong(i + 1, (Long) param);
            } else if (param instanceof Boolean) {
                statement.setBoolean(i + 1, (Boolean) param);
            } else {
                statement.setString(i + 1, String.valueOf(param));
            }
        }
    }
}

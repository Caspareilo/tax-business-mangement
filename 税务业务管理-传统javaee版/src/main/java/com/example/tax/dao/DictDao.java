package com.example.tax.dao;

import com.example.tax.model.DictItem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DictDao {

    public List<DictItem> listAll(Connection connection) throws SQLException {
        return Jdbc.query(connection,
                "SELECT * FROM sys_dict ORDER BY dict_type, sort_order, id", this::map);
    }

    public List<DictItem> listByType(Connection connection, String dictType) throws SQLException {
        return Jdbc.query(connection,
                "SELECT * FROM sys_dict WHERE dict_type = ? AND enabled = 1 "
                        + "ORDER BY sort_order, id", this::map, dictType);
    }

    public long insert(Connection connection, DictItem item) throws SQLException {
        return Jdbc.insert(connection,
                "INSERT INTO sys_dict (dict_type, dict_key, dict_label, sort_order, enabled) "
                        + "VALUES (?, ?, ?, ?, ?)",
                item.getDictType(), item.getDictKey(), item.getDictLabel(),
                item.getSortOrder(), item.isEnabled());
    }

    public int update(Connection connection, DictItem item) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE sys_dict SET dict_label = ?, sort_order = ?, enabled = ? WHERE id = ?",
                item.getDictLabel(), item.getSortOrder(), item.isEnabled(), item.getId());
    }

    public int setEnabled(Connection connection, long id, boolean enabled) throws SQLException {
        return Jdbc.update(connection, "UPDATE sys_dict SET enabled = ? WHERE id = ?", enabled, id);
    }

    private DictItem map(ResultSet rs) throws SQLException {
        DictItem item = new DictItem();
        item.setId(rs.getLong("id"));
        item.setDictType(rs.getString("dict_type"));
        item.setDictKey(rs.getString("dict_key"));
        item.setDictLabel(rs.getString("dict_label"));
        item.setSortOrder(rs.getInt("sort_order"));
        item.setEnabled(rs.getBoolean("enabled"));
        return item;
    }
}

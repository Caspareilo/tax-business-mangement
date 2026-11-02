package com.example.tax.service;

import com.example.tax.dao.DictDao;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.DictItem;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.util.Db;
import com.example.tax.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class SystemService {

    private final DictDao dictDao = new DictDao();
    private final AuditService auditService = new AuditService();

    public List<DictItem> listDictionaries() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return dictDao.listAll(connection);
        }
    }

    public void saveDictionary(SessionUser actor, Long id, String dictType, String dictKey,
                               String dictLabel, int sortOrder, String traceId) throws SQLException {
        requireManagePermission(actor);
        Map<String, String> errors = ValidationUtil.newErrors();
        if (id == null) {
            ValidationUtil.requireCode(errors, "dictType", "字典类型", dictType);
            ValidationUtil.requireCode(errors, "dictKey", "字典键", dictKey);
        }
        ValidationUtil.requireText(errors, "dictLabel", "字典标签", dictLabel, 100);
        if (sortOrder < 0 || sortOrder > 9999) {
            errors.put("sortOrder", "排序值须在 0-9999 之间");
        }
        ValidationUtil.throwIfAny(errors, "请检查字典表单");

        try (Connection connection = Db.getConnection()) {
            if (id == null) {
                DictItem item = new DictItem();
                item.setDictType(dictType.trim());
                item.setDictKey(dictKey.trim());
                item.setDictLabel(dictLabel.trim());
                item.setSortOrder(sortOrder);
                item.setEnabled(true);
                long newId = dictDao.insert(connection, item);
                auditService.record(connection, actor, "DICT_CREATE", "DICT",
                        String.valueOf(newId), true,
                        "新增字典 " + dictType.trim() + "/" + dictKey.trim(), traceId);
            } else {
                DictItem existing = findIn(connection, id);
                existing.setDictLabel(dictLabel.trim());
                existing.setSortOrder(sortOrder);
                dictDao.update(connection, existing);
                auditService.record(connection, actor, "DICT_UPDATE", "DICT",
                        String.valueOf(id), true,
                        "更新字典 " + existing.getDictType() + "/" + existing.getDictKey(), traceId);
            }
        }
    }

    public void setDictionaryEnabled(SessionUser actor, long id, boolean enabled, String traceId)
            throws SQLException {
        requireManagePermission(actor);
        try (Connection connection = Db.getConnection()) {
            DictItem existing = findIn(connection, id);
            dictDao.setEnabled(connection, id, enabled);
            auditService.record(connection, actor, "DICT_STATUS", "DICT", String.valueOf(id), true,
                    "字典 " + existing.getDictType() + "/" + existing.getDictKey()
                            + (enabled ? " 已启用" : " 已停用"), traceId);
        }
    }

    private DictItem findIn(Connection connection, long id) throws SQLException {
        for (DictItem item : dictDao.listAll(connection)) {
            if (item.getId() == id) {
                return item;
            }
        }
        throw new BusinessException("字典项不存在");
    }

    private void requireManagePermission(SessionUser actor) {
        if (actor == null || !actor.hasPermission(PermissionCodes.DICT_MANAGE)) {
            throw new ForbiddenException("无权维护系统字典");
        }
    }
}

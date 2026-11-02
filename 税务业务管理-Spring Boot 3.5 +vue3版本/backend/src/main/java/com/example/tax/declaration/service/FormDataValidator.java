package com.example.tax.declaration.service;

import com.example.tax.common.exception.BusinessException;
import com.example.tax.formtemplate.dto.FieldOptionRequest;
import com.example.tax.formtemplate.entity.FormField;
import com.example.tax.formtemplate.service.FormTemplateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class FormDataValidator {

    private final FormTemplateService templateService;
    private final ObjectMapper objectMapper;

    public FormDataValidator(FormTemplateService templateService, ObjectMapper objectMapper) {
        this.templateService = templateService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> validate(Long templateId, Map<String, Object> rawData) {
        Map<String, Object> input = rawData == null ? Map.of() : rawData;
        if (input.size() > 200) {
            throw BusinessException.badRequest("表单数据项过多");
        }
        List<FormField> fields = templateService.listFields(templateId).stream()
                .filter(field -> field.getEnabled() != null && field.getEnabled() == 1)
                .toList();
        Map<String, FormField> fieldMap = new LinkedHashMap<>();
        fields.forEach(field -> fieldMap.put(field.getFieldCode(), field));

        for (String key : input.keySet()) {
            if (!fieldMap.containsKey(key)) {
                throw BusinessException.badRequest("表单包含未定义字段：" + key);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (FormField field : fields) {
            Object value = input.get(field.getFieldCode());
            boolean empty = value == null || (value instanceof String text && text.isBlank())
                    || (value instanceof List<?> list && list.isEmpty());
            if (empty) {
                if (field.getRequired() != null && field.getRequired() == 1) {
                    throw BusinessException.badRequest("字段「" + field.getFieldLabel() + "」为必填项");
                }
                continue;
            }
            result.put(field.getFieldCode(), normalize(field, value));
        }
        return result;
    }

    private Object normalize(FormField field, Object value) {
        String label = field.getFieldLabel();
        String type = field.getFieldType();
        switch (type) {
            case "TEXT" -> {
                String text = String.valueOf(value);
                if (field.getMaxLength() != null && text.length() > field.getMaxLength()) {
                    throw BusinessException.badRequest("字段「" + label + "」长度不能超过 "
                            + field.getMaxLength() + " 个字符");
                }
                return text;
            }
            case "INTEGER" -> {
                BigDecimal number = toDecimal(value, label);
                if (number.stripTrailingZeros().scale() > 0) {
                    throw BusinessException.badRequest("字段「" + label + "」必须是整数");
                }
                checkRange(field, number, label);
                return number.longValueExact();
            }
            case "DECIMAL" -> {
                BigDecimal number = toDecimal(value, label);
                if (number.scale() > 2) {
                    throw BusinessException.badRequest("字段「" + label + "」最多支持两位小数");
                }
                checkRange(field, number, label);
                return number;
            }
            case "DATE" -> {
                String text = String.valueOf(value);
                try {
                    return LocalDate.parse(text).toString();
                } catch (DateTimeParseException ex) {
                    throw BusinessException.badRequest("字段「" + label + "」日期格式应为 yyyy-MM-dd");
                }
            }
            case "RADIO" -> {
                String text = String.valueOf(value);
                Set<String> allowed = optionValues(field);
                if (!allowed.contains(text)) {
                    throw BusinessException.badRequest("字段「" + label + "」的选项值不合法");
                }
                return text;
            }
            case "MULTI_SELECT" -> {
                if (!(value instanceof List<?> list)) {
                    throw BusinessException.badRequest("字段「" + label + "」应为多选列表");
                }
                Set<String> allowed = optionValues(field);
                List<String> selected = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (Object item : list) {
                    String text = String.valueOf(item);
                    if (!allowed.contains(text)) {
                        throw BusinessException.badRequest("字段「" + label + "」包含非法选项");
                    }
                    if (seen.add(text)) {
                        selected.add(text);
                    }
                }
                return selected;
            }
            case "BOOLEAN" -> {
                if (value instanceof Boolean bool) {
                    return bool;
                }
                String text = String.valueOf(value).toLowerCase(Locale.ROOT);
                if ("true".equals(text) || "false".equals(text)) {
                    return Boolean.parseBoolean(text);
                }
                throw BusinessException.badRequest("字段「" + label + "」应为布尔值");
            }
            default -> throw BusinessException.badRequest("字段「" + label + "」类型不受支持");
        }
    }

    private BigDecimal toDecimal(Object value, String label) {
        try {
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw BusinessException.badRequest("字段「" + label + "」必须是数字");
        }
    }

    private void checkRange(FormField field, BigDecimal value, String label) {
        if (field.getMinValue() != null && value.compareTo(field.getMinValue()) < 0) {
            throw BusinessException.badRequest("字段「" + label + "」不能小于 " + field.getMinValue());
        }
        if (field.getMaxValue() != null && value.compareTo(field.getMaxValue()) > 0) {
            throw BusinessException.badRequest("字段「" + label + "」不能大于 " + field.getMaxValue());
        }
    }

    private Set<String> optionValues(FormField field) {
        Set<String> values = new HashSet<>();
        if (field.getOptionsJson() == null || field.getOptionsJson().isBlank()) {
            return values;
        }
        try {
            List<FieldOptionRequest> options = objectMapper.readValue(field.getOptionsJson(),
                    new TypeReference<>() {
                    });
            options.forEach(option -> values.add(option.value()));
        } catch (Exception ex) {
            throw BusinessException.badRequest("字段「" + field.getFieldLabel() + "」的选项配置无法解析");
        }
        return values;
    }
}

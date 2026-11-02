package com.example.tax.dto;

import com.example.tax.model.FormField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FieldInput {

    private String fieldCode;
    private String fieldLabel;
    private String fieldType;
    private boolean required;
    private String minValue;
    private String maxValue;
    private String maxLength;
    private String options;
    private String helpText;

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getMinValue() {
        return minValue;
    }

    public void setMinValue(String minValue) {
        this.minValue = minValue;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(String maxValue) {
        this.maxValue = maxValue;
    }

    public String getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(String maxLength) {
        this.maxLength = maxLength;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public List<String> optionList() {
        List<String> result = new ArrayList<>();
        if (options == null) {
            return result;
        }
        for (String part : options.split("[,，\\n]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public FormField toFormField(int sortOrder) {
        FormField field = new FormField();
        field.setFieldCode(fieldCode == null ? "" : fieldCode.trim());
        field.setFieldLabel(fieldLabel == null ? "" : fieldLabel.trim());
        field.setFieldType(fieldType == null ? "" : fieldType.trim());
        field.setRequired(required);
        field.setMinValue(parseDecimal(minValue));
        field.setMaxValue(parseDecimal(maxValue));
        field.setMaxLength(parseInt(maxLength));
        List<String> optionsList = optionList();
        field.setOptionsJson(optionsList.isEmpty() ? null
                : com.example.tax.util.JsonUtil.toJson(optionsList));
        field.setHelpText(helpText == null ? null : helpText.trim());
        field.setSortOrder(sortOrder);
        field.setEnabled(true);
        return field;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

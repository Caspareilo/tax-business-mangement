package com.example.tax.model;

import java.math.BigDecimal;

public class FormField {

    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_INTEGER = "INTEGER";
    public static final String TYPE_AMOUNT = "AMOUNT";
    public static final String TYPE_DATE = "DATE";
    public static final String TYPE_RADIO = "RADIO";
    public static final String TYPE_MULTI = "MULTI";
    public static final String TYPE_BOOLEAN = "BOOLEAN";

    private long id;
    private long templateId;
    private String fieldCode;
    private String fieldLabel;
    private String fieldType;
    private boolean required;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Integer maxLength;
    private String optionsJson;
    private String helpText;
    private int sortOrder;
    private boolean enabled;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }

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

    public BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTypeLabel() {
        switch (fieldType == null ? "" : fieldType) {
            case TYPE_TEXT:
                return "文本";
            case TYPE_INTEGER:
                return "整数";
            case TYPE_AMOUNT:
                return "金额";
            case TYPE_DATE:
                return "日期";
            case TYPE_RADIO:
                return "单选";
            case TYPE_MULTI:
                return "多选";
            case TYPE_BOOLEAN:
                return "是否";
            default:
                return fieldType;
        }
    }

    /** Comma-separated option text used when re-editing a template draft. */
    public String getOptionsText() {
        return String.join(",", getOptionList());
    }

    public java.util.List<String> getOptionList() {
        java.util.List<String> options = new java.util.ArrayList<>();
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return options;
        }
        try {
            com.google.gson.JsonArray array =
                    com.google.gson.JsonParser.parseString(optionsJson).getAsJsonArray();
            for (com.google.gson.JsonElement element : array) {
                if (!element.isJsonNull()) {
                    options.add(element.getAsString());
                }
            }
        } catch (RuntimeException e) {
            return options;
        }
        return options;
    }
}

package com.example.tax.formtemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("form_field")
public class FormField {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String fieldCode;
    private String fieldLabel;
    private String fieldType;

    @TableField("is_required")
    private Integer required;

    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Integer maxLength;
    private String optionsJson;
    private String validationJson;
    private String helpText;
    private Integer sortOrder;
    private Integer enabled;
}

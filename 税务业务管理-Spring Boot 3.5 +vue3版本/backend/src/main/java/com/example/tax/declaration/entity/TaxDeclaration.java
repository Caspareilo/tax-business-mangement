package com.example.tax.declaration.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("tax_declaration")
public class TaxDeclaration {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String declarationNo;
    private Long taxpayerId;
    private String taxpayerNameSnapshot;
    private Long declarationTypeId;
    private String declarationTypeSnapshot;
    private Long templateId;
    private Integer templateVersionNo;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;
    private BigDecimal declaredAmount;
    private String formData;
    private Long createdBy;
    private LocalDateTime submittedAt;
    private LocalDateTime lastReviewedAt;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}

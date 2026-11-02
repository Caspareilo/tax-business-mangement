package com.example.tax.review.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("declaration_review")
public class DeclarationReview {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long declarationId;
    private Long reviewerId;
    private String reviewAction;
    private String reviewComment;
    private String fromStatus;
    private String toStatus;
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

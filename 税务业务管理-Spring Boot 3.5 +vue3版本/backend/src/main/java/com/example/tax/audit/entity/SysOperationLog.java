package com.example.tax.audit.entity;

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
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String operatorNameSnapshot;
    private String operationType;
    private String targetType;
    private String targetId;
    private String result;
    private String summary;
    private String traceId;
    private String ipMasked;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

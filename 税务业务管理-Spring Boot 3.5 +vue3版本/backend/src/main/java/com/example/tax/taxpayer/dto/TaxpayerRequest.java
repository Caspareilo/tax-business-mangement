package com.example.tax.taxpayer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TaxpayerRequest(
        @NotBlank(message = "请输入纳税人编号")
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,64}$", message = "编号只能包含字母、数字、下划线或短横线")
        String taxpayerCode,

        @NotBlank(message = "请输入纳税人名称")
        @Size(max = 200, message = "名称不能超过 200 个字符")
        String taxpayerName,

        @NotBlank(message = "请选择主体类型")
        @Pattern(regexp = "ENTERPRISE|INDIVIDUAL|OTHER", message = "主体类型不合法")
        String taxpayerType,

        @Size(max = 100, message = "联系人不能超过 100 个字符")
        String contactName,

        @Pattern(regexp = "^$|^[0-9+\\-]{5,40}$", message = "联系电话格式不正确")
        String contactPhone,

        @Size(max = 500, message = "备注不能超过 500 个字符")
        String remark
) {
}

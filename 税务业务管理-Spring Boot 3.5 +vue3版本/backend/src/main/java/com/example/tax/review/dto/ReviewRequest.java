package com.example.tax.review.dto;

import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @Size(max = 1000, message = "审核意见不能超过 1000 个字符")
        String comment
) {
}

package com.example.tax.review.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewVO {

    private Long id;
    private Long declarationId;
    private Long reviewerId;
    private String reviewerName;
    private String action;
    private String comment;
    private String fromStatus;
    private String toStatus;
    private LocalDateTime reviewedAt;
}

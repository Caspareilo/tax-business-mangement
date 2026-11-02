package com.example.tax.review.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewHistoryVO {

    private Long id;
    private Long declarationId;
    private String declarationNo;
    private String taxpayerName;
    private String declarationTypeName;
    private String action;
    private String comment;
    private Long reviewerId;
    private String reviewerName;
    private String fromStatus;
    private String toStatus;
    private LocalDateTime reviewedAt;
}

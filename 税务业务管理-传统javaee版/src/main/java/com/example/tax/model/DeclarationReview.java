package com.example.tax.model;

import java.sql.Timestamp;

public class DeclarationReview {

    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_RETURN = "RETURN";

    private long id;
    private long declarationId;
    private long reviewerId;
    private String reviewerName;
    private String reviewAction;
    private String reviewComment;
    private String fromStatus;
    private String toStatus;
    private Timestamp reviewedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDeclarationId() {
        return declarationId;
    }

    public void setDeclarationId(long declarationId) {
        this.declarationId = declarationId;
    }

    public long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewAction() {
        return reviewAction;
    }

    public void setReviewAction(String reviewAction) {
        this.reviewAction = reviewAction;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getActionLabel() {
        if (ACTION_APPROVE.equals(reviewAction)) {
            return "审核通过";
        }
        if (ACTION_RETURN.equals(reviewAction)) {
            return "退回修改";
        }
        return reviewAction;
    }
}

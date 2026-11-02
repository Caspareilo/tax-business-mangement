package com.example.tax.dao;

import com.example.tax.model.DeclarationReview;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ReviewDao {

    public long insert(Connection connection, DeclarationReview review) throws SQLException {
        return Jdbc.insert(connection,
                "INSERT INTO declaration_review (declaration_id, reviewer_id, review_action, "
                        + "review_comment, from_status, to_status, reviewed_at, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                review.getDeclarationId(), review.getReviewerId(), review.getReviewAction(),
                review.getReviewComment(), review.getFromStatus(), review.getToStatus());
    }

    public List<DeclarationReview> listByDeclaration(Connection connection, long declarationId)
            throws SQLException {
        return Jdbc.query(connection,
                "SELECT r.*, u.display_name AS reviewer_name FROM declaration_review r "
                        + "LEFT JOIN sys_user u ON u.id = r.reviewer_id "
                        + "WHERE r.declaration_id = ? ORDER BY r.reviewed_at DESC, r.id DESC",
                this::map, declarationId);
    }

    public boolean hasReviews(Connection connection, long declarationId) throws SQLException {
        return Jdbc.queryCount(connection,
                "SELECT COUNT(*) FROM declaration_review WHERE declaration_id = ?",
                declarationId) > 0;
    }

    private DeclarationReview map(ResultSet rs) throws SQLException {
        DeclarationReview review = new DeclarationReview();
        review.setId(rs.getLong("id"));
        review.setDeclarationId(rs.getLong("declaration_id"));
        review.setReviewerId(rs.getLong("reviewer_id"));
        review.setReviewerName(rs.getString("reviewer_name"));
        review.setReviewAction(rs.getString("review_action"));
        review.setReviewComment(rs.getString("review_comment"));
        review.setFromStatus(rs.getString("from_status"));
        review.setToStatus(rs.getString("to_status"));
        review.setReviewedAt(rs.getTimestamp("reviewed_at"));
        return review;
    }
}

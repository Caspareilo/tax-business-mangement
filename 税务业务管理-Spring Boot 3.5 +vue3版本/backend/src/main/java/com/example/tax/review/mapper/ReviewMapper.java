package com.example.tax.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.tax.review.entity.DeclarationReview;
import com.example.tax.review.vo.ReviewHistoryVO;
import com.example.tax.review.vo.ReviewVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewMapper extends BaseMapper<DeclarationReview> {

    @Select("""
            SELECT r.id, r.declaration_id, r.reviewer_id, u.display_name AS reviewer_name,
                   r.review_action AS action, r.review_comment AS comment,
                   r.from_status, r.to_status, r.reviewed_at
            FROM declaration_review r
            JOIN sys_user u ON u.id = r.reviewer_id
            WHERE r.declaration_id = #{declarationId}
            ORDER BY r.reviewed_at DESC, r.id DESC
            """)
    List<ReviewVO> selectByDeclaration(@Param("declarationId") Long declarationId);

    IPage<ReviewHistoryVO> selectHistoryPage(Page<?> page,
                                             @Param("reviewerId") Long reviewerId,
                                             @Param("action") String action,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to,
                                             @Param("keyword") String keyword);
}

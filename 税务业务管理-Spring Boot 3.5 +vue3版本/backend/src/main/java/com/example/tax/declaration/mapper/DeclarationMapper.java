package com.example.tax.declaration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.tax.declaration.entity.TaxDeclaration;
import com.example.tax.declaration.dto.DeclarationQuery;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface DeclarationMapper extends BaseMapper<TaxDeclaration> {

    @Select("SELECT * FROM tax_declaration WHERE id = #{id} AND deleted = 0")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    TaxDeclaration selectFreshById(@Param("id") Long id);

    IPage<DeclarationListRow> selectDeclarationPage(Page<?> page, @Param("q") DeclarationQuery query);

    List<DeclarationListRow> selectDeclarationList(@Param("q") DeclarationQuery query);

    @Update("""
            UPDATE tax_declaration
            SET status = #{toStatus}, submitted_at = COALESCE(#{submittedAt}, submitted_at),
                updated_at = NOW(), version = version + 1
            WHERE id = #{id} AND status = #{fromStatus} AND deleted = 0
            """)
    int updateStatusConditional(@Param("id") Long id,
                                @Param("fromStatus") String fromStatus,
                                @Param("toStatus") String toStatus,
                                @Param("submittedAt") LocalDateTime submittedAt);

    @Select("""
            SELECT COUNT(*) FROM tax_declaration
            WHERE taxpayer_id = #{taxpayerId}
              AND declaration_type_id = #{typeId}
              AND period_start = #{periodStart}
              AND period_end = #{periodEnd}
              AND status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED')
              AND deleted = 0
              AND id <> #{excludeId}
            """)
    long countEffectiveDuplicates(@Param("taxpayerId") Long taxpayerId,
                                  @Param("typeId") Long typeId,
                                  @Param("periodStart") LocalDate periodStart,
                                  @Param("periodEnd") LocalDate periodEnd,
                                  @Param("excludeId") Long excludeId);

    @Update("""
            UPDATE tax_declaration
            SET status = #{toStatus}, last_reviewed_at = #{reviewedAt},
                updated_at = NOW(), version = version + 1
            WHERE id = #{id} AND status = #{fromStatus} AND deleted = 0
            """)
    int updateStatusConditionalWithReview(@Param("id") Long id,
                                         @Param("fromStatus") String fromStatus,
                                         @Param("toStatus") String toStatus,
                                         @Param("reviewedAt") LocalDateTime reviewedAt);

    @Select("""
            SELECT COUNT(*) FROM tax_declaration
            WHERE status = 'PENDING_REVIEW' AND deleted = 0
              AND (#{scopeUserId} IS NULL OR created_by = #{scopeUserId})
              AND (#{typeId} IS NULL OR declaration_type_id = #{typeId})
            """)
    long countPending(@Param("scopeUserId") Long scopeUserId, @Param("typeId") Long typeId);
}

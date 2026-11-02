package com.example.tax.statistics.mapper;

import com.example.tax.statistics.vo.OverviewStat;
import com.example.tax.statistics.vo.StatusStat;
import com.example.tax.statistics.vo.TaxpayerTypeStat;
import com.example.tax.statistics.vo.TrendStat;
import com.example.tax.statistics.vo.TypeStat;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsMapper {

    OverviewStat selectOverview(@Param("scopeUserId") Long scopeUserId,
                                @Param("typeId") Long typeId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    List<TrendStat> selectTrend(@Param("scopeUserId") Long scopeUserId,
                                @Param("typeId") Long typeId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    List<StatusStat> selectByStatus(@Param("scopeUserId") Long scopeUserId,
                                    @Param("typeId") Long typeId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    List<TypeStat> selectByType(@Param("scopeUserId") Long scopeUserId,
                                @Param("typeId") Long typeId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    List<TaxpayerTypeStat> selectByTaxpayerType(@Param("scopeUserId") Long scopeUserId,
                                                @Param("typeId") Long typeId,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);
}

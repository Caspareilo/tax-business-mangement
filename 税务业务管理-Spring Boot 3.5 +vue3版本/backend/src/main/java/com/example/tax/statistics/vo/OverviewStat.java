package com.example.tax.statistics.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OverviewStat {

    private Long total;
    private Long draft;
    private Long pending;
    private Long approved;
    private Long returned;
    private Long withdrawn;
    private BigDecimal totalAmount;
}

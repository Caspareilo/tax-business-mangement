package com.example.tax.statistics.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TrendStat {

    private String month;
    private Long count;
    private BigDecimal amount;
}

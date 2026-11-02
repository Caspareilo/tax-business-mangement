package com.example.tax.statistics.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TypeStat {

    private String typeName;
    private Long count;
    private BigDecimal amount;
}

package com.example.tax.statistics.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TaxpayerTypeStat {

    private String taxpayerType;
    private Long count;
    private BigDecimal amount;
}

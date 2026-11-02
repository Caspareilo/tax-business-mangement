package com.example.tax.taxpayer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.tax.taxpayer.entity.Taxpayer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TaxpayerMapper extends BaseMapper<Taxpayer> {

    @Select("SELECT COUNT(*) FROM tax_declaration WHERE taxpayer_id = #{taxpayerId} AND deleted = 0")
    long countDeclarations(@Param("taxpayerId") Long taxpayerId);
}

package com.example.tax.formtemplate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.tax.formtemplate.entity.FormTemplate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FormTemplateMapper extends BaseMapper<FormTemplate> {

    @Select("""
            SELECT * FROM form_template
            WHERE declaration_type_id = #{typeId} AND status = 'PUBLISHED'
            ORDER BY version_no DESC LIMIT 1
            """)
    FormTemplate selectLatestPublished(@Param("typeId") Long typeId);

    @Select("SELECT COALESCE(MAX(version_no), 0) FROM form_template WHERE template_code = #{code}")
    int selectMaxVersion(@Param("code") String code);
}

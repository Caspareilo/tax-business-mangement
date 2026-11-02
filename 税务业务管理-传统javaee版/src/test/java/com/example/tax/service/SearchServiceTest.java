package com.example.tax.service;

import com.example.tax.dto.DeclarationFilter;
import com.example.tax.dto.Page;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.support.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchServiceTest extends DatabaseTestBase {

    @Test
    @DisplayName("SEARCH-01 组合条件检索申报")
    void search01CombinedFilters() throws Exception {
        DeclarationFilter filter = new DeclarationFilter();
        filter.setTaxpayerId(2L);
        filter.setStatus(TaxDeclaration.STATUS_PENDING);
        filter.setDeclarationTypeId(2L);

        Page<TaxDeclaration> page = new DeclarationService().list(analyst(), filter, 1, 10);

        assertEquals(1L, page.getTotal());
        assertEquals("TD20261012-000006", page.getItems().get(0).getDeclarationNo());
        assertEquals("青禾供应链（演示）有限公司",
                page.getItems().get(0).getTaxpayerNameSnapshot());
    }

    @Test
    @DisplayName("SEARCH-02 关键字匹配申报编号与纳税人名称")
    void search02Keyword() throws Exception {
        DeclarationFilter filter = new DeclarationFilter();
        filter.setKeyword("青禾");

        Page<TaxDeclaration> page = new DeclarationService().list(analyst(), filter, 1, 20);

        assertEquals(2L, page.getTotal());
    }

    @Test
    @DisplayName("SEARCH-03 经办员只能看到本人创建的申报")
    void search03ClerkScope() throws Exception {
        Page<TaxDeclaration> page = new DeclarationService()
                .list(clerk(), new DeclarationFilter(), 1, 50);

        assertEquals(12L, page.getTotal());
        assertEquals(12L, page.getItems().stream()
                .filter(item -> item.getCreatedBy() == 2L).count());
    }

    @Test
    @DisplayName("SEARCH-04 导出权限与结果条数")
    void search04Export() throws Exception {
        DeclarationFilter filter = new DeclarationFilter();
        filter.setDeclarationTypeId(1L);
        filter.setStatus(TaxDeclaration.STATUS_APPROVED);

        assertEquals(2, new DeclarationService()
                .exportRows(analyst(), filter, "test-trace").size());

        assertThrows(ForbiddenException.class,
                () -> new DeclarationService().exportRows(viewer(), filter, "test-trace"));
    }
}

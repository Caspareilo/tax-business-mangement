package com.example.tax.service;

import com.example.tax.dto.DeclarationForm;
import com.example.tax.dto.Page;
import com.example.tax.dto.TaxpayerFilter;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.Taxpayer;
import com.example.tax.support.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxpayerServiceTest extends DatabaseTestBase {

    @Test
    @DisplayName("TAX-01 纳税人编号重复时拒绝创建")
    void tax01DuplicateCodeRejected() throws Exception {
        Taxpayer taxpayer = new Taxpayer();
        taxpayer.setTaxpayerCode("DEMO-COMP-001");
        taxpayer.setTaxpayerName("重复编号演示公司");
        taxpayer.setTaxpayerType("COMPANY");
        taxpayer.setRegistrationStatus("ACTIVE");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> new TaxpayerService().create(admin(), taxpayer, "test-trace"));
        assertEquals("纳税人编号已存在", exception.getMessage());
        assertEquals(6L, new TaxpayerService().list(new TaxpayerFilter(), 1, 20).getTotal());
    }

    @Test
    @DisplayName("TAX-02 新增纳税人后可按关键字检索")
    void tax02CreateAndSearch() throws Exception {
        Taxpayer taxpayer = new Taxpayer();
        taxpayer.setTaxpayerCode("DEMO-NEW-001");
        taxpayer.setTaxpayerName("新程测试（演示）有限公司");
        taxpayer.setTaxpayerType("COMPANY");
        taxpayer.setContactName("测试联系人");
        taxpayer.setContactPhone("000-0000-0099");
        taxpayer.setRegistrationStatus("ACTIVE");
        taxpayer.setRemark("单元测试创建");

        long id = new TaxpayerService().create(admin(), taxpayer, "test-trace");
        assertTrue(id > 0);

        TaxpayerFilter filter = new TaxpayerFilter();
        filter.setKeyword("新程测试");
        Page<Taxpayer> page = new TaxpayerService().list(filter, 1, 10);
        assertEquals(1L, page.getTotal());
        assertEquals("DEMO-NEW-001", page.getItems().get(0).getTaxpayerCode());
    }

    @Test
    @DisplayName("TAX-03 停用纳税人不能新建申报")
    void tax03DisabledTaxpayerCannotDeclare() throws Exception {
        new TaxpayerService().setStatus(admin(), 3L, "DISABLED", "test-trace");

        DeclarationForm form = new DeclarationForm();
        form.setTaxpayerId(3L);
        form.setDeclarationTypeId(1L);
        form.setPeriodStart(LocalDate.of(2026, 9, 1));
        form.setPeriodEnd(LocalDate.of(2026, 9, 30));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> new DeclarationService().saveDraft(clerk(), form, "test-trace"));
        assertEquals("纳税人已停用", exception.getMessage());
        assertNotNull(exception.getFieldErrors().get("taxpayerId"));
    }
}

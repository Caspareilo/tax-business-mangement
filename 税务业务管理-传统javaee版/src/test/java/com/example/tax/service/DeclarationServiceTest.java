package com.example.tax.service;

import com.example.tax.dto.DeclarationForm;
import com.example.tax.exception.BusinessException;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.security.SessionUser;
import com.example.tax.support.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclarationServiceTest extends DatabaseTestBase {

    @Test
    @DisplayName("DECL-01 保存草稿写入快照并按金额字段汇总")
    void decl01SaveDraft() throws Exception {
        SessionUser actor = clerk();
        DeclarationService service = new DeclarationService();

        long id = service.saveDraft(actor, newForm(null, 1L, 1L), "test-trace");
        TaxDeclaration declaration = service.findDetail(id, actor);

        assertEquals(TaxDeclaration.STATUS_DRAFT, declaration.getStatus());
        assertEquals("晨星数字科技（演示）有限公司", declaration.getTaxpayerNameSnapshot());
        assertEquals("增值税及附加税费申报", declaration.getDeclarationTypeSnapshot());
        assertEquals(1, declaration.getTemplateVersionNo());
        assertEquals(0, new BigDecimal("120000.00")
                .compareTo(declaration.getDeclaredAmount()));
        assertEquals("100000.00", declaration.getFormValues().get("sales_amount"));

        TaxDeclaration editable = service.findForEdit(id, actor);
        assertEquals("月度", editable.getFormValues().get("declaration_cycle"));
    }

    @Test
    @DisplayName("DECL-02 提交后进入待审核，重复期间申报被拒绝")
    void decl02SubmitAndDuplicateGuard() throws Exception {
        SessionUser actor = clerk();
        DeclarationService service = new DeclarationService();

        long id = service.saveDraft(actor, newForm(null, 1L, 1L), "test-trace");
        service.submit(actor, id, "test-trace");
        assertEquals(TaxDeclaration.STATUS_PENDING,
                service.findDetail(id, actor).getStatus());

        long second = service.saveDraft(actor, newForm(null, 1L, 1L), "test-trace");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submit(actor, second, "test-trace"));
        assertTrue(exception.getMessage().contains("不能重复提交"));
    }

    @Test
    @DisplayName("DECL-03 待审核可撤回，已通过不可撤回或编辑")
    void decl03WithdrawRules() throws Exception {
        SessionUser actor = clerk();
        DeclarationService service = new DeclarationService();

        long id = service.saveDraft(actor, newForm(null, 1L, 1L), "test-trace");
        service.submit(actor, id, "test-trace");
        service.withdraw(actor, id, "test-trace");
        assertEquals(TaxDeclaration.STATUS_WITHDRAWN,
                service.findDetail(id, actor).getStatus());
        assertThrows(BusinessException.class, () -> service.findForEdit(id, actor));
        assertThrows(BusinessException.class,
                () -> service.withdraw(actor, id, "test-trace"));

        assertThrows(BusinessException.class,
                () -> service.findForEdit(4L, actor));
        assertThrows(BusinessException.class,
                () -> service.withdraw(actor, 4L, "test-trace"));
    }

    @Test
    @DisplayName("DECL-04 已退回申报可修改并重新提交")
    void decl04ReturnedCanResubmit() throws Exception {
        SessionUser actor = clerk();
        DeclarationService service = new DeclarationService();

        TaxDeclaration returned = service.findForEdit(3L, actor);
        assertEquals(TaxDeclaration.STATUS_RETURNED, returned.getStatus());

        DeclarationForm form = newForm(3L, 2L, 1L);
        form.setPeriodStart(LocalDate.of(2026, 8, 1));
        form.setPeriodEnd(LocalDate.of(2026, 8, 31));
        service.saveDraft(actor, form, "test-trace");
        service.submit(actor, 3L, "test-trace");

        TaxDeclaration resubmitted = service.findDetail(3L, actor);
        assertEquals(TaxDeclaration.STATUS_PENDING, resubmitted.getStatus());
        assertTrue(resubmitted.getVersion() > returned.getVersion());
    }

    private DeclarationForm newForm(Long id, Long taxpayerId, Long typeId) {
        DeclarationForm form = new DeclarationForm();
        form.setId(id);
        form.setTaxpayerId(taxpayerId);
        form.setDeclarationTypeId(typeId);
        form.setPeriodStart(LocalDate.of(2026, 9, 1));
        form.setPeriodEnd(LocalDate.of(2026, 9, 30));
        Map<String, String> values = new LinkedHashMap<>();
        values.put("sales_amount", "100000.00");
        values.put("deductible_amount", "20000.00");
        values.put("tax_method", "一般计税");
        values.put("declaration_cycle", "月度");
        values.put("has_contract", "true");
        values.put("remark", "单元测试草稿");
        form.setFormValues(values);
        return form;
    }
}

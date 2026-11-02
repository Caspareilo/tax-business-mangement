package com.example.tax.taxpayer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.tax.audit.service.AuditLogService;
import com.example.tax.common.api.PageResult;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.taxpayer.dto.TaxpayerRequest;
import com.example.tax.taxpayer.entity.Taxpayer;
import com.example.tax.taxpayer.mapper.TaxpayerMapper;
import com.example.tax.taxpayer.vo.TaxpayerVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaxpayerService {

    private final TaxpayerMapper taxpayerMapper;
    private final AuditLogService auditLogService;

    public TaxpayerService(TaxpayerMapper taxpayerMapper, AuditLogService auditLogService) {
        this.taxpayerMapper = taxpayerMapper;
        this.auditLogService = auditLogService;
    }

    public PageResult<TaxpayerVO> page(long page, long pageSize, String keyword, String type, String status) {
        LambdaQueryWrapper<Taxpayer> wrapper = new LambdaQueryWrapper<Taxpayer>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Taxpayer::getTaxpayerCode, keyword)
                        .or().like(Taxpayer::getTaxpayerName, keyword))
                .eq(type != null && !type.isBlank(), Taxpayer::getTaxpayerType, type)
                .eq(status != null && !status.isBlank(), Taxpayer::getRegistrationStatus, status)
                .orderByDesc(Taxpayer::getCreatedAt);
        Page<Taxpayer> resultPage = taxpayerMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<TaxpayerVO> items = resultPage.getRecords().stream().map(TaxpayerVO::from).toList();
        return PageResult.of(items, resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    public List<TaxpayerVO> options() {
        return taxpayerMapper.selectList(new LambdaQueryWrapper<Taxpayer>()
                        .eq(Taxpayer::getRegistrationStatus, "ACTIVE")
                        .orderByAsc(Taxpayer::getTaxpayerCode))
                .stream().map(TaxpayerVO::from).toList();
    }

    public TaxpayerVO get(Long id) {
        return TaxpayerVO.from(requireTaxpayer(id));
    }

    public Taxpayer requireTaxpayer(Long id) {
        Taxpayer taxpayer = taxpayerMapper.selectById(id);
        if (taxpayer == null) {
            throw BusinessException.notFound("纳税人不存在");
        }
        return taxpayer;
    }

    @Transactional
    public TaxpayerVO create(TaxpayerRequest request) {
        if (taxpayerMapper.selectCount(new LambdaQueryWrapper<Taxpayer>()
                .eq(Taxpayer::getTaxpayerCode, request.taxpayerCode())) > 0) {
            throw BusinessException.conflict("纳税人编号已存在");
        }
        Taxpayer taxpayer = new Taxpayer();
        taxpayer.setTaxpayerCode(request.taxpayerCode());
        taxpayer.setTaxpayerName(request.taxpayerName());
        taxpayer.setTaxpayerType(request.taxpayerType());
        taxpayer.setContactName(request.contactName());
        taxpayer.setContactPhone(request.contactPhone());
        taxpayer.setRegistrationStatus("ACTIVE");
        taxpayer.setRemark(request.remark());
        try {
            taxpayerMapper.insert(taxpayer);
        } catch (DuplicateKeyException ex) {
            throw BusinessException.conflict("纳税人编号已存在");
        }
        auditLogService.record("TAXPAYER_CREATE", "TAXPAYER", String.valueOf(taxpayer.getId()),
                "SUCCESS", "新增纳税人 " + taxpayer.getTaxpayerCode());
        return TaxpayerVO.from(taxpayer);
    }

    @Transactional
    public TaxpayerVO update(Long id, TaxpayerRequest request) {
        Taxpayer existing = requireTaxpayer(id);
        if (!existing.getTaxpayerCode().equals(request.taxpayerCode())) {
            throw BusinessException.badRequest("纳税人编号创建后不可修改");
        }
        Taxpayer update = new Taxpayer();
        update.setId(id);
        update.setTaxpayerName(request.taxpayerName());
        update.setTaxpayerType(request.taxpayerType());
        update.setContactName(request.contactName());
        update.setContactPhone(request.contactPhone());
        update.setRemark(request.remark());
        taxpayerMapper.updateById(update);
        auditLogService.record("TAXPAYER_UPDATE", "TAXPAYER", String.valueOf(id), "SUCCESS",
                "更新纳税人 " + existing.getTaxpayerCode());
        return TaxpayerVO.from(taxpayerMapper.selectById(id));
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Taxpayer existing = requireTaxpayer(id);
        Taxpayer update = new Taxpayer();
        update.setId(id);
        update.setRegistrationStatus(status);
        taxpayerMapper.updateById(update);
        auditLogService.record("TAXPAYER_STATUS", "TAXPAYER", String.valueOf(id), "SUCCESS",
                ("INACTIVE".equals(status) ? "停用" : "启用") + "纳税人 " + existing.getTaxpayerCode());
    }

    @Transactional
    public void delete(Long id) {
        Taxpayer existing = requireTaxpayer(id);
        if (taxpayerMapper.countDeclarations(id) > 0) {
            throw BusinessException.conflict("该纳税人已有申报记录，不能删除，请改用停用");
        }
        taxpayerMapper.deleteById(id);
        auditLogService.record("TAXPAYER_DELETE", "TAXPAYER", String.valueOf(id), "SUCCESS",
                "删除纳税人 " + existing.getTaxpayerCode());
    }
}

package com.example.tax.formtemplate.controller;

import com.example.tax.common.api.ApiResponse;
import com.example.tax.common.api.PageResult;
import com.example.tax.formtemplate.dto.FieldRequest;
import com.example.tax.formtemplate.dto.TemplateRequest;
import com.example.tax.formtemplate.service.FormTemplateService;
import com.example.tax.formtemplate.vo.FieldVO;
import com.example.tax.formtemplate.vo.TemplateVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/form-templates")
public class FormTemplateController {

    private final FormTemplateService templateService;

    public FormTemplateController(FormTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('form-template:read')")
    public ApiResponse<PageResult<TemplateVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long declarationTypeId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(templateService.page(page, Math.min(pageSize, 100), keyword,
                declarationTypeId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('form-template:read')")
    public ApiResponse<TemplateVO> get(@PathVariable Long id) {
        return ApiResponse.ok(templateService.get(id));
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAuthority('form-template:read')")
    public ApiResponse<TemplateVO> preview(@PathVariable Long id) {
        return ApiResponse.ok(templateService.preview(id));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('form-template:read')")
    public ApiResponse<List<TemplateVO>> versions(@PathVariable Long id) {
        return ApiResponse.ok(templateService.versions(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('form-template:write')")
    public ApiResponse<TemplateVO> create(@Valid @RequestBody TemplateRequest request) {
        return ApiResponse.ok(templateService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('form-template:write')")
    public ApiResponse<TemplateVO> update(@PathVariable Long id, @Valid @RequestBody TemplateRequest request) {
        return ApiResponse.ok(templateService.update(id, request));
    }

    @PostMapping("/{id}/fields")
    @PreAuthorize("hasAuthority('form-template:write')")
    public ApiResponse<FieldVO> addField(@PathVariable Long id, @Valid @RequestBody FieldRequest request) {
        return ApiResponse.ok(templateService.addField(id, request));
    }

    @PutMapping("/{id}/fields/{fieldId}")
    @PreAuthorize("hasAuthority('form-template:write')")
    public ApiResponse<FieldVO> updateField(@PathVariable Long id, @PathVariable Long fieldId,
                                            @Valid @RequestBody FieldRequest request) {
        return ApiResponse.ok(templateService.updateField(id, fieldId, request));
    }

    @DeleteMapping("/{id}/fields/{fieldId}")
    @PreAuthorize("hasAuthority('form-template:write')")
    public ApiResponse<Void> deleteField(@PathVariable Long id, @PathVariable Long fieldId) {
        templateService.deleteField(id, fieldId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('form-template:write')")
    public ApiResponse<TemplateVO> publish(@PathVariable Long id) {
        return ApiResponse.ok(templateService.publish(id));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('form-template:write')")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        templateService.disable(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/new-version")
    @PreAuthorize("hasAuthority('form-template:write')")
    public ApiResponse<TemplateVO> newVersion(@PathVariable Long id) {
        return ApiResponse.ok(templateService.newVersion(id));
    }
}

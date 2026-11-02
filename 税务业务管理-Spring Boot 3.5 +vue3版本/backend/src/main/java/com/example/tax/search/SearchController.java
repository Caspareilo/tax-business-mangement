package com.example.tax.search;

import com.example.tax.common.api.ApiResponse;
import com.example.tax.common.api.PageResult;
import com.example.tax.declaration.dto.DeclarationQuery;
import com.example.tax.declaration.service.DeclarationService;
import com.example.tax.declaration.vo.DeclarationVO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final DeclarationService declarationService;
    private final SearchService searchService;

    public SearchController(DeclarationService declarationService, SearchService searchService) {
        this.declarationService = declarationService;
        this.searchService = searchService;
    }

    @GetMapping("/declarations")
    @PreAuthorize("hasAuthority('search:read')")
    public ApiResponse<PageResult<DeclarationVO>> search(
            @ModelAttribute DeclarationQuery query,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(declarationService.page(query, page, pageSize));
    }

    @GetMapping("/declarations/export")
    @PreAuthorize("hasAuthority('declaration:export')")
    public ResponseEntity<byte[]> export(@ModelAttribute DeclarationQuery query) {
        byte[] body = searchService.exportCsv(query);
        String filename = "declarations-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(body);
    }
}

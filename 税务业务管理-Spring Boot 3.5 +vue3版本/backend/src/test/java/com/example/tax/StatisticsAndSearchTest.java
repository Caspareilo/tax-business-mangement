package com.example.tax;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestDatabaseConfig.class)
class StatisticsAndSearchTest extends IntegrationTestSupport {

    @Test
    void dashboardTotalsMatchSearchTotals() throws Exception {
        MockHttpSession admin = loginAdmin();
        MvcResult dashboard = mockMvc.perform(get("/api/v1/statistics/dashboard").session(admin))
                .andExpect(status().isOk())
                .andReturn();
        long total = body(dashboard).path("data").path("overview").path("total").asLong();
        long approved = body(dashboard).path("data").path("overview").path("approved").asLong();
        assertThat(total).isEqualTo(8);
        assertThat(approved).isEqualTo(3);

        mockMvc.perform(get("/api/v1/search/declarations").session(admin)
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value((int) total));
        mockMvc.perform(get("/api/v1/search/declarations").session(admin)
                        .param("status", "APPROVED").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value((int) approved));
    }

    @Test
    void searchSupportsKeywordAndPagination() throws Exception {
        MockHttpSession admin = loginAdmin();
        mockMvc.perform(get("/api/v1/search/declarations").session(admin)
                        .param("keyword", "星海").param("page", "1").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(2));
        mockMvc.perform(get("/api/v1/search/declarations").session(admin)
                        .param("declarationNo", "DEC-2026-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].declarationNo").value("DEC-2026-000001"));
    }

    @Test
    void exportReturnsCsvAndWritesAuditLog() throws Exception {
        MockHttpSession admin = loginAdmin();
        MvcResult exported = mockMvc.perform(get("/api/v1/search/declarations/export").session(admin)
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andReturn();
        String body = exported.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(body).startsWith("﻿申报编号,纳税人编号");
        assertThat(body).contains("DEC-2026-000003");
        assertThat(body).doesNotContain("DEC-2026-000001");

        mockMvc.perform(get("/api/v1/audit-logs").session(admin)
                        .param("operationType", "DECLARATION_EXPORT").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].summary")
                        .value(org.hamcrest.Matchers.containsString("导出申报 CSV")));
    }

    @Test
    void viewerCannotExportButCanSearch() throws Exception {
        MockHttpSession viewer = loginViewer();
        mockMvc.perform(get("/api/v1/search/declarations").session(viewer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/search/declarations/export").session(viewer))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditLogsNeverContainPasswords() throws Exception {
        MockHttpSession admin = loginAdmin();
        MvcResult result = mockMvc.perform(get("/api/v1/audit-logs").session(admin)
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andReturn();
        String logs = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(logs).doesNotContain(DEMO_PASSWORD);
        assertThat(logs.toLowerCase()).doesNotContain("\"password\"");
    }
}

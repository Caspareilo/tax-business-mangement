package com.example.tax;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestDatabaseConfig.class)
class TaxpayerAndTemplateTest extends IntegrationTestSupport {

    @Test
    void duplicateTaxpayerCodeIsRejected() throws Exception {
        MockHttpSession session = loginAdmin();
        mockMvc.perform(post("/api/v1/taxpayers").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"taxpayerCode":"TP20260001","taxpayerName":"重复编号测试",
                                 "taxpayerType":"ENTERPRISE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void taxpayerListFiltersAndPaginates() throws Exception {
        MockHttpSession session = loginAdmin();
        mockMvc.perform(get("/api/v1/taxpayers").session(session)
                        .param("type", "ENTERPRISE")
                        .param("page", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    void publishingTemplateWithoutFieldsIsRejected() throws Exception {
        MockHttpSession session = loginAdmin();
        MvcResult created = mockMvc.perform(post("/api/v1/form-templates").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateCode":"FT-EMPTY-01","templateName":"空模板",
                                 "declarationTypeId":1,"fields":[]}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long templateId = dataId(created);
        mockMvc.perform(post("/api/v1/form-templates/" + templateId + "/publish").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("至少需要一个启用字段")));
    }

    @Test
    void publishedTemplateCannotBeEditedInPlace() throws Exception {
        MockHttpSession session = loginAdmin();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/form-templates/1").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateCode":"FT-COMP-01","templateName":"改已发布模板",
                                 "declarationTypeId":1}
                                """))
                .andExpect(status().isConflict());
    }
}

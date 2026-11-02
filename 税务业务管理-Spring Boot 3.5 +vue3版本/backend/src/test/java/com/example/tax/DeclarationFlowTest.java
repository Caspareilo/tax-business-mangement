package com.example.tax;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestDatabaseConfig.class)
class DeclarationFlowTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BODY_TEMPLATE = """
            {"taxpayerId":1,"declarationTypeId":1,
             "periodStart":"%s","periodEnd":"%s","declaredAmount":1000.00,
             "formData":{"period_note":"集成测试数据","income_amount":1000,
                         "business_category":"SERVICE"}}
            """;

    private long createDraft(MockHttpSession session, String periodStart, String periodEnd) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/declarations").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_TEMPLATE.formatted(periodStart, periodEnd)))
                .andExpect(status().isOk())
                .andReturn();
        return dataId(result);
    }

    private void submit(MockHttpSession session, long id) throws Exception {
        mockMvc.perform(post("/api/v1/declarations/" + id + "/submit").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void createDraftPersistsAndIsReadable() throws Exception {
        MockHttpSession session = loginClerk();
        long id = createDraft(session, "2027-01-01", "2027-01-31");
        mockMvc.perform(get("/api/v1/declarations/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.taxpayerId").value(1))
                .andExpect(jsonPath("$.data.declarationNo").isNotEmpty());
    }

    @Test
    void submitRejectsMissingRequiredFormFields() throws Exception {
        MockHttpSession session = loginClerk();
        long id = createDraft(session, "2027-02-01", "2027-02-28");
        int updated = jdbcTemplate.update("UPDATE tax_declaration SET form_data = '{}' WHERE id = ?", id);
        org.assertj.core.api.Assertions.assertThat(updated).isEqualTo(1);
        String stored = jdbcTemplate.queryForObject(
                "SELECT form_data FROM tax_declaration WHERE id = ?", String.class, id);
        org.assertj.core.api.Assertions.assertThat(stored).isEqualTo("{}");
        mockMvc.perform(post("/api/v1/declarations/" + id + "/submit").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("必填")));
    }

    @Test
    void submitMovesToPendingAndCannotSubmitTwice() throws Exception {
        MockHttpSession session = loginClerk();
        long id = createDraft(session, "2027-03-01", "2027-03-31");
        submit(session, id);
        mockMvc.perform(get("/api/v1/declarations/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());
        mockMvc.perform(post("/api/v1/declarations/" + id + "/submit").session(session))
                .andExpect(status().isConflict());
    }

    @Test
    void duplicateDeclarationForSamePeriodIsRejected() throws Exception {
        MockHttpSession session = loginClerk();
        createDraft(session, "2027-04-01", "2027-04-30");
        mockMvc.perform(post("/api/v1/declarations").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_TEMPLATE.formatted("2027-04-01", "2027-04-30")))
                .andExpect(status().isConflict());
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tax_declaration WHERE taxpayer_id = 1 AND declaration_type_id = 1"
                        + " AND period_start = '2027-04-01' AND period_end = '2027-04-30'"
                        + " AND status IN ('DRAFT','PENDING_REVIEW','APPROVED')", Integer.class);
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    void reviewerCanApproveAndHistoryIsRecorded() throws Exception {
        MockHttpSession clerk = loginClerk();
        MockHttpSession reviewer = loginReviewer();
        long id = createDraft(clerk, "2027-05-01", "2027-05-31");
        submit(clerk, id);
        mockMvc.perform(post("/api/v1/declarations/" + id + "/review/approve").session(reviewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"材料齐全，模拟审核通过\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/declarations/" + id).session(clerk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        mockMvc.perform(get("/api/v1/declarations/" + id + "/reviews").session(clerk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].action").value("APPROVE"))
                .andExpect(jsonPath("$.data[0].reviewerName").value("审核人·周样例"));
    }

    @Test
    void approvingProcessedDeclarationReturnsConflict() throws Exception {
        MockHttpSession clerk = loginClerk();
        MockHttpSession reviewer = loginReviewer();
        long id = createDraft(clerk, "2027-06-01", "2027-06-30");
        submit(clerk, id);
        mockMvc.perform(post("/api/v1/declarations/" + id + "/review/approve").session(reviewer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/declarations/" + id + "/review/approve").session(reviewer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void returnRequiresCommentThenMovesToReturned() throws Exception {
        MockHttpSession clerk = loginClerk();
        MockHttpSession reviewer = loginReviewer();
        long id = createDraft(clerk, "2027-07-01", "2027-07-31");
        submit(clerk, id);
        mockMvc.perform(post("/api/v1/declarations/" + id + "/review/return").session(reviewer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/declarations/" + id + "/review/return").session(reviewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"模拟金额说明不完整，请补充\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/declarations/" + id).session(clerk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"));
    }

    @Test
    void reviewerCannotApproveOwnDeclaration() throws Exception {
        MockHttpSession admin = loginAdmin();
        long id = createDraft(admin, "2027-08-01", "2027-08-31");
        submit(admin, id);
        mockMvc.perform(post("/api/v1/declarations/" + id + "/review/approve").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"自审测试\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void submittedDeclarationCannotBeEditedByClerk() throws Exception {
        MockHttpSession clerk = loginClerk();
        long id = createDraft(clerk, "2027-09-01", "2027-09-30");
        submit(clerk, id);
        mockMvc.perform(put("/api/v1/declarations/" + id).session(clerk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_TEMPLATE.formatted("2027-09-01", "2027-09-30")))
                .andExpect(status().isConflict());
    }

    @Test
    void approvedDeclarationCannotBeEdited() throws Exception {
        MockHttpSession clerk = loginClerk();
        MockHttpSession reviewer = loginReviewer();
        long id = createDraft(clerk, "2027-10-01", "2027-10-31");
        submit(clerk, id);
        mockMvc.perform(post("/api/v1/declarations/" + id + "/review/approve").session(reviewer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/declarations/" + id).session(clerk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_TEMPLATE.formatted("2027-10-01", "2027-10-31")))
                .andExpect(status().isConflict());
    }

    @Test
    void clerkCanWithdrawPendingDeclaration() throws Exception {
        MockHttpSession clerk = loginClerk();
        long id = createDraft(clerk, "2027-11-01", "2027-11-30");
        submit(clerk, id);
        mockMvc.perform(post("/api/v1/declarations/" + id + "/withdraw").session(clerk))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/declarations/" + id).session(clerk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
    }
}

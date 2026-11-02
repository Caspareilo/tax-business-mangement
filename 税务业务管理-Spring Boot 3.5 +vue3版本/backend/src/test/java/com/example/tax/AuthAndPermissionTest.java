package com.example.tax;

import com.example.tax.user.entity.SysUser;
import com.example.tax.user.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestDatabaseConfig.class)
class AuthAndPermissionTest extends IntegrationTestSupport {

    @Autowired
    private SysUserMapper userMapper;

    @Test
    void loginSucceedsWithValidCredentials() throws Exception {
        MockHttpSession session = loginClerk();
        assertThat(session).isNotNull();
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("clerk"))
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        loginExpectFailure("clerk", "WrongPass123");
    }

    @Test
    void inactiveUserCannotLogin() throws Exception {
        SysUser update = new SysUser();
        update.setId(5L);
        update.setStatus("INACTIVE");
        userMapper.updateById(update);
        loginExpectFailure("viewer", DEMO_PASSWORD);
    }

    @Test
    void clerkCannotAccessUserManagement() throws Exception {
        MockHttpSession session = loginClerk();
        mockMvc.perform(get("/api/v1/users").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/taxpayers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCannotCreateTaxpayer() throws Exception {
        MockHttpSession session = loginViewer();
        mockMvc.perform(post("/api/v1/taxpayers").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"taxpayerCode":"TP90000001","taxpayerName":"只读越权测试",
                                 "taxpayerType":"ENTERPRISE"}
                                """))
                .andExpect(status().isForbidden());
    }
}

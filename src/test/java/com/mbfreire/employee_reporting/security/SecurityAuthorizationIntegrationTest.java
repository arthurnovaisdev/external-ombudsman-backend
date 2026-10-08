package com.mbfreire.employee_reporting.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "reports.retention.enabled=false",
                "notifications.admin.enabled=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedRouteWithoutAuthenticationReturns401() throws Exception {

        mockMvc.perform(
                        get("/api/reports/mine")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.erro")
                                .value(
                                        "Token ausente, inválido ou expirado"
                                )
                );
    }

    @Test
    @WithMockUser(
            username = "cliente.a",
            roles = "CLIENT"
    )
    void clientCannotAccessAdminReportRoute() throws Exception {

        mockMvc.perform(
                        get(
                                "/api/reports/admin/DEN-2026-ABCD2345"
                        )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                )
                .andExpect(
                        jsonPath("$.erro")
                                .value(
                                        "Você não tem permissão para acessar este recurso"
                                )
                );
    }

    @Test
    @WithMockUser(
            username = "cliente.a",
            roles = "CLIENT"
    )
    void clientCannotRegisterAnotherUser() throws Exception {

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "name": "Usuário Indevido",
                                          "username": "usuario.indevido",
                                          "contactEmail": "indevido@example.com",
                                          "password": "SenhaTeste123!"
                                        }
                                        """)
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                )
                .andExpect(
                        jsonPath("$.erro")
                                .value(
                                        "Você não tem permissão para acessar este recurso"
                                )
                );
    }

    @Test
    @WithMockUser(
            username = "cliente.a",
            roles = "CLIENT"
    )
    void clientCannotAccessUserManagement() throws Exception {

        mockMvc.perform(
                        get("/api/users")
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                );
    }

    @Test
    void apiHealthIsPublic() throws Exception {

        mockMvc.perform(
                        get("/api/health")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("UP")
                );
    }
}
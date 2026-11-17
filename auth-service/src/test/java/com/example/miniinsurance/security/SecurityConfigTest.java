package com.example.miniinsurance.security;

import com.example.miniinsurance.admin.AdminController;
import com.example.miniinsurance.auth.AuthController;
import com.example.miniinsurance.auth.AuthService;
import com.example.miniinsurance.common.HealthController;
import com.example.miniinsurance.customer.CustomerController;
import com.example.miniinsurance.policy.PolicyController;
import com.example.miniinsurance.policy.PolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization rules from SecurityConfig, exercised through the real filter chain.
 * The controllers are real; only the services and JWT collaborators are mocked.
 */
@WebMvcTest(controllers = {
        HealthController.class,
        AuthController.class,
        CustomerController.class,
        AdminController.class,
        PolicyController.class
})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PolicyService policyService;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void registerIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Silva",
                                 "email":"ana@example.com","password":"secret123"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    void customerEndpointWithoutTokenReturns401JsonBody() throws Exception {
        mockMvc.perform(get("/api/customer/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Authentication required")));
    }

    @Test
    void adminEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ana@example.com", roles = "CUSTOMER")
    void customerCanReachCustomerEndpoint() throws Exception {
        mockMvc.perform(get("/api/customer/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string("Authenticated customer: ana@example.com"));
    }

    @Test
    @WithMockUser(username = "ana@example.com", roles = "CUSTOMER")
    void customerAccessingAdminEndpointGets403NotNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Access denied")));
    }

    @Test
    @WithMockUser(username = "root@example.com", roles = "ADMIN")
    void adminCanReachAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string("Authenticated administrator: root@example.com"));
    }

    @Test
    @WithMockUser(username = "root@example.com", roles = "ADMIN")
    void adminCannotPurchasePolicyAsCustomer() throws Exception {
        mockMvc.perform(post("/api/customer/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quoteId\":1,\"startDate\":\"2027-01-01\"}"))
                .andExpect(status().isForbidden());
    }
}

package com.example.miniinsurance.policy;

import com.example.miniinsurance.common.ApiExceptionHandler;
import com.example.miniinsurance.common.exception.QuoteExpiredException;
import com.example.miniinsurance.common.exception.ResourceNotFoundException;
import com.example.miniinsurance.policy.dto.PolicyResponse;
import com.example.miniinsurance.policy.dto.PurchasePolicyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for POST /api/customer/policies. Standalone MockMvc, with the strict mapper
 * that rejects unknown properties, as configured in JacksonConfig.
 */
class PolicyControllerTest {

    private static final String EMAIL = "ana@example.com";
    private static final String PURCHASE_BODY = "{\"quoteId\":1,\"startDate\":\"2027-01-01\"}";

    private static final JsonMapper STRICT_MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final PolicyService service = mock(PolicyService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PolicyController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(STRICT_MAPPER))
            .build();

    private final Authentication customer = new UsernamePasswordAuthenticationToken(
            EMAIL, "n/a", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    @Test
    void purchase_returns201WithPolicy() throws Exception {
        when(service.purchasePolicy(any(PurchasePolicyRequest.class), eq(EMAIL)))
                .thenReturn(new PolicyResponse(
                        1L, "POL-ABCDEF12", PolicyType.MOTOR,
                        LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31),
                        new BigDecimal("687.50"), new BigDecimal("25000.00"),
                        PolicyStatus.ACTIVE));

        mockMvc.perform(post("/api/customer/policies")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURCHASE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyNumber").value("POL-ABCDEF12"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void purchase_clientSuppliedPremium_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/customer/policies")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quoteId\":1,\"startDate\":\"2027-01-01\",\"premium\":1}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).purchasePolicy(any(), any());
    }

    @Test
    void purchase_missingQuoteId_returns400() throws Exception {
        mockMvc.perform(post("/api/customer/policies")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2027-01-01\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).purchasePolicy(any(), any());
    }

    @Test
    void purchase_quoteOfAnotherCustomer_returns404() throws Exception {
        when(service.purchasePolicy(any(), eq(EMAIL)))
                .thenThrow(new ResourceNotFoundException("Quote not found"));

        mockMvc.perform(post("/api/customer/policies")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURCHASE_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void purchase_expiredQuote_returns410() throws Exception {
        when(service.purchasePolicy(any(), eq(EMAIL)))
                .thenThrow(new QuoteExpiredException("Quote has expired"));

        mockMvc.perform(post("/api/customer/policies")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURCHASE_BODY))
                .andExpect(status().isGone());
    }
}

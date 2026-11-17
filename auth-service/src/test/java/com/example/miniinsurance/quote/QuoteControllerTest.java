package com.example.miniinsurance.quote;

import com.example.miniinsurance.common.ApiExceptionHandler;
import com.example.miniinsurance.common.exception.ResourceNotFoundException;
import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.dto.QuoteRequest;
import com.example.miniinsurance.quote.dto.QuoteResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for quote endpoints. Standalone MockMvc: no database, no security filter chain.
 * Role and path rules come from SecurityConfig and are not exercised here.
 */
class QuoteControllerTest {

    private static final String EMAIL = "customer@example.com";
    private static final String URL = "/api/customer/quotes";

    private static final String VALID_HEALTH = """
            {"policyType":"HEALTH","dateOfBirth":"1995-06-01","heightCm":180,"weightKg":71.3,
             "smoker":false,"conditions":["ASTHMA"],"coverageAmount":100000}
            """;

    /** Same strictness as JacksonConfig: unknown properties must fail, not be ignored. */
    private static final JsonMapper STRICT_MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final QuoteService service = mock(QuoteService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new QuoteController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(STRICT_MAPPER))
            .build();

    private final Authentication customer = new UsernamePasswordAuthenticationToken(
            EMAIL, "n/a", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    @Test
    void createsQuoteAndReturns201() throws Exception {
        when(service.createQuote(any(QuoteRequest.class), eq(EMAIL))).thenReturn(response());

        mvc.perform(post(URL).principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_HEALTH))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("QUOTED"))
                .andExpect(jsonPath("$.annualPremium").value(1200.00));
    }

    @Test
    void unknownPolicyTypeIsBadRequest() throws Exception {
        String body = VALID_HEALTH.replace("HEALTH", "CAR");

        mvc.perform(post(URL).principal(customer).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(service, never()).createQuote(any(), any());
    }

    @Test
    void clientSuppliedPremiumIsRejectedAsUnknownField() throws Exception {
        String body = VALID_HEALTH.replace("\"coverageAmount\":100000", "\"coverageAmount\":100000,\"premium\":1");

        mvc.perform(post(URL).principal(customer).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(service, never()).createQuote(any(), any());
    }

    @Test
    void missingRequiredFieldReturnsFieldErrors() throws Exception {
        String body = VALID_HEALTH.replace("\"dateOfBirth\":\"1995-06-01\",", "");

        mvc.perform(post(URL).principal(customer).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("dateOfBirth")));
    }

    @Test
    void malformedValueIsNotEchoedBack() throws Exception {
        String body = VALID_HEALTH.replace("1995-06-01", "not-a-date");

        mvc.perform(post(URL).principal(customer).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("not-a-date"))));
    }

    @Test
    void otherCustomersQuoteIsNotFound() throws Exception {
        when(service.getOwnQuote(99L, EMAIL)).thenThrow(new ResourceNotFoundException("Quote not found"));

        mvc.perform(get(URL + "/99").principal(customer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Quote not found"));
    }

    private static QuoteResponse response() {
        return new QuoteResponse(
                1L,
                PolicyType.HEALTH,
                QuoteStatus.QUOTED,
                new BigDecimal("1200.00"),
                "USD",
                new BigDecimal("100000"),
                List.of(),
                LocalDateTime.of(2026, 12, 17, 10, 0),
                null
        );
    }
}

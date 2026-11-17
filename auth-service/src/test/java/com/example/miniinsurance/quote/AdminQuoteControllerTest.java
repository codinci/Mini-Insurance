package com.example.miniinsurance.quote;

import com.example.miniinsurance.common.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminQuoteControllerTest {

    private final QuoteService service = mock(QuoteService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminQuoteController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void unknownStatusFilterIsBadRequest() throws Exception {
        mvc.perform(get("/api/admin/quotes").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
        verify(service, never()).listAllQuotes(null, null, 0, 20);
    }

    @Test
    void unknownPolicyTypeFilterIsBadRequest() throws Exception {
        mvc.perform(get("/api/admin/quotes").param("policyType", "BOAT"))
                .andExpect(status().isBadRequest());
    }
}

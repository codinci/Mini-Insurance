package com.example.miniinsurance.quote;

import com.example.miniinsurance.common.PageResponse;
import com.example.miniinsurance.common.exception.InvalidInputException;
import com.example.miniinsurance.common.exception.ResourceNotFoundException;
import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.dto.HealthQuoteRequest;
import com.example.miniinsurance.quote.dto.QuoteResponse;
import com.example.miniinsurance.quote.rating.HealthPremiumCalculator;
import com.example.miniinsurance.quote.rating.HomePremiumCalculator;
import com.example.miniinsurance.quote.rating.LifePremiumCalculator;
import com.example.miniinsurance.quote.rating.MotorPremiumCalculator;
import com.example.miniinsurance.quote.rating.TravelPremiumCalculator;
import com.example.miniinsurance.user.Role;
import com.example.miniinsurance.user.User;
import com.example.miniinsurance.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class QuoteServiceTest {

    private static final String EMAIL = "customer@example.com";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);

    private final QuoteRepository quotes = mock(QuoteRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final QuoteService service = new QuoteService(
            quotes,
            users,
            List.of(
                    new HealthPremiumCalculator(),
                    new MotorPremiumCalculator(),
                    new HomePremiumCalculator(),
                    new TravelPremiumCalculator(),
                    new LifePremiumCalculator()
            ),
            JsonMapper.builder().build(),
            CLOCK
    );

    private final User customer = User.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Customer")
            .email(EMAIL)
            .password("encoded")
            .role(Role.CUSTOMER)
            .build();

    @BeforeEach
    void setUp() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(quotes.save(any(Quote.class))).thenAnswer(invocation -> {
            Quote quote = invocation.getArgument(0);
            if (quote.getId() == null) {
                quote.setId(42L);
            }
            return quote;
        });
    }

    @Test
    void createsQuotedHealthQuoteValidForThirtyDays() {
        QuoteResponse response = service.createQuote(healthRequest(LocalDate.of(1995, 6, 1)), EMAIL);

        assertThat(response.status()).isEqualTo(QuoteStatus.QUOTED);
        assertThat(response.annualPremium()).isEqualByComparingTo("1200.00");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.expiresAt()).isEqualTo(LocalDateTime.of(2026, 12, 17, 10, 0));
        assertThat(response.breakdown()).isNotEmpty();
    }

    @Test
    void declinedQuoteHasNoPremiumAndReason() {
        QuoteResponse response = service.createQuote(healthRequest(LocalDate.of(1940, 1, 1)), EMAIL);

        assertThat(response.status()).isEqualTo(QuoteStatus.DECLINED);
        assertThat(response.annualPremium()).isNull();
        assertThat(response.declineReason()).isEqualTo("HEALTH_AGE_OUT_OF_RANGE");
        assertThat(response.breakdown()).isEmpty();
    }

    @Test
    void ratingValuesAreNotWrittenToLogs(CapturedOutput output) {
        service.createQuote(healthRequest(LocalDate.of(1995, 6, 1)), EMAIL);

        verify(quotes).save(any(Quote.class));
        assertThat(output.getAll()).doesNotContain("1995-06-01").doesNotContain("71.3");
    }

    @Test
    void unknownCustomerIsNotFoundAndNothingIsSaved() {
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createQuote(healthRequest(LocalDate.of(1995, 6, 1)), "nobody@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(quotes, never()).save(any());
    }

    @Test
    void otherCustomersQuoteIsNotFound() {
        when(quotes.findByIdAndCustomer(99L, customer)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnQuote(99L, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Quote not found");
    }

    @Test
    void pastValidityWindowReadsAsExpiredWithoutStoredStatusChange() {
        Quote stored = Quote.builder()
                .id(5L)
                .customer(customer)
                .policyType(PolicyType.HEALTH)
                .status(QuoteStatus.QUOTED)
                .coverageAmount(new BigDecimal("100000"))
                .annualPremium(new BigDecimal("1200.00"))
                .currency("USD")
                .ratingVersion("2026.1")
                .breakdown("[]")
                .expiresAt(LocalDateTime.of(2026, 11, 16, 10, 0))
                .build();
        when(quotes.findByIdAndCustomer(5L, customer)).thenReturn(Optional.of(stored));

        QuoteResponse response = service.getOwnQuote(5L, EMAIL);

        assertThat(response.status()).isEqualTo(QuoteStatus.EXPIRED);
        assertThat(stored.getStatus()).isEqualTo(QuoteStatus.QUOTED);
    }

    @Test
    void pageSizeIsCappedAtOneHundred() {
        assertThat(QuoteService.pageRequest(0, 500).getPageSize()).isEqualTo(100);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> QuoteService.pageRequest(-1, 20))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void listReturnsStableEnvelope() {
        when(quotes.findByCustomer(any(User.class), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));

        PageResponse<QuoteResponse> page = service.listOwnQuotes(EMAIL, 0, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    private static HealthQuoteRequest healthRequest(LocalDate dateOfBirth) {
        return new HealthQuoteRequest(
                dateOfBirth,
                new BigDecimal("180"),
                new BigDecimal("71.3"),
                false,
                Set.of(),
                new BigDecimal("100000")
        );
    }
}

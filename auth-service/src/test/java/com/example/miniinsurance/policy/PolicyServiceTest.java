package com.example.miniinsurance.policy;

import com.example.miniinsurance.common.exception.BusinessRuleException;
import com.example.miniinsurance.common.exception.ConflictException;
import com.example.miniinsurance.common.exception.QuoteExpiredException;
import com.example.miniinsurance.common.exception.ResourceNotFoundException;
import com.example.miniinsurance.policy.dto.PolicyResponse;
import com.example.miniinsurance.policy.dto.PurchasePolicyRequest;
import com.example.miniinsurance.quote.Quote;
import com.example.miniinsurance.quote.QuoteRepository;
import com.example.miniinsurance.quote.QuoteStatus;
import com.example.miniinsurance.user.Role;
import com.example.miniinsurance.user.User;
import com.example.miniinsurance.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyServiceTest {

    private static final String EMAIL = "customer@example.com";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime FUTURE_EXPIRY = LocalDateTime.of(2026, 12, 17, 10, 0);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 18);

    private final PolicyRepository policies = mock(PolicyRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final QuoteRepository quotes = mock(QuoteRepository.class);
    private final PolicyService service = new PolicyService(policies, users, quotes, CLOCK);

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
        when(policies.saveAndFlush(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quotes.saveAndFlush(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void purchaseCreatesActivePolicyWithOneYearTermAndMarksQuotePurchased() {
        Quote quote = quote(PolicyType.HEALTH, QuoteStatus.QUOTED, FUTURE_EXPIRY);
        givenQuote(quote);

        PolicyResponse response = purchase(quote, TOMORROW);

        assertThat(response.status()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(response.startDate()).isEqualTo(TOMORROW);
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2027, 11, 17));
        assertThat(response.premium()).isEqualByComparingTo("1200.00");
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.PURCHASED);
    }

    @Test
    void lifePolicyRunsForQuotedTermYears() {
        Quote quote = quote(PolicyType.LIFE, QuoteStatus.QUOTED, FUTURE_EXPIRY);
        quote.setTermYears(10);
        givenQuote(quote);

        assertThat(purchase(quote, TOMORROW).endDate()).isEqualTo(LocalDate.of(2036, 11, 17));
    }

    @Test
    void travelPolicyIsFixedToTheQuotedTrip() {
        Quote quote = quote(PolicyType.TRAVEL, QuoteStatus.QUOTED, FUTURE_EXPIRY);
        quote.setTripStart(LocalDate.of(2027, 1, 1));
        quote.setTripEnd(LocalDate.of(2027, 1, 7));
        givenQuote(quote);

        assertThat(purchase(quote, LocalDate.of(2027, 1, 1)).endDate()).isEqualTo(LocalDate.of(2027, 1, 7));
    }

    @Test
    void travelStartMustMatchTripStart() {
        Quote quote = quote(PolicyType.TRAVEL, QuoteStatus.QUOTED, FUTURE_EXPIRY);
        quote.setTripStart(LocalDate.of(2027, 1, 1));
        quote.setTripEnd(LocalDate.of(2027, 1, 7));
        givenQuote(quote);

        assertThatThrownBy(() -> purchase(quote, LocalDate.of(2027, 1, 2)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void alreadyPurchasedQuoteIsConflict() {
        Quote quote = quote(PolicyType.HEALTH, QuoteStatus.PURCHASED, FUTURE_EXPIRY);
        givenQuote(quote);

        assertThatThrownBy(() -> purchase(quote, TOMORROW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void declinedQuoteIsBusinessRuleViolation() {
        Quote quote = quote(PolicyType.HEALTH, QuoteStatus.DECLINED, FUTURE_EXPIRY);
        givenQuote(quote);

        assertThatThrownBy(() -> purchase(quote, TOMORROW)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void expiredQuoteIsGone() {
        Quote quote = quote(PolicyType.HEALTH, QuoteStatus.QUOTED, LocalDateTime.of(2026, 11, 16, 10, 0));
        givenQuote(quote);

        assertThatThrownBy(() -> purchase(quote, TOMORROW)).isInstanceOf(QuoteExpiredException.class);
    }

    @Test
    void quoteExpiringExactlyNowIsExpired() {
        Quote quote = quote(PolicyType.HEALTH, QuoteStatus.QUOTED, LocalDateTime.of(2026, 11, 17, 10, 0));
        givenQuote(quote);

        assertThatThrownBy(() -> purchase(quote, TOMORROW)).isInstanceOf(QuoteExpiredException.class);
    }

    @Test
    void startDateMoreThanSixtyDaysAheadIsRejected() {
        Quote quote = quote(PolicyType.HEALTH, QuoteStatus.QUOTED, FUTURE_EXPIRY);
        givenQuote(quote);

        assertThatThrownBy(() -> purchase(quote, LocalDate.of(2027, 1, 17)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void startDateInThePastIsRejected() {
        Quote quote = quote(PolicyType.HEALTH, QuoteStatus.QUOTED, FUTURE_EXPIRY);
        givenQuote(quote);

        assertThatThrownBy(() -> purchase(quote, LocalDate.of(2026, 11, 16)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void anotherCustomersQuoteIsNotFound() {
        when(quotes.findByIdAndCustomer(7L, customer)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.purchasePolicy(new PurchasePolicyRequest(7L, TOMORROW), EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
        org.mockito.Mockito.verify(policies, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Test
    void losingConcurrentPurchaseSurfacesOptimisticLockFailure() {
        Quote quote = quote(PolicyType.HEALTH, QuoteStatus.QUOTED, FUTURE_EXPIRY);
        givenQuote(quote);
        when(quotes.saveAndFlush(any(Quote.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Quote.class, 7L));

        assertThatThrownBy(() -> purchase(quote, TOMORROW))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private void givenQuote(Quote quote) {
        when(quotes.findByIdAndCustomer(quote.getId(), customer)).thenReturn(Optional.of(quote));
    }

    private PolicyResponse purchase(Quote quote, LocalDate startDate) {
        return service.purchasePolicy(new PurchasePolicyRequest(quote.getId(), startDate), EMAIL);
    }

    private Quote quote(PolicyType type, QuoteStatus status, LocalDateTime expiresAt) {
        return Quote.builder()
                .id(7L)
                .customer(customer)
                .policyType(type)
                .status(status)
                .coverageAmount(new BigDecimal("100000"))
                .annualPremium(new BigDecimal("1200.00"))
                .currency("USD")
                .ratingVersion("2026.1")
                .breakdown("[]")
                .expiresAt(expiresAt)
                .build();
    }
}

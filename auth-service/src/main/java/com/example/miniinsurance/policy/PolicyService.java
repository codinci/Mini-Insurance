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
import com.example.miniinsurance.user.User;
import com.example.miniinsurance.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PolicyService {

    static final int MAX_START_DAYS_AHEAD = 60;

    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final QuoteRepository quoteRepository;
    private final Clock clock;

    public PolicyService(
            PolicyRepository policyRepository,
            UserRepository userRepository,
            QuoteRepository quoteRepository,
            Clock clock
    ) {
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.quoteRepository = quoteRepository;
        this.clock = clock;
    }

    /**
     * Converts an owned, unexpired QUOTED quote into an ACTIVE policy.
     * Concurrent purchases of the same quote: the @Version check on the quote and the unique
     * quote_id on the policy both reject the loser, which surfaces as 409.
     */
    @Transactional
    public PolicyResponse purchasePolicy(PurchasePolicyRequest request, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Quote quote = quoteRepository.findByIdAndCustomer(request.quoteId(), customer)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found"));

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = LocalDate.now(clock);

        if (quote.getStatus() == QuoteStatus.PURCHASED) {
            throw new ConflictException("Quote has already been purchased");
        }
        if (quote.getStatus() == QuoteStatus.DECLINED) {
            throw new BusinessRuleException("Declined quotes cannot be purchased");
        }
        if (quote.isExpiredAt(now)) {
            throw new QuoteExpiredException("Quote has expired");
        }

        LocalDate startDate = request.startDate();
        if (startDate.isBefore(today) || startDate.isAfter(today.plusDays(MAX_START_DAYS_AHEAD))) {
            throw new BusinessRuleException("Start date must be today or within 60 days");
        }

        LocalDate endDate = coverageEnd(quote, startDate);

        quote.setStatus(QuoteStatus.PURCHASED);
        quoteRepository.saveAndFlush(quote);

        Policy policy = Policy.builder()
                .policyNumber(generatePolicyNumber())
                .policyType(quote.getPolicyType())
                .startDate(startDate)
                .endDate(endDate)
                .premium(quote.getAnnualPremium())
                .coverageAmount(quote.getCoverageAmount())
                .status(PolicyStatus.ACTIVE)
                .customer(customer)
                .quote(quote)
                .build();

        return toResponse(policyRepository.saveAndFlush(policy));
    }

    /**
     * Default term is one year less one day. LIFE uses its term in years.
     * TRAVEL is fixed to the quoted trip, so the start date must match the trip start.
     */
    private LocalDate coverageEnd(Quote quote, LocalDate startDate) {
        return switch (quote.getPolicyType()) {
            case TRAVEL -> {
                if (!startDate.equals(quote.getTripStart())) {
                    throw new BusinessRuleException("Start date must match the trip start date");
                }
                yield quote.getTripEnd();
            }
            case LIFE -> startDate.plusYears(quote.getTermYears()).minusDays(1);
            default -> startDate.plusYears(1).minusDays(1);
        };
    }

    private String generatePolicyNumber() {
        return "POL-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    private PolicyResponse toResponse(Policy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getPolicyNumber(),
                policy.getPolicyType(),
                policy.getStartDate(),
                policy.getEndDate(),
                policy.getPremium(),
                policy.getCoverageAmount(),
                policy.getStatus()
        );
    }
}

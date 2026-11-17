package com.example.miniinsurance.quote;

import com.example.miniinsurance.common.PageResponse;
import com.example.miniinsurance.common.exception.InvalidInputException;
import com.example.miniinsurance.common.exception.ResourceNotFoundException;
import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.dto.BreakdownLine;
import com.example.miniinsurance.quote.dto.QuoteRequest;
import com.example.miniinsurance.quote.dto.QuoteResponse;
import com.example.miniinsurance.quote.rating.PolicyTerms;
import com.example.miniinsurance.quote.rating.PremiumCalculator;
import com.example.miniinsurance.quote.rating.RatingOutcome;
import com.example.miniinsurance.user.User;
import com.example.miniinsurance.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@Service
public class QuoteService {

    static final String CURRENCY = "USD";
    static final String RATING_VERSION = "2026.1";
    static final int VALIDITY_DAYS = 30;
    static final int MAX_PAGE_SIZE = 100;

    private static final TypeReference<List<BreakdownLine>> BREAKDOWN_TYPE = new TypeReference<>() {
    };

    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final EnumMap<PolicyType, PremiumCalculator> calculators;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public QuoteService(
            QuoteRepository quoteRepository,
            UserRepository userRepository,
            List<PremiumCalculator> calculatorList,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.quoteRepository = quoteRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.calculators = new EnumMap<>(PolicyType.class);
        for (PremiumCalculator calculator : calculatorList) {
            calculators.put(calculator.policyType(), calculator);
        }
    }

    @Transactional
    public QuoteResponse createQuote(QuoteRequest request, String customerEmail) {
        User customer = findCustomer(customerEmail);
        PremiumCalculator calculator = calculators.get(request.policyType());
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        calculator.validate(request, today);
        RatingOutcome outcome = calculator.rate(request, today);
        PolicyTerms terms = outcome.terms();

        Quote quote = Quote.builder()
                .customer(customer)
                .policyType(request.policyType())
                .status(outcome.isDeclined() ? QuoteStatus.DECLINED : QuoteStatus.QUOTED)
                .coverageAmount(request.coverage())
                .annualPremium(outcome.annualPremium())
                .currency(CURRENCY)
                .declineReason(outcome.declineReason())
                .ratingVersion(RATING_VERSION)
                .inputs(writeJson(request))
                .breakdown(writeJson(outcome.breakdown()))
                .termYears(terms == null ? null : terms.termYears())
                .tripStart(terms == null ? null : terms.tripStart())
                .tripEnd(terms == null ? null : terms.tripEnd())
                .expiresAt(now.plusDays(VALIDITY_DAYS))
                .build();

        return toResponse(quoteRepository.save(quote), now);
    }

    @Transactional(readOnly = true)
    public QuoteResponse getOwnQuote(Long id, String customerEmail) {
        User customer = findCustomer(customerEmail);
        Quote quote = quoteRepository.findByIdAndCustomer(id, customer)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found"));
        return toResponse(quote, LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public PageResponse<QuoteResponse> listOwnQuotes(String customerEmail, int page, int size) {
        User customer = findCustomer(customerEmail);
        LocalDateTime now = LocalDateTime.now(clock);
        Page<Quote> result = quoteRepository.findByCustomer(customer, pageRequest(page, size));
        return PageResponse.from(result.map(quote -> toResponse(quote, now)));
    }

    @Transactional(readOnly = true)
    public QuoteResponse getAnyQuote(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found"));
        return toResponse(quote, LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public PageResponse<QuoteResponse> listAllQuotes(QuoteStatus status, PolicyType policyType, int page, int size) {
        LocalDateTime now = LocalDateTime.now(clock);
        Page<Quote> result = quoteRepository.findAll(filter(status, policyType, now), pageRequest(page, size));
        return PageResponse.from(result.map(quote -> toResponse(quote, now)));
    }

    /**
     * Filters on the effective status. EXPIRED is stored as QUOTED with a passed expiry,
     * so it maps to a stored-status check plus a time check.
     */
    private static Specification<Quote> filter(QuoteStatus status, PolicyType policyType, LocalDateTime now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (policyType != null) {
                predicates.add(cb.equal(root.get("policyType"), policyType));
            }
            if (status != null) {
                switch (status) {
                    case EXPIRED -> {
                        predicates.add(cb.equal(root.get("status"), QuoteStatus.QUOTED));
                        predicates.add(cb.lessThanOrEqualTo(root.get("expiresAt"), now));
                    }
                    case QUOTED -> {
                        predicates.add(cb.equal(root.get("status"), QuoteStatus.QUOTED));
                        predicates.add(cb.greaterThan(root.get("expiresAt"), now));
                    }
                    default -> predicates.add(cb.equal(root.get("status"), status));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static PageRequest pageRequest(int page, int size) {
        if (page < 0) {
            throw new InvalidInputException("page must be zero or greater");
        }
        if (size < 1) {
            throw new InvalidInputException("size must be at least 1");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private QuoteResponse toResponse(Quote quote, LocalDateTime now) {
        List<BreakdownLine> breakdown = quote.getBreakdown() == null
                ? List.of()
                : readBreakdown(quote.getBreakdown());
        return new QuoteResponse(
                quote.getId(),
                quote.getPolicyType(),
                effectiveStatus(quote, now),
                quote.getAnnualPremium(),
                quote.getCurrency(),
                quote.getCoverageAmount(),
                breakdown,
                quote.getExpiresAt(),
                quote.getDeclineReason()
        );
    }

    private static QuoteStatus effectiveStatus(Quote quote, LocalDateTime now) {
        if (quote.getStatus() == QuoteStatus.QUOTED && quote.isExpiredAt(now)) {
            return QuoteStatus.EXPIRED;
        }
        return quote.getStatus();
    }

    private User findCustomer(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private String writeJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    private List<BreakdownLine> readBreakdown(String json) {
        return objectMapper.readValue(json, BREAKDOWN_TYPE);
    }
}

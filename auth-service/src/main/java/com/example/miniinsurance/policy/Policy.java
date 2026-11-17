package com.example.miniinsurance.policy;

import com.example.miniinsurance.quote.Quote;
import com.example.miniinsurance.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType policyType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal premium;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal coverageAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /** Null for policies created before quotes existed. Unique: one quote yields at most one policy. */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "quote_id", unique = true)
    private Quote quote;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
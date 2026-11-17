package com.example.miniinsurance.quote;

import com.example.miniinsurance.common.crypto.EncryptedStringConverter;
import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType policyType;

    /** Stored status. EXPIRED is derived at read time and never persisted. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal coverageAmount;

    /** Null when DECLINED. Annual for most types; the full trip premium for TRAVEL. */
    @Column(precision = 15, scale = 2)
    private BigDecimal annualPremium;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 64)
    private String declineReason;

    @Column(nullable = false, length = 32)
    private String ratingVersion;

    /** Encrypted JSON of the submitted rating inputs. Null after retention purge. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String inputs;

    /** Encrypted JSON of the premium breakdown. Null after retention purge. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String breakdown;

    /** LIFE only: policy term in whole years. */
    private Integer termYears;

    /** TRAVEL only: fixed cover window. Purchase must start on tripStart. */
    private LocalDate tripStart;

    private LocalDate tripEnd;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    /** A quote is expired once now reaches expiresAt (inclusive). */
    public boolean isExpiredAt(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

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

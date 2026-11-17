package com.example.miniinsurance.quote;

import com.example.miniinsurance.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long>, JpaSpecificationExecutor<Quote> {

    /** Owner-scoped lookup: another customer's id yields empty, which callers turn into 404. */
    Optional<Quote> findByIdAndCustomer(Long id, User customer);

    Page<Quote> findByCustomer(User customer, Pageable pageable);

    /**
     * Retention purge: clears rating inputs and breakdown of unpurchased quotes
     * that expired before the cutoff. Keeps id, status, owner, premium and ratingVersion.
     */
    @Modifying
    @Query("""
            update Quote q
               set q.inputs = null, q.breakdown = null, q.tripStart = null, q.tripEnd = null
             where q.status <> :purchased
               and q.expiresAt < :cutoff
               and q.inputs is not null
            """)
    int purgeInputsExpiredBefore(
            @Param("purchased") QuoteStatus purchased,
            @Param("cutoff") LocalDateTime cutoff
    );
}

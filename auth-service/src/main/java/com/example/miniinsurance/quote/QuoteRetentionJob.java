package com.example.miniinsurance.quote;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Purges rating inputs of unpurchased quotes 90 days after they expired.
 * Runs daily at 03:15. Logs only the count, never data.
 */
@Slf4j
@Component
public class QuoteRetentionJob {

    static final long RETENTION_DAYS = 90;

    private final QuoteRepository quoteRepository;
    private final Clock clock;

    public QuoteRetentionJob(QuoteRepository quoteRepository, Clock clock) {
        this.quoteRepository = quoteRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public int purgeExpiredInputs() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(RETENTION_DAYS);
        int purged = quoteRepository.purgeInputsExpiredBefore(QuoteStatus.PURCHASED, cutoff);
        log.info("Quote retention purge cleared inputs for {} quote(s)", purged);
        return purged;
    }
}

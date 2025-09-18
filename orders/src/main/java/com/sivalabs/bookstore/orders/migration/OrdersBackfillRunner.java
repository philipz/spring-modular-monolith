package com.sivalabs.bookstore.orders.migration;

import com.sivalabs.bookstore.orders.config.BackfillProperties;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "orders.backfill", name = "enabled", havingValue = "true")
public class OrdersBackfillRunner implements ApplicationRunner {

    private final BackfillProperties properties;
    private final OrdersBackfillService ordersBackfillService;
    private final OrdersBackfillAuditor auditor;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.debug("Orders backfill disabled");
            return;
        }

        int limit = properties.getRecordLimit() != null ? properties.getRecordLimit() : 0;
        if (limit <= 0) {
            log.warn("Orders backfill record limit is not configured. Skipping backfill execution.");
            return;
        }

        LocalDateTime since = null;
        if (properties.getLookbackDays() != null && properties.getLookbackDays() > 0) {
            since = LocalDateTime.now().minusDays(properties.getLookbackDays());
        }

        BackfillRequest request = new BackfillRequest(since, limit);
        long auditId = auditor.recordStart(request);
        try {
            int processed = ordersBackfillService.runBackfill(request);
            auditor.recordSuccess(auditId, processed);
        } catch (Exception ex) {
            auditor.recordFailure(auditId, ex);
            throw ex;
        }
    }
}

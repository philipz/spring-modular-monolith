package com.sivalabs.bookstore.orders.migration;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrdersBackfillAuditor {

    private final JdbcTemplate ordersBackfillJdbcTemplate;

    public long recordStart(BackfillRequest request) {
        String sql =
                """
                INSERT INTO orders.backfill_audit (started_at, source_since, record_limit, status)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;
        LocalDateTime now = LocalDateTime.now();
        Timestamp since = request.since() != null ? Timestamp.valueOf(request.since()) : null;
        Long id = ordersBackfillJdbcTemplate.queryForObject(
                sql, Long.class, Timestamp.valueOf(now), since, request.limit(), "RUNNING");
        log.info("Backfill audit record {} created", id);
        return id != null ? id : -1L;
    }

    public void recordSuccess(long auditId, int processed) {
        if (auditId <= 0) {
            return;
        }
        String sql =
                """
                UPDATE orders.backfill_audit
                SET completed_at = ?, records_processed = ?, status = ?
                WHERE id = ?
                """;
        ordersBackfillJdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.now()), processed, "COMPLETED", auditId);
    }

    public void recordFailure(long auditId, Throwable throwable) {
        if (auditId <= 0) {
            return;
        }
        String message = throwable != null ? throwable.getMessage() : "Unknown error";
        if (StringUtils.hasLength(message) && message.length() > 1024) {
            message = message.substring(0, 1024);
        }
        String sql =
                """
                UPDATE orders.backfill_audit
                SET completed_at = ?, status = ?, error_message = ?
                WHERE id = ?
                """;
        ordersBackfillJdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.now()), "FAILED", message, auditId);
    }
}

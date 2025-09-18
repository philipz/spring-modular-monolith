package com.sivalabs.bookstore.orders.migration;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LegacyOrderReader {

    private static final String BASE_SELECT =
            "SELECT order_number, customer_name, customer_email, customer_phone, delivery_address, "
                    + "product_code, product_name, product_price, quantity, status, created_at, updated_at "
                    + "FROM orders.orders";

    private final JdbcTemplate ordersBackfillJdbcTemplate;

    public LegacyOrderReader(@Qualifier("legacyOrdersJdbcTemplate") JdbcTemplate ordersBackfillJdbcTemplate) {
        this.ordersBackfillJdbcTemplate = ordersBackfillJdbcTemplate;
    }

    public List<LegacyOrderRecord> fetchOrders(BackfillRequest request) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();

        if (request.since() != null) {
            sql.append(" WHERE created_at >= ?");
            params.add(Timestamp.valueOf(request.since()));
        }

        sql.append(" ORDER BY created_at, order_number LIMIT ?");
        params.add(request.limit());

        return ordersBackfillJdbcTemplate.query(sql.toString(), params.toArray(), legacyOrderRowMapper());
    }

    private RowMapper<LegacyOrderRecord> legacyOrderRowMapper() {
        return (ResultSet rs, int rowNum) -> new LegacyOrderRecord(
                rs.getString("order_number"),
                rs.getString("customer_name"),
                rs.getString("customer_email"),
                rs.getString("customer_phone"),
                rs.getString("delivery_address"),
                rs.getString("product_code"),
                rs.getString("product_name"),
                readPrice(rs.getString("product_price")),
                rs.getInt("quantity"),
                rs.getString("status"),
                readDateTime(rs.getTimestamp("created_at")),
                readDateTime(rs.getTimestamp("updated_at")));
    }

    private BigDecimal readPrice(String value) {
        if (!StringUtils.hasText(value)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.trim());
    }

    private LocalDateTime readDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}

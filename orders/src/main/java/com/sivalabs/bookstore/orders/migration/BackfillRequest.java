package com.sivalabs.bookstore.orders.migration;

import java.time.LocalDateTime;

public record BackfillRequest(LocalDateTime since, int limit) {}

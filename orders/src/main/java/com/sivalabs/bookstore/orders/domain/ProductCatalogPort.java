package com.sivalabs.bookstore.orders.domain;

import java.math.BigDecimal;

public interface ProductCatalogPort {

    void validate(String productCode, BigDecimal price);
}

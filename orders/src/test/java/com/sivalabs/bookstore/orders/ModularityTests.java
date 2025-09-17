package com.sivalabs.bookstore.orders;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    private static final ApplicationModules modules = ApplicationModules.of(OrdersApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}

package com.sivalabs.bookstore.orders;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

    private static final ApplicationModules modules = ApplicationModules.of(OrdersApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void createsModuleDocumentation() {
        new Documenter(modules, Documenter.Options.defaults().withOutputFolder("target/modulith-docs"))
                .writeDocumentation()
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}

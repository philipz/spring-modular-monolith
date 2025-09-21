package com.sivalabs.bookstore.orders.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class OrderDetailsTemplateRenderingTests {

    private static TemplateEngine templateEngine;

    @BeforeAll
    static void setupEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        templateEngine.addDialect(new LayoutDialect());
    }

    @Test
    void rendersFallbackWhenOrderMissing() {
        Context context = new Context();
        context.setVariable("order", null);

        String html = templateEngine.process("order_details", context);

        assertThat(html).contains("We couldn't find that order.");
    }

    @Test
    void escapesOrderFieldsAndMarksInputsReadonlyForAccessibility() {
        TestOrderItem item =
                new TestOrderItem(
                        "<script>item()</script>", new BigDecimal("12.50"), 1);
        TestCustomer customer =
                new TestCustomer(
                        "<script>name()</script>",
                        "customer@example.com",
                        "1234567890");
        Context context = new Context();
        context.setVariable(
                "order",
                new TestOrder(
                        "ORD-1",
                        "PLACED",
                        item,
                        customer,
                        "123 Main St",
                        new BigDecimal("12.50")));

        String html = templateEngine.process("order_details", context);

        assertThat(html).contains("&lt;script&gt;item()&lt;/script&gt;");
        assertThat(html).doesNotContain("<script>item()</script>");
        assertThat(html).contains("aria-readonly=\"true\"");
    }

    private static final class TestOrder {
        private final String orderNumber;
        private final String status;
        private final TestOrderItem item;
        private final TestCustomer customer;
        private final String deliveryAddress;
        private final BigDecimal totalAmount;

        private TestOrder(
                String orderNumber,
                String status,
                TestOrderItem item,
                TestCustomer customer,
                String deliveryAddress,
                BigDecimal totalAmount) {
            this.orderNumber = orderNumber;
            this.status = status;
            this.item = item;
            this.customer = customer;
            this.deliveryAddress = deliveryAddress;
            this.totalAmount = totalAmount;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public String getStatus() {
            return status;
        }

        public TestOrderItem getItem() {
            return item;
        }

        public TestCustomer getCustomer() {
            return customer;
        }

        public String getDeliveryAddress() {
            return deliveryAddress;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }

    private static final class TestOrderItem {
        private final String name;
        private final BigDecimal price;
        private final Integer quantity;

        private TestOrderItem(String name, BigDecimal price, Integer quantity) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public Integer getQuantity() {
            return quantity;
        }
    }

    private static final class TestCustomer {
        private final String name;
        private final String email;
        private final String phone;

        private TestCustomer(String name, String email, String phone) {
            this.name = name;
            this.email = email;
            this.phone = phone;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }
    }
}

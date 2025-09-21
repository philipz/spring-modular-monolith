package com.sivalabs.bookstore.orders.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

class CartTemplateRenderingTests {

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
    void rendersEmptyCartMessageWhenNoItem() {
        WebContext context = createWebContext();
        context.setVariable("cart", new TestCart(null, BigDecimal.ZERO));

        String html = templateEngine.process("partials/cart", context);

        assertThat(html).contains("Your cart is empty");
    }

    @Test
    void escapesCartItemFields() {
        TestCartItem item = new TestCartItem("P-123", "<script>alert('xss')</script>", new BigDecimal("10.00"), 2);
        WebContext context = createWebContext();
        context.setVariable("cart", new TestCart(item, new BigDecimal("20.00")));

        String html = templateEngine.process("partials/cart", context);

        assertThat(html).contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
        assertThat(html).doesNotContain("<script>alert('xss')</script>");
    }

    @Test
    void rendersFallbackWhenCartMissing() {
        WebContext context = createWebContext();
        context.setVariable("cart", null);

        String html = templateEngine.process("partials/cart", context);

        assertThat(html).contains("We couldn't load your cart right now");
    }

    private static WebContext createWebContext() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        return new WebContext(application.buildExchange(request, response));
    }

    private static final class TestCart {
        private final TestCartItem item;
        private final BigDecimal totalAmount;

        private TestCart(TestCartItem item, BigDecimal totalAmount) {
            this.item = item;
            this.totalAmount = totalAmount;
        }

        public TestCartItem getItem() {
            return item;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }

    private static final class TestCartItem {
        private final String code;
        private final String name;
        private final BigDecimal price;
        private final Integer quantity;

        private TestCartItem(String code, String name, BigDecimal price, Integer quantity) {
            this.code = code;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public String getCode() {
            return code;
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
}

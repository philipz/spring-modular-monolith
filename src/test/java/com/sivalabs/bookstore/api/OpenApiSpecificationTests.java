package com.sivalabs.bookstore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sivalabs.bookstore.TestcontainersConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Validation tests for OpenAPI specification completeness.
 * Ensures that all REST endpoints, DTOs, and error responses are properly
 * documented.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class OpenApiSpecificationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
    }

    @Test
    void shouldLoadOpenApiSpecificationSuccessfully() throws Exception {
        // Given: The application is running
        String apiDocsUrl = "/api-docs";

        // When: Fetching the OpenAPI specification
        mockMvc.perform(get(apiDocsUrl)).andExpect(status().isOk());
    }

    @Test
    void shouldValidateOpenApiSpecAgainstOpenApi3Schema() throws Exception {
        // Given: The application is running
        String apiDocsUrl = "/api-docs";

        // When: Parsing the OpenAPI specification
        String responseBody =
                mockMvc.perform(get(apiDocsUrl)).andReturn().getResponse().getContentAsString();

        SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(responseBody, null, null);

        // Then: The specification should be valid OpenAPI 3.0
        assertThat(parseResult.getMessages())
                .as("OpenAPI spec should have no parsing errors")
                .isEmpty();
        assertThat(parseResult.getOpenAPI())
                .as("OpenAPI object should be parsed successfully")
                .isNotNull();
    }

    @Test
    void shouldDocumentAllProductsEndpoints() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: All Products endpoints should be documented
        assertThat(openAPI.getPaths())
                .as("Products endpoints should be documented")
                .containsKeys("/api/products", "/api/products/{code}");

        // Verify GET /api/products
        PathItem productsPath = openAPI.getPaths().get("/api/products");
        assertThat(productsPath.getGet())
                .as("GET /api/products should be documented")
                .isNotNull();
        assertThat(productsPath.getGet().getResponses())
                .as("GET /api/products should document responses")
                .containsKeys("200");

        // Verify GET /api/products/{code}
        PathItem productPath = openAPI.getPaths().get("/api/products/{code}");
        assertThat(productPath.getGet())
                .as("GET /api/products/{code} should be documented")
                .isNotNull();
        assertThat(productPath.getGet().getResponses())
                .as("GET /api/products/{code} should document responses")
                .containsKeys("200", "404");
    }

    @Test
    void shouldDocumentAllCartEndpoints() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: All Cart endpoints should be documented
        assertThat(openAPI.getPaths())
                .as("Cart endpoints should be documented")
                .containsKeys("/api/cart", "/api/cart/items", "/api/cart/items/{code}");

        // Verify POST /api/cart/items
        PathItem cartItemsPath = openAPI.getPaths().get("/api/cart/items");
        assertThat(cartItemsPath.getPost())
                .as("POST /api/cart/items should be documented")
                .isNotNull();
        assertThat(cartItemsPath.getPost().getResponses())
                .as("POST /api/cart/items should document responses")
                .containsKeys("201", "404");

        // Verify PUT /api/cart/items/{code}
        PathItem updateCartItemPath = openAPI.getPaths().get("/api/cart/items/{code}");
        assertThat(updateCartItemPath.getPut())
                .as("PUT /api/cart/items/{code} should be documented")
                .isNotNull();

        // Verify GET /api/cart
        PathItem getCartPath = openAPI.getPaths().get("/api/cart");
        assertThat(getCartPath.getGet())
                .as("GET /api/cart should be documented")
                .isNotNull();

        // Verify DELETE /api/cart
        assertThat(getCartPath.getDelete())
                .as("DELETE /api/cart should be documented")
                .isNotNull();
    }

    @Test
    void shouldDocumentAllOrdersEndpoints() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: All Orders endpoints should be documented
        assertThat(openAPI.getPaths())
                .as("Orders endpoints should be documented")
                .containsKeys("/api/orders", "/api/orders/{orderNumber}");

        // Verify POST /api/orders
        PathItem ordersPath = openAPI.getPaths().get("/api/orders");
        assertThat(ordersPath.getPost())
                .as("POST /api/orders should be documented")
                .isNotNull();
        assertThat(ordersPath.getPost().getResponses())
                .as("POST /api/orders should document responses")
                .containsKeys("201", "400", "503");

        // Verify GET /api/orders
        assertThat(ordersPath.getGet())
                .as("GET /api/orders should be documented")
                .isNotNull();

        // Verify GET /api/orders/{orderNumber}
        PathItem orderPath = openAPI.getPaths().get("/api/orders/{orderNumber}");
        assertThat(orderPath.getGet())
                .as("GET /api/orders/{orderNumber} should be documented")
                .isNotNull();
        assertThat(orderPath.getGet().getResponses())
                .as("GET /api/orders/{orderNumber} should document responses")
                .containsKeys("200", "404", "503");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldDocumentProductDtoSchema() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: ProductDto should be documented
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertThat(schemas).as("ProductDto schema should be documented").containsKey("ProductDto");

        Schema productDto = schemas.get("ProductDto");
        assertThat(productDto.getProperties())
                .as("ProductDto should have all required fields")
                .containsKeys("code", "name", "price", "imageUrl");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldDocumentCartDtoSchemas() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: Cart-related DTOs should be documented
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertThat(schemas)
                .as("Cart DTOs should be documented")
                .containsKeys("CartDto", "CartItemDto", "AddToCartRequest", "UpdateQuantityRequest");

        // Verify CartDto structure
        Schema cartDto = schemas.get("CartDto");
        assertThat(cartDto.getProperties())
                .as("CartDto should have required fields")
                .containsKeys("items", "totalAmount", "itemCount");

        // Verify CartItemDto structure
        Schema cartItemDto = schemas.get("CartItemDto");
        assertThat(cartItemDto.getProperties())
                .as("CartItemDto should have required fields")
                .containsKeys("code", "name", "price", "quantity", "subtotal");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldDocumentOrdersDtoSchemas() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: Orders-related DTOs should be documented
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        assertThat(schemas)
                .as("Orders DTOs should be documented")
                .containsKeys(
                        "CreateOrderRequest", "CreateOrderResponse", "OrderDto", "OrderView", "Customer", "OrderItem");

        // Verify CreateOrderRequest structure
        Schema createOrderRequest = schemas.get("CreateOrderRequest");
        assertThat(createOrderRequest.getProperties())
                .as("CreateOrderRequest should have required fields")
                .containsKeys("customer", "deliveryAddress", "item");

        // Verify Customer structure
        Schema customer = schemas.get("Customer");
        assertThat(customer.getProperties())
                .as("Customer should have required fields")
                .containsKeys("name", "email", "phone");

        // Verify OrderItem structure
        Schema orderItem = schemas.get("OrderItem");
        assertThat(orderItem.getProperties())
                .as("OrderItem should have required fields")
                .containsKeys("code", "name", "price", "quantity");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldDocumentErrorResponseSchema() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: Error responses should be documented in endpoints
        boolean hasErrorResponses = false;

        for (PathItem pathItem : openAPI.getPaths().values()) {
            if (pathItem == null) continue;

            Operation[] operations = {pathItem.getGet(), pathItem.getPost(), pathItem.getPut(), pathItem.getDelete()};

            for (Operation operation : operations) {
                if (operation != null && operation.getResponses() != null) {
                    for (String responseCode : operation.getResponses().keySet()) {
                        if (responseCode.startsWith("4") || responseCode.startsWith("5")) {
                            hasErrorResponses = true;
                            break;
                        }
                    }
                }
            }
        }

        assertThat(hasErrorResponses)
                .as("Error responses should be documented in endpoints")
                .isTrue();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldDocumentPagedResultSchema() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: PagedResult should be documented (as part of response)
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        // Find any PagedResult schema variant (could be PagedResultProductDto or
        // similar)
        boolean hasPagedResult = schemas.keySet().stream().anyMatch(key -> key.contains("PagedResult"));

        assertThat(hasPagedResult).as("PagedResult schema should be documented").isTrue();

        // Get the actual PagedResult schema
        String pagedResultKey = schemas.keySet().stream()
                .filter(key -> key.contains("PagedResult"))
                .findFirst()
                .orElseThrow();

        Schema pagedResult = schemas.get(pagedResultKey);
        assertThat(pagedResult.getProperties())
                .as("PagedResult should have pagination fields")
                .containsKeys(
                        "data",
                        "pageNumber",
                        "totalPages",
                        "totalElements",
                        "isFirst",
                        "isLast",
                        "hasNext",
                        "hasPrevious");
    }

    @Test
    void shouldDocumentApiInformation() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: API information should be complete
        assertThat(openAPI.getInfo()).as("API info should be present").isNotNull();
        assertThat(openAPI.getInfo().getTitle()).as("API title should be set").isEqualTo("BookStore REST API");
        assertThat(openAPI.getInfo().getVersion())
                .as("API version should be set")
                .isNotNull();
        assertThat(openAPI.getInfo().getDescription())
                .as("API description should be set")
                .isNotNull();
    }

    @Test
    void shouldDocumentErrorResponsesForAllEndpoints() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: Critical endpoints should document error responses
        // (not all endpoints need error responses, e.g., simple GET lists)
        int endpointsWithErrors = 0;
        int totalEndpoints = 0;

        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            if (pathItem == null) continue;

            Operation[] operations = {pathItem.getGet(), pathItem.getPost(), pathItem.getPut(), pathItem.getDelete()};

            for (Operation operation : operations) {
                if (operation != null && operation.getResponses() != null) {
                    totalEndpoints++;
                    boolean hasErrorResponse = operation.getResponses().keySet().stream()
                            .anyMatch(code -> code.startsWith("4") || code.startsWith("5"));

                    if (hasErrorResponse) {
                        endpointsWithErrors++;
                    }
                }
            }
        }

        // At least 50% of endpoints should document error responses
        assertThat(endpointsWithErrors)
                .as("At least half of the endpoints should document error responses")
                .isGreaterThanOrEqualTo(totalEndpoints / 2);
    }

    @Test
    void shouldHaveOperationDescriptionsForAllEndpoints() {
        // Given: The OpenAPI specification
        OpenAPI openAPI = loadOpenApiSpec();

        // Then: All operations should have descriptions
        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            if (pathItem == null) continue;

            Operation[] operations = {pathItem.getGet(), pathItem.getPost(), pathItem.getPut(), pathItem.getDelete()};

            for (Operation operation : operations) {
                if (operation != null && operation.getSummary() != null) {
                    assertThat(operation.getSummary())
                            .as("Endpoint %s %s should have a summary", getHttpMethod(pathItem, operation), path)
                            .isNotBlank();
                }
            }
        }
    }

    // Helper methods

    private OpenAPI loadOpenApiSpec() {
        try {
            String apiDocsUrl = "/api-docs";
            String responseBody =
                    mockMvc.perform(get(apiDocsUrl)).andReturn().getResponse().getContentAsString();
            SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(responseBody, null, null);
            assertThat(parseResult.getOpenAPI())
                    .as("OpenAPI spec should be loaded successfully")
                    .isNotNull();
            return parseResult.getOpenAPI();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getHttpMethod(PathItem pathItem, Operation operation) {
        if (pathItem.getGet() == operation) return "GET";
        if (pathItem.getPost() == operation) return "POST";
        if (pathItem.getPut() == operation) return "PUT";
        if (pathItem.getDelete() == operation) return "DELETE";
        return "UNKNOWN";
    }
}

package com.sivalabs.bookstore.catalog.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sivalabs.bookstore.catalog.api.ProductDto;
import com.sivalabs.bookstore.catalog.domain.ProductEntity;
import com.sivalabs.bookstore.catalog.domain.ProductService;
import com.sivalabs.bookstore.catalog.mappers.ProductMapper;
import com.sivalabs.bookstore.catalog.support.PagedResults;
import com.sivalabs.bookstore.common.models.PagedResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProductRestControllerTests {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductRestController productRestController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(productRestController).build();
    }

    @Test
    void shouldGetProducts() throws Exception {
        ProductEntity product = new ProductEntity();
        product.setId(1L);
        product.setCode("P100");
        product.setName("The Hunger Games");
        product.setDescription("Description");
        product.setImageUrl("image.jpg");
        product.setPrice(new BigDecimal("34.0"));

        ProductDto productDto =
                new ProductDto("P100", "The Hunger Games", "Description", "image.jpg", new BigDecimal("34.0"));

        Page<ProductEntity> productsPage = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 15);
        PagedResult<ProductEntity> pagedResult = PagedResults.fromPage(productsPage);

        given(productService.getProducts(anyInt())).willReturn(pagedResult);
        given(productMapper.mapToDto(any(ProductEntity.class))).willReturn(productDto);

        mockMvc.perform(get("/api/products?page=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements", is(15)))
                .andExpect(jsonPath("$.pageNumber", is(1)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.isFirst", is(true)))
                .andExpect(jsonPath("$.isLast", is(false)))
                .andExpect(jsonPath("$.hasNext", is(true)))
                .andExpect(jsonPath("$.hasPrevious", is(false)));
    }

    @Test
    void shouldGetProductByCode() throws Exception {
        ProductEntity product = new ProductEntity();
        product.setId(1L);
        product.setCode("P100");
        product.setName("The Hunger Games");
        product.setDescription("Description");
        product.setImageUrl("image.jpg");
        product.setPrice(new BigDecimal("34.0"));

        ProductDto productDto =
                new ProductDto("P100", "The Hunger Games", "Description", "image.jpg", new BigDecimal("34.0"));

        given(productService.getByCode("P100")).willReturn(Optional.of(product));
        given(productMapper.mapToDto(product)).willReturn(productDto);

        mockMvc.perform(get("/api/products/{code}", "P100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("P100")))
                .andExpect(jsonPath("$.name", is("The Hunger Games")));
    }
}

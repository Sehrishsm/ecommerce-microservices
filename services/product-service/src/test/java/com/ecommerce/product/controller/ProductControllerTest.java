package com.ecommerce.product.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.exception.GlobalExceptionHandler;
import com.ecommerce.product.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        productResponse = new ProductResponse(
                1L,
                "Wireless Mouse",
                "Wireless mouse",
                new BigDecimal("24.99"),
                1L,
                "Electronics");
    }

    @Test
    void pingShouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(get("/products/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from Product Service!"));
    }

    @Test
    void getAllProductsShouldReturnProductList() throws Exception {
        when(productService.getAllProductResponses())
                .thenReturn(List.of(productResponse));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Wireless Mouse"))
                .andExpect(jsonPath("$[0].price").value(24.99))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].categoryName").value("Electronics"));

        verify(productService).getAllProductResponses();
    }

    @Test
    void getProductByIdShouldReturnProduct() throws Exception {
        when(productService.getProductResponseById(1L))
                .thenReturn(productResponse);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"));

        verify(productService).getProductResponseById(1L);
    }

    @Test
    void searchProductsShouldPassNameToService() throws Exception {
        when(productService.searchProductResponses("Mouse"))
                .thenReturn(List.of(productResponse));

        mockMvc.perform(get("/products/search").param("name", "Mouse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Wireless Mouse"));

        verify(productService).searchProductResponses("Mouse");
    }

    @Test
    void getProductsByCategoryShouldPassCategoryIdToService() throws Exception {
        when(productService.getProductResponsesByCategory(1L))
                .thenReturn(List.of(productResponse));

        mockMvc.perform(get("/products/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(1));

        verify(productService).getProductResponsesByCategory(1L);
    }

    @Test
    void createProductShouldReturnCreatedProduct() throws Exception {
        when(productService.createProductResponse(
                "Wireless Mouse",
                "Wireless mouse",
                new BigDecimal("24.99"),
                1L))
                .thenReturn(productResponse);

        String requestJson = """
                {
                    "name": "Wireless Mouse",
                    "description": "Wireless mouse",
                    "price": 24.99,
                    "categoryId": 1
                }
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.categoryName").value("Electronics"));

        verify(productService).createProductResponse(
                "Wireless Mouse",
                "Wireless mouse",
                new BigDecimal("24.99"),
                1L);
    }

    @Test
    void createProductShouldRejectBlankName() throws Exception {
        String requestJson = """
                {
                    "name": "   ",
                    "description": "Wireless mouse",
                    "price": 24.99,
                    "categoryId": 1
                }
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Product name is required"));

        verifyNoInteractions(productService);
    }

    @Test
    void updateProductShouldReturnUpdatedProduct() throws Exception {
        when(productService.updateProductResponse(
                1L,
                "Wireless Mouse",
                "Updated description",
                new BigDecimal("29.99"),
                1L))
                .thenReturn(productResponse);

        String requestJson = """
                {
                    "name": "Wireless Mouse",
                    "description": "Updated description",
                    "price": 29.99,
                    "categoryId": 1
                }
                """;

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"));

        verify(productService).updateProductResponse(
                1L,
                "Wireless Mouse",
                "Updated description",
                new BigDecimal("29.99"),
                1L);
    }

    @Test
    void updateProductShouldRejectInvalidPrice() throws Exception {
        String requestJson = """
                {
                    "name": "Wireless Mouse",
                    "description": "Wireless mouse",
                    "price": 0,
                    "categoryId": 1
                }
                """;

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Price must be greater than zero"));

        verifyNoInteractions(productService);
    }

    @Test
    void deleteProductShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(productService).deleteProduct(1L);
    }

    @Test
    void getMissingProductShouldReturnNotFound() throws Exception {
        when(productService.getProductResponseById(999L))
                .thenThrow(new IllegalArgumentException("Product not found: 999"));

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found: 999"));

        verify(productService).getProductResponseById(999L);
        verify(productService, never()).deleteProduct(999L);
    }
}
package com.ecommerce.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ecommerce.product.entity.Category;
import com.ecommerce.product.exception.GlobalExceptionHandler;
import com.ecommerce.product.service.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

        @Mock
        private CategoryService categoryService;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .standaloneSetup(new CategoryController(categoryService))
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        private void setCategoryId(Category category, Long id) throws Exception {
                Field idField = Category.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(category, id);
        }

        @Test
        void getAllCategoriesShouldReturnCategories() throws Exception {
                Category category = new Category("Electronics", "Electronic products");
                setCategoryId(category, 1L);

                when(categoryService.getAllCategories()).thenReturn(List.of(category));

                mockMvc.perform(get("/categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].name").value("Electronics"))
                                .andExpect(jsonPath("$[0].description").value("Electronic products"));
        }

        @Test
        void getCategoryByIdShouldReturnCategory() throws Exception {
                Category category = new Category("Electronics", "Electronic products");
                setCategoryId(category, 1L);

                when(categoryService.getCategoryById(1L)).thenReturn(category);

                mockMvc.perform(get("/categories/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.name").value("Electronics"));
        }

        @Test
        void createCategoryShouldReturnCreatedCategory() throws Exception {
                Category category = new Category("Books", "Printed and digital books");
                setCategoryId(category, 2L);

                when(categoryService.createCategory("Books", "Printed and digital books"))
                                .thenReturn(category);

                mockMvc.perform(post("/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Books",
                                                  "description": "Printed and digital books"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(2))
                                .andExpect(jsonPath("$.name").value("Books"))
                                .andExpect(jsonPath("$.description").value("Printed and digital books"));
        }

        @Test
        void createCategoryWithBlankNameShouldReturnBadRequest() throws Exception {
                mockMvc.perform(post("/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": " ",
                                                  "description": "Invalid category"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Category name is required"));

                verify(categoryService, never()).createCategory(anyString(), any());
        }

        @Test
        void getMissingCategoryShouldReturnNotFound() throws Exception {
                when(categoryService.getCategoryById(999L))
                                .thenThrow(new IllegalArgumentException("Category not found: 999"));

                mockMvc.perform(get("/categories/999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("Category not found: 999"));
        }
}
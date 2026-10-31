package com.ecommerce.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.product.entity.Category;
import com.ecommerce.product.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

        @Mock
        private CategoryRepository categoryRepository;

        @InjectMocks
        private CategoryService categoryService;

        @Test
        void createCategoryShouldTrimNameBeforeSaving() {
                when(categoryRepository.findByNameIgnoreCase("Electronics"))
                                .thenReturn(Optional.empty());

                when(categoryRepository.save(any(Category.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Category result = categoryService.createCategory(
                                "  Electronics  ",
                                "Electronic devices");

                assertEquals("Electronics", result.getName());

                verify(categoryRepository)
                                .findByNameIgnoreCase("Electronics");
                verify(categoryRepository).save(any(Category.class));
        }

        @Test
        void createCategoryShouldRejectDuplicateNameIgnoringCase() {
                when(categoryRepository.findByNameIgnoreCase("electronics"))
                                .thenReturn(Optional.of(
                                                new Category("Electronics", "Existing category")));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> categoryService.createCategory(
                                                " electronics ",
                                                "Another description"));

                assertEquals(
                                "Category already exists: electronics",
                                exception.getMessage());

                verify(categoryRepository).findByNameIgnoreCase("electronics");
                verify(categoryRepository, org.mockito.Mockito.never())
                                .save(any(Category.class));
        }

        @Test
        void createCategoryShouldRejectBlankName() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> categoryService.createCategory("   ", "Description"));

                assertEquals("Category name is required", exception.getMessage());

                verifyNoInteractions(categoryRepository);
        }

        @Test
        void getCategoryByIdShouldRejectMissingCategory() {
                when(categoryRepository.findById(999999L))
                                .thenReturn(Optional.empty());

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> categoryService.getCategoryById(999999L));

                assertEquals("Category not found: 999999", exception.getMessage());

                verify(categoryRepository).findById(999999L);
        }

    @Test
    void getAllCategoriesShouldReturnRepositoryResults() {
        java.util.List<Category> categories = java.util.List.of(
                new Category("Electronics", "Electronic devices"),
                new Category("Books", "Printed books"));

        when(categoryRepository.findAll()).thenReturn(categories);

        java.util.List<Category> result = categoryService.getAllCategories();

        assertEquals(categories, result);
        verify(categoryRepository).findAll();
    }

    @Test
    void getCategoryByIdShouldReturnExistingCategory() {
        Category category = new Category(
                "Electronics",
                "Electronic devices");

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        Category result = categoryService.getCategoryById(1L);

        assertEquals(category, result);
        verify(categoryRepository).findById(1L);
    }
}
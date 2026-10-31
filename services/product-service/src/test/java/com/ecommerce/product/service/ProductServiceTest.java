package com.ecommerce.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.InventoryRepository;
import com.ecommerce.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

        @Mock
        private ProductRepository productRepository;

        @Mock
        private CategoryService categoryService;

        @Mock
        private InventoryRepository inventoryRepository;

        private ProductService productService;
        private Category category;

        @BeforeEach
        void setUp() {
                productService = new ProductService(
                                productRepository,
                                categoryService,
                                inventoryRepository);

                category = new Category("Electronics", "Electronic devices");
        }

        @Test
        void createProductShouldTrimNameAndSaveProduct() {
                when(categoryService.getCategoryById(1L)).thenReturn(category);
                when(productRepository.save(any(Product.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.createProduct(
                                "  Wireless Mouse  ",
                                "Wireless mouse",
                                new BigDecimal("24.99"),
                                1L);

                assertEquals("Wireless Mouse", result.getName());
                assertEquals("Wireless mouse", result.getDescription());
                assertEquals(new BigDecimal("24.99"), result.getPrice());
                assertEquals(category, result.getCategory());

                verify(productRepository).save(any(Product.class));
        }

        @Test
        void createProductShouldRejectBlankName() {
                assertThrows(IllegalArgumentException.class, () -> productService.createProduct(
                                "   ",
                                "Description",
                                new BigDecimal("10.00"),
                                1L));

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void createProductShouldRejectZeroOrNegativePrice() {
                assertThrows(IllegalArgumentException.class, () -> productService.createProduct(
                                "Mouse",
                                "Description",
                                BigDecimal.ZERO,
                                1L));

                assertThrows(IllegalArgumentException.class, () -> productService.createProduct(
                                "Mouse",
                                "Description",
                                new BigDecimal("-1.00"),
                                1L));

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void createProductShouldRejectMissingCategory() {
                when(categoryService.getCategoryById(999L))
                                .thenThrow(new IllegalArgumentException("Category not found: 999"));

                assertThrows(IllegalArgumentException.class, () -> productService.createProduct(
                                "Mouse",
                                "Description",
                                new BigDecimal("10.00"),
                                999L));

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void updateProductShouldUpdateProductFields() {
                Product existingProduct = new Product(
                                "Old Mouse",
                                "Old description",
                                new BigDecimal("15.00"),
                                category);

                Category newCategory = new Category("Accessories", "Computer accessories");

                when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
                when(categoryService.getCategoryById(2L)).thenReturn(newCategory);
                when(productRepository.save(any(Product.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.updateProduct(
                                1L,
                                "  New Mouse  ",
                                "Updated description",
                                new BigDecimal("29.99"),
                                2L);

                assertEquals("New Mouse", result.getName());
                assertEquals("Updated description", result.getDescription());
                assertEquals(new BigDecimal("29.99"), result.getPrice());
                assertEquals(newCategory, result.getCategory());

                verify(productRepository).save(existingProduct);
        }

        @Test
        void deleteProductShouldRejectProductWithInventory() {
                Product existingProduct = new Product(
                                "Wireless Mouse",
                                "Description",
                                new BigDecimal("24.99"),
                                category);

                when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
                when(inventoryRepository.existsByProductId(1L)).thenReturn(true);

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> productService.deleteProduct(1L));

                assertEquals(
                                "Cannot delete product because inventory exists for product: 1",
                                exception.getMessage());

                verify(productRepository, never()).delete(existingProduct);
        }

        @Test
        void getProductByIdShouldRejectMissingProduct() {
                when(productRepository.findById(999L)).thenReturn(Optional.empty());

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> productService.getProductById(999L));

                assertEquals("Product not found: 999", exception.getMessage());
        }

        @Test
        void searchProductsShouldReturnAllProductsForBlankSearch() {
                List<Product> products = List.of(
                                new Product("Mouse", "Description", new BigDecimal("20.00"), category),
                                new Product("Keyboard", "Description", new BigDecimal("30.00"), category));

                when(productRepository.findAll()).thenReturn(products);

                List<Product> result = productService.searchProducts("  ");

                assertEquals(2, result.size());
                assertEquals(products, result);
        }

        @Test
        void searchProductsShouldTrimSearchText() {
                List<Product> products = List.of(
                                new Product("Wireless Mouse", "Description",
                                                new BigDecimal("24.99"), category));

                when(productRepository.findByNameContainingIgnoreCase("Mouse"))
                                .thenReturn(products);

                List<Product> result = productService.searchProducts("  Mouse  ");

                assertEquals(1, result.size());
                assertEquals("Wireless Mouse", result.get(0).getName());
                verify(productRepository).findByNameContainingIgnoreCase("Mouse");
        }
}
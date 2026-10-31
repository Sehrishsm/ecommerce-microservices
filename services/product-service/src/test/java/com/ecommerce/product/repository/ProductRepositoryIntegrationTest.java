package com.ecommerce.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Inventory;
import com.ecommerce.product.entity.Product;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
                "spring.datasource.url=jdbc:h2:mem:producttest;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "eureka.client.enabled=false"
})
class ProductRepositoryIntegrationTest {

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private ProductRepository productRepository;
        @Autowired
        private TestEntityManager entityManager;
        @Autowired
        private InventoryRepository inventoryRepository;

        @Test
        void shouldSaveAndRetrieveProductWithItsCategory() {
                // Arrange: save a category first because a product requires one.
                Category category = new Category(
                                "Integration Test Electronics",
                                "Category created by a database integration test");

                Category savedCategory = categoryRepository.saveAndFlush(category);

                // Act: save a product linked to that persisted category.
                Product product = new Product(
                                "Integration Test Headphones",
                                "Database persistence test product",
                                new BigDecimal("49.99"),
                                savedCategory);

                Product savedProduct = productRepository.saveAndFlush(product);

                // Clear the persistence context so the next read comes from the database.
                entityManager.flush();
                entityManager.clear();

                Product retrievedProduct = productRepository.findById(savedProduct.getId())
                                .orElseThrow(() -> new AssertionError("Saved product was not found"));

                // Assert: verify the product and its category relationship.
                assertThat(savedProduct.getId()).isNotNull();
                assertThat(retrievedProduct.getName())
                                .isEqualTo("Integration Test Headphones");
                assertThat(retrievedProduct.getPrice())
                                .isEqualByComparingTo("49.99");
                assertThat(retrievedProduct.getCategory().getId())
                                .isEqualTo(savedCategory.getId());
                assertThat(categoryRepository.findById(savedCategory.getId()))
                                .isPresent();
        }

        @Test
        void shouldSaveAndUpdateInventoryWithVersioning() {
                // Arrange: create an inventory record for product ID 1001.
                Inventory inventory = new Inventory(1001L, 20);

                Inventory savedInventory = inventoryRepository.saveAndFlush(inventory);

                assertThat(savedInventory.getId()).isNotNull();
                assertThat(savedInventory.getProductId()).isEqualTo(1001L);
                assertThat(savedInventory.getQuantity()).isEqualTo(20);
                assertThat(savedInventory.getVersion()).isNotNull();

                Long initialVersion = savedInventory.getVersion();

                // Act: change the stock quantity and force Hibernate to update the row.
                savedInventory.setQuantity(15);
                inventoryRepository.flush();

                // Clear the persistence context and retrieve the record again.
                entityManager.clear();

                Inventory retrievedInventory = inventoryRepository.findByProductId(1001L)
                                .orElseThrow(() -> new AssertionError("Inventory record was not found"));

                // Assert: verify the stored quantity and version progression.
                assertThat(retrievedInventory.getQuantity()).isEqualTo(15);
                assertThat(retrievedInventory.getVersion()).isGreaterThan(initialVersion);
        }
}
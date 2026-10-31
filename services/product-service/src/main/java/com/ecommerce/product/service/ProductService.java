
package com.ecommerce.product.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.InventoryRepository;
import com.ecommerce.product.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final InventoryRepository inventoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryService categoryService,
            InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.inventoryRepository = inventoryRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    public List<Product> searchProducts(String name) {
        if (name == null || name.isBlank()) {
            return getAllProducts();
        }

        return productRepository.findByNameContainingIgnoreCase(name.trim());
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        categoryService.getCategoryById(categoryId);
        return productRepository.findByCategory_Id(categoryId);
    }

    @Transactional
    public Product createProduct(
            String name,
            String description,
            BigDecimal price,
            Long categoryId) {

        validateProduct(name, price);

        Category category = categoryService.getCategoryById(categoryId);

        Product product = new Product(
                name.trim(),
                description,
                price,
                category);

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(
            Long id,
            String name,
            String description,
            BigDecimal price,
            Long categoryId) {

        validateProduct(name, price);

        Product product = getProductById(id);
        Category category = categoryService.getCategoryById(categoryId);

        product.setName(name.trim());
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);

        if (inventoryRepository.existsByProductId(id)) {
            throw new IllegalArgumentException(
                    "Cannot delete product because inventory exists for product: " + id);
        }

        productRepository.delete(product);
    }

    private void validateProduct(String name, BigDecimal price) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
    }

    // The DTO Mapping Layer (The "Facade")
    public List<ProductResponse> getAllProductResponses() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getProductResponseById(Long id) {
        return ProductResponse.from(getProductById(id));
    }

    public List<ProductResponse> searchProductResponses(String name) {
        return searchProducts(name)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> getProductResponsesByCategory(Long categoryId) {
        return getProductsByCategory(categoryId)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse createProductResponse(
            String name,
            String description,
            BigDecimal price,
            Long categoryId) {

        Product product = createProduct(name, description, price, categoryId);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProductResponse(
            Long id,
            String name,
            String description,
            BigDecimal price,
            Long categoryId) {

        Product product = updateProduct(id, name, description, price, categoryId);
        return ProductResponse.from(product);
    }

}

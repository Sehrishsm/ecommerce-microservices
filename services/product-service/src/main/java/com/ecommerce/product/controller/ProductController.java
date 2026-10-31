
package com.ecommerce.product.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "Hello from Product Service!";
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProductResponses();
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable("id") Long id) {
        return productService.getProductResponseById(id);
    }

    @GetMapping("/search")
    public List<ProductResponse> searchProducts(
            @RequestParam(name = "name", required = false) String name) {

        return productService.searchProductResponses(name);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductResponse> getProductsByCategory(
            @PathVariable("categoryId") Long categoryId) {

        return productService.getProductResponsesByCategory(categoryId);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.createProductResponse(
                request.name(),
                request.description(),
                request.price(),
                request.categoryId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductRequest request) {

        return productService.updateProductResponse(
                id,
                request.name(),
                request.description(),
                request.price(),
                request.categoryId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

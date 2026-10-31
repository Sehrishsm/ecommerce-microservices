
package com.ecommerce.product.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.product.dto.CategoryRequest;
import com.ecommerce.product.dto.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable("id") Long id) {
        Category category = categoryService.getCategoryById(id);
        return CategoryResponse.from(category);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {

        Category category = categoryService.createCategory(
                request.name(),
                request.description());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CategoryResponse.from(category));
    }
}

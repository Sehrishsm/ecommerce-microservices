
package com.ecommerce.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategory_Id(Long categoryId);
}

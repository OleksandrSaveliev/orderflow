package com.my.orderflow.repository;

import com.my.orderflow.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByTitle(String title);

    boolean existsByTitle(String title);
}
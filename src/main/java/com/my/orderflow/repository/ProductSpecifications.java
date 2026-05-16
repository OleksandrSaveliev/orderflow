package com.my.orderflow.repository;

import com.my.orderflow.model.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductSpecifications {

    public static Specification<Product> hasTitleContaining(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("title")),
                "%" + search.toLowerCase() + "%"
        );
    }

    public static Specification<Product> hasMinPrice(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> hasMaxPrice(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> hasCategoryId(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<Product> hasMinQuantity(Integer minQuantity) {
        if (minQuantity == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("quantity"), minQuantity);
    }
}
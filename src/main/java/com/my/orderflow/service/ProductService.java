package com.my.orderflow.service;

import com.my.orderflow.dto.product.ProductRequestDto;
import com.my.orderflow.dto.product.ProductResponseDto;
import com.my.orderflow.exception.ProductNotFoundException;
import com.my.orderflow.mapper.ProductMapper;
import com.my.orderflow.model.Product;
import com.my.orderflow.repository.ProductSpecifications;
import com.my.orderflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> search(
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            UUID categoryId,
            Integer minQuantity,
            Pageable pageable) {

        Specification<Product> spec = Specification.where(
                ProductSpecifications.hasTitleContaining(search))
                .and(ProductSpecifications.hasMinPrice(minPrice))
                .and(ProductSpecifications.hasMaxPrice(maxPrice))
                .and(ProductSpecifications.hasCategoryId(categoryId))
                .and(ProductSpecifications.hasMinQuantity(minQuantity));

        return productRepository.findAll(spec, pageable)
                .map(productMapper::toResponse);
    }

    @Transactional
    public ProductResponseDto create(ProductRequestDto request) {
        if (productRepository.existsByTitle(request.title())) {
            throw new IllegalArgumentException("Product with title already exists: " + request.title());
        }

        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);

        log.info("Created product with id: {}", saved.getId());

        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponseDto update(UUID id, ProductRequestDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getTitle().equals(request.title())
                && productRepository.existsByTitle(request.title())) {
            throw new IllegalArgumentException("Product with title already exists: " + request.title());
        }

        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        product.setCategoryId(request.categoryId());

        Product updated = productRepository.save(product);

        log.info("Updated product with id: {}", id);

        return productMapper.toResponse(updated);
    }

    @Transactional
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        productRepository.deleteById(id);

        log.info("Deleted product with id: {}", id);
    }
}
package com.my.orderflow.service;

import com.my.orderflow.dto.product.ProductRequestDto;
import com.my.orderflow.dto.product.ProductResponseDto;
import com.my.orderflow.exception.ProductAlreadyExistsException;
import com.my.orderflow.exception.ProductNotFoundException;
import com.my.orderflow.mapper.ProductMapper;
import com.my.orderflow.model.Product;
import com.my.orderflow.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("getById - Should return product when exists")
    void getById_Success() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder().id(id).title("Test Product").build();
        ProductResponseDto responseDto = new ProductResponseDto(id,
                "Test Product",
                "Desc",
                BigDecimal.TEN,
                10,
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now());

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(responseDto);

        ProductResponseDto result = productService.getById(id);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Test Product");
        verify(productRepository).findById(id);
    }

    @Test
    @DisplayName("getById - Should throw ProductNotFoundException when not exists")
    void getById_NotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("create - Should create product when title is unique")
    void create_Success() {
        ProductRequestDto request = new ProductRequestDto("New Product",
                "Desc",
                BigDecimal.TEN,
                10,
                UUID.randomUUID());
        Product product = Product.builder().title("New Product").build();
        Product savedProduct = Product.builder().id(UUID.randomUUID()).title("New Product").build();
        ProductResponseDto responseDto = new ProductResponseDto(savedProduct.getId(),
                "New Product",
                "Desc",
                BigDecimal.TEN,
                10,
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now());

        when(productRepository.existsByTitle(request.title())).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(savedProduct);
        when(productMapper.toResponse(savedProduct)).thenReturn(responseDto);

        ProductResponseDto result = productService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("New Product");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("create - Should throw ProductAlreadyExistsException when title exists")
    void create_AlreadyExists() {
        ProductRequestDto request = new ProductRequestDto("Existing Product",
                "Desc",
                BigDecimal.TEN,
                10,
                UUID.randomUUID());
        when(productRepository.existsByTitle(request.title())).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ProductAlreadyExistsException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - Should update product when exists and title is unique")
    void update_Success() {
        UUID id = UUID.randomUUID();
        ProductRequestDto request = new ProductRequestDto("Updated Title",
                "Updated Desc",
                BigDecimal.ONE,
                5,
                UUID.randomUUID());
        Product product = Product.builder().id(id).title("Old Title").build();
        ProductResponseDto responseDto = new ProductResponseDto(id,
                "Updated Title",
                "Updated Desc",
                BigDecimal.ONE,
                5,
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now());

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.existsByTitle(request.title())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(responseDto);

        ProductResponseDto result = productService.update(id, request);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Updated Title");
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("update - Should throw ProductNotFoundException when not exists")
    void update_NotFound() {
        UUID id = UUID.randomUUID();
        ProductRequestDto request = new ProductRequestDto("Title",
                "Desc",
                BigDecimal.TEN,
                10,
                UUID.randomUUID());
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(id, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("delete - Should delete when exists")
    void delete_Success() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);

        productService.delete(id);

        verify(productRepository).deleteById(id);
    }

    @Test
    @DisplayName("delete - Should throw ProductNotFoundException when not exists")
    void delete_NotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(id))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).deleteById(any());
    }
}

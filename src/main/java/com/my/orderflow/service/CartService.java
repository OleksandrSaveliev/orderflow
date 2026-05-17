package com.my.orderflow.service;

import com.my.orderflow.dto.cart.CartItemRequestDto;
import com.my.orderflow.dto.cart.CartResponseDto;
import com.my.orderflow.exception.CartNotFoundException;
import com.my.orderflow.exception.InsufficientStockException;
import com.my.orderflow.mapper.CartMapper;
import com.my.orderflow.model.Cart;
import com.my.orderflow.model.CartItem;
import com.my.orderflow.model.Product;
import com.my.orderflow.repository.CartItemRepository;
import com.my.orderflow.repository.CartRepository;
import com.my.orderflow.repository.ProductRepository;
import com.my.orderflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    @Transactional(readOnly = true)
    public CartResponseDto getCart(UUID userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .map(cartMapper::toResponse)
                .orElseGet(() -> new CartResponseDto(null, List.of(), BigDecimal.ZERO));
    }

    @Transactional
    public CartResponseDto addItem(UUID userId, CartItemRequestDto request) {
        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.productId()));
        checkStock(product, request.quantity());

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
            cartItemRepository.save(existingItem);
            log.info("Updated cart item quantity for product: {}", product.getTitle());
        } else {
            CartItem newItem = CartItem.builder()
                    .product(product)
                    .quantity(request.quantity())
                    .priceAtAdd(product.getPrice())
                    .build();
            cart.getItems().add(newItem);
            cartRepository.save(cart);
            log.info("Added new item to cart: {}", product.getTitle());
        }

        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponseDto updateItemQuantity(UUID userId, UUID itemId, int quantity) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartNotFoundException("Item not found: " + itemId));

        checkStock(item.getProduct(), quantity);

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        log.info("Updated cart item {} quantity to {}", itemId, quantity);

        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponseDto removeItem(UUID userId, UUID itemId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartNotFoundException("Item not found: " + itemId));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        log.info("Removed cart item: {}", itemId);

        return cartMapper.toResponse(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();

        log.info("Cleared cart for user: {}", userId);
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(userRepository.getReferenceById(userId))
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private void checkStock(Product product, int requestedQuantity) {
        if (product.getQuantity() < requestedQuantity) {
            throw new InsufficientStockException(
                    product.getTitle(),
                    requestedQuantity,
                    product.getQuantity()
            );
        }
    }
}
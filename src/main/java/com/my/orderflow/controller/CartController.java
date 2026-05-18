package com.my.orderflow.controller;

import com.my.orderflow.dto.cart.CartItemRequestDto;
import com.my.orderflow.dto.cart.CartResponseDto;
import com.my.orderflow.model.User;
import com.my.orderflow.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponseDto> getCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cartService.getCart(user.getId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDto> addItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CartItemRequestDto request) {
        return ResponseEntity.ok(cartService.addItem(user.getId(), request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> updateItemQuantity(
            @AuthenticationPrincipal User user,
            @PathVariable UUID itemId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateItemQuantity(user.getId(), itemId, quantity));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(cartService.removeItem(user.getId(), itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user.getId());
        return ResponseEntity.noContent().build();
    }
}
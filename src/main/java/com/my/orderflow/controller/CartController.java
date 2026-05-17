package com.my.orderflow.controller;

import com.my.orderflow.dto.cart.CartItemRequestDto;
import com.my.orderflow.dto.cart.CartResponseDto;
import com.my.orderflow.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponseDto> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        CartResponseDto response = cartService.getCart(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDto> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemRequestDto request) {
        UUID userId = getUserId(userDetails);
        CartResponseDto response = cartService.addItem(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> updateItemQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID itemId,
            @RequestParam int quantity) {
        UUID userId = getUserId(userDetails);
        CartResponseDto response = cartService.updateItemQuantity(userId, itemId, quantity);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID itemId) {
        UUID userId = getUserId(userDetails);
        CartResponseDto response = cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getUserId(UserDetails userDetails) {
        return cartService.getUserIdByEmail(userDetails.getUsername());
    }
}
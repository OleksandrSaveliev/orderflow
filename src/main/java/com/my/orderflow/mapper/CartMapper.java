package com.my.orderflow.mapper;

import com.my.orderflow.dto.cart.CartItemResponseDto;
import com.my.orderflow.dto.cart.CartResponseDto;
import com.my.orderflow.model.Cart;
import com.my.orderflow.model.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CartMapper {

    public CartResponseDto toResponse(Cart cart) {
        List<CartItemResponseDto> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = items.stream()
                .map(item -> item.priceAtAdd().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponseDto(cart.getId(), items, total);
    }

    public CartItemResponseDto toItemResponse(CartItem item) {
        return new CartItemResponseDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getTitle(),
                item.getQuantity(),
                item.getPriceAtAdd()
        );
    }
}
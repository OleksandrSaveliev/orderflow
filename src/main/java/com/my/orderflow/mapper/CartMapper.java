package com.my.orderflow.mapper;

import com.my.orderflow.dto.cart.CartItemResponseDto;
import com.my.orderflow.dto.cart.CartResponseDto;
import com.my.orderflow.model.Cart;
import com.my.orderflow.model.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CartMapper {

    default CartResponseDto toResponse(Cart cart) {
        List<CartItemResponseDto> items = toItemResponseList(cart.getItems());

        BigDecimal total = items.stream()
                .map(item -> item.priceAtAdd().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponseDto(cart.getId(), items, total);
    }

    List<CartItemResponseDto> toItemResponseList(Set<CartItem> items);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productTitle", source = "product.title")
    CartItemResponseDto toItemResponse(CartItem item);
}

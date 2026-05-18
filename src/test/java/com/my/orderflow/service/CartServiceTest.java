package com.my.orderflow.service;

import com.my.orderflow.dto.cart.CartItemRequestDto;
import com.my.orderflow.dto.cart.CartResponseDto;
import com.my.orderflow.exception.CartNotFoundException;
import com.my.orderflow.exception.InsufficientStockException;
import com.my.orderflow.exception.ProductNotFoundException;
import com.my.orderflow.mapper.CartMapper;
import com.my.orderflow.model.Cart;
import com.my.orderflow.model.CartItem;
import com.my.orderflow.model.Product;
import com.my.orderflow.model.User;
import com.my.orderflow.repository.CartItemRepository;
import com.my.orderflow.repository.CartRepository;
import com.my.orderflow.repository.ProductRepository;
import com.my.orderflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    private UUID userId;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        product = Product.builder()
                .id(UUID.randomUUID())
                .title("Test Product")
                .price(BigDecimal.TEN)
                .quantity(100)
                .build();
        cart = Cart.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(userId).build())
                .items(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("getCart - Should return cart when it exists")
    void getCart_Exists() {
        CartResponseDto responseDto = new CartResponseDto(cart.getId(), List.of(), BigDecimal.ZERO);
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(responseDto);

        CartResponseDto result = cartService.getCart(userId);

        assertThat(result).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("getCart - Should return empty cart when it does not exist")
    void getCart_NotExists() {
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.empty());

        CartResponseDto result = cartService.getCart(userId);

        assertThat(result).isNotNull();
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("addItem - Should add new item to cart")
    void addItem_NewItem() {
        CartItemRequestDto request = new CartItemRequestDto(product.getId(), 2);
        CartResponseDto responseDto = new CartResponseDto(cart.getId(), List.of(), BigDecimal.ZERO);
        
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(cart)).thenReturn(responseDto);

        CartResponseDto result = cartService.addItem(userId, request);

        assertThat(result).isEqualTo(responseDto);
        assertThat(cart.getItems()).hasSize(1);
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("addItem - Should update quantity if item already exists")
    void addItem_ExistingItem() {
        CartItem existingItem = CartItem.builder()
                .id(UUID.randomUUID())
                .product(product)
                .quantity(1)
                .build();
        cart.getItems().add(existingItem);
        
        CartItemRequestDto request = new CartItemRequestDto(product.getId(), 2);
        CartResponseDto responseDto = new CartResponseDto(cart.getId(), List.of(), BigDecimal.ZERO);

        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(cart)).thenReturn(responseDto);

        CartResponseDto result = cartService.addItem(userId, request);

        assertThat(result).isEqualTo(responseDto);
        assertThat(existingItem.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    @DisplayName("addItem - Should throw ProductNotFoundException when product not found")
    void addItem_ProductNotFound() {
        CartItemRequestDto request = new CartItemRequestDto(UUID.randomUUID(), 2);
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(userId, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("addItem - Should throw InsufficientStockException when stock is low")
    void addItem_InsufficientStock() {
        CartItemRequestDto request = new CartItemRequestDto(product.getId(), 200);
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(userId, request))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("updateItemQuantity - Should update quantity successfully")
    void updateItemQuantity_Success() {
        UUID itemId = UUID.randomUUID();
        CartItem item = CartItem.builder()
                .id(itemId)
                .product(product)
                .quantity(1)
                .build();
        cart.getItems().add(item);
        
        CartResponseDto responseDto = new CartResponseDto(cart.getId(), List.of(), BigDecimal.ZERO);
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(responseDto);

        CartResponseDto result = cartService.updateItemQuantity(userId, itemId, 5);

        assertThat(result).isEqualTo(responseDto);
        assertThat(item.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(item);
    }

    @Test
    @DisplayName("updateItemQuantity - Should throw CartNotFoundException when cart not found")
    void updateItemQuantity_CartNotFound() {
        UUID itemId = UUID.randomUUID();
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(userId, itemId, 5))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    @DisplayName("removeItem - Should remove item successfully")
    void removeItem_Success() {
        UUID itemId = UUID.randomUUID();
        CartItem item = CartItem.builder()
                .id(itemId)
                .product(product)
                .quantity(1)
                .build();
        cart.getItems().add(item);
        
        CartResponseDto responseDto = new CartResponseDto(cart.getId(), List.of(), BigDecimal.ZERO);
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(responseDto);

        CartResponseDto result = cartService.removeItem(userId, itemId);

        assertThat(result).isEqualTo(responseDto);
        assertThat(cart.getItems()).isEmpty();
        verify(cartItemRepository).delete(item);
    }

    @Test
    @DisplayName("clearCart - Should clear all items")
    void clearCart_Success() {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID())
                .product(product)
                .quantity(1)
                .build();
        cart.getItems().add(item);
        
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));

        cartService.clearCart(userId);

        assertThat(cart.getItems()).isEmpty();
        verify(cartItemRepository).deleteAll(any());
    }
}

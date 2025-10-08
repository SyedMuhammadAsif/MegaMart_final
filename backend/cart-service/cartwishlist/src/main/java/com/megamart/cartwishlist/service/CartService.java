package com.megamart.cartwishlist.service;

import com.megamart.cartwishlist.dto.*;
import com.megamart.cartwishlist.model.Cart;
import com.megamart.cartwishlist.model.CartItem;
import com.megamart.cartwishlist.repository.CartRepository;
import com.megamart.cartwishlist.client.ProductServiceClient;
import com.megamart.cartwishlist.exception.ItemNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;
	private final ProductServiceClient productServiceClient;

	protected Cart getOrCreateCart(Long userId) {
		return cartRepository.findByUserId(userId).orElseGet(() -> {
			Cart cart = Cart.builder().userId(userId).build();
			return cartRepository.save(cart);
		});
	}

	private void recalcCartTotal(Cart cart) {
		BigDecimal total = cart.getItems().stream()
			.map(CartItem::getLineTotal)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		cart.setTotal(total);
	}

	@Transactional
	public CartResponseDto getCart(Long userId) {
		Cart cart = getOrCreateCart(userId);
		recalcCartTotal(cart);
		cartRepository.save(cart);
		return toResponse(cart);
	}

	@Transactional
	public CartResponseDto addItem(Long userId, AddItemRequestDto request) {
		Cart cart = getOrCreateCart(userId);
		CartItem existing = cart.getItems().stream()
			.filter(i -> i.getProductId().equals(request.getProductId()))
			.findFirst().orElse(null);

		// Get product details and price
		BigDecimal unitPrice = BigDecimal.valueOf(10.0); // Default price
		try {
			ProductDto product = productServiceClient.getProduct(request.getProductId()).orElse(null);
			if (product != null) {
				unitPrice = product.getPrice();
			}
		} catch (Exception e) {
			log.warn("Could not get product details for productId {}: {}", request.getProductId(), e.getMessage());
		}

		if (existing != null) {
			existing.setQuantity(existing.getQuantity() + request.getQuantity());
			existing.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(existing.getQuantity())));
		} else {
			CartItem item = CartItem.builder()
				.cart(cart)
				.productId(request.getProductId())
				.quantity(request.getQuantity())
				.lineTotal(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())))
				.build();
			cart.getItems().add(item);
		}
		
		recalcCartTotal(cart);
		cartRepository.save(cart);
		return toResponse(cart);
	}

	@Transactional
	public CartResponseDto updateQuantity(Long userId, Long itemId, int quantity) {
		Cart cart = getOrCreateCart(userId);
		CartItem item = cart.getItems().stream()
			.filter(i -> i.getId().equals(itemId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Item not found in cart"));

		if (quantity < 1) throw new IllegalArgumentException("Quantity must be >= 1");
		
		// Get current product price
		BigDecimal unitPrice = BigDecimal.valueOf(10.0); // Default price
		try {
			ProductDto product = productServiceClient.getProduct(item.getProductId()).orElse(null);
			if (product != null) {
				unitPrice = product.getPrice();
			}
		} catch (Exception e) {
			log.warn("Could not get product details for productId {}: {}", item.getProductId(), e.getMessage());
		}
		
		item.setQuantity(quantity);
		item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
		
		recalcCartTotal(cart);
		cartRepository.save(cart);
		return toResponse(cart);
	}

	@Transactional
	public CartResponseDto removeItem(Long userId, Long itemId) {
		Cart cart = getOrCreateCart(userId);
		cart.getItems().removeIf(i -> i.getId().equals(itemId));
		recalcCartTotal(cart);
		cartRepository.save(cart);
		return toResponse(cart);
	}

	@Transactional
	public void clearCart(Long userId) {
		Cart cart = getOrCreateCart(userId);
		cart.getItems().clear();
		recalcCartTotal(cart);
		cartRepository.save(cart);
	}

	private CartResponseDto toResponse(Cart cart) {
		List<CartItemResponseDto> items = cart.getItems().stream()
			.map(ci -> CartItemResponseDto.builder()
				.id(ci.getId())
				.productId(ci.getProductId())
				.quantity(ci.getQuantity())
				.lineTotal(ci.getLineTotal())
				.build())
			.toList();

		int totalItems = items.stream().mapToInt(CartItemResponseDto::getQuantity).sum();
		BigDecimal totalPrice = items.stream()
			.map(CartItemResponseDto::getLineTotal)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		return CartResponseDto.builder()
			.id(cart.getId())
			.userId(cart.getUserId())
			.total(cart.getTotal())
			.items(items)
			.totalItems(totalItems)
			.totalPrice(totalPrice)
			.build();
	}


} 
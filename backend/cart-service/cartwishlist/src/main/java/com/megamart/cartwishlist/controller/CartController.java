package com.megamart.cartwishlist.controller;

import com.megamart.cartwishlist.dto.*;
import com.megamart.cartwishlist.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Validated
public class CartController {

	private final CartService cartService;

	@GetMapping("/{userId}")
	public ResponseEntity<CartResponseDto> getCart(@PathVariable("userId") Long userId) {
		return ResponseEntity.ok(cartService.getCart(userId));
	}

	@PostMapping("/{userId}/items")
	public ResponseEntity<CartResponseDto> addItem(@PathVariable("userId") Long userId, @Valid @RequestBody AddItemRequestDto request) {
		return ResponseEntity.ok(cartService.addItem(userId, request));
	}

	@PatchMapping("/{userId}/items/{itemId}")
	public ResponseEntity<CartResponseDto> updateQuantity(
		@PathVariable("userId") Long userId,
		@PathVariable("itemId") @Min(1) Long itemId,
		@Valid @RequestBody UpdateQuantityRequestDto request
	) {
		return ResponseEntity.ok(cartService.updateQuantity(userId, itemId, request.getQuantity()));
	}

	@DeleteMapping("/{userId}/items/{itemId}")
	public ResponseEntity<CartResponseDto> removeItem(@PathVariable("userId") Long userId, @PathVariable("itemId") @Min(1) Long itemId) {
		return ResponseEntity.ok(cartService.removeItem(userId, itemId));
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> clear(@PathVariable("userId") Long userId) {
		cartService.clearCart(userId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/health")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("Cart Service is running on port 8087");
	}
} 
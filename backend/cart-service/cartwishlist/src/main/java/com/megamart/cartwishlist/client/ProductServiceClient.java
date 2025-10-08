package com.megamart.cartwishlist.client;

import com.megamart.cartwishlist.dto.ProductDto;
import com.megamart.cartwishlist.dto.ProductDataDto;
import com.megamart.cartwishlist.dto.ProductServiceResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;


@FeignClient(name = "productservice")
public interface ProductServiceClient {

	@CircuitBreaker(name = "product-service", fallbackMethod = "serviceFallback")
	@GetMapping("api/products/{productId}")
	ProductServiceResponseDto getProductById(@PathVariable Long productId);

	default ProductServiceResponseDto serviceFallback(Long productId, Exception ex) {

		return null;
	}

	@PostMapping("api/products/{productId}/stock")
	Map<String, Object> updateStock(@PathVariable Long productId, @RequestBody Map<String, Integer> request);

	default Optional<ProductDto> getProduct(Long productId) {
		try {
			ProductServiceResponseDto response = getProductById(productId);
			if (response != null && response.getData() != null) {
				ProductDataDto data = response.getData();
				return Optional.of(ProductDto.builder()
					.productId(data.getId())
					.name(data.getTitle())
					.price(data.getPrice())
					.stock(data.getStock())
					.build());
			}
			return Optional.empty();
		} catch (Exception e) {

			return Optional.empty();
		}
	}
} 
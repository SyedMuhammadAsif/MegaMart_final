package com.megamart.productservice.controller;

import com.megamart.productservice.dto.*;
import com.megamart.productservice.entity.Category;
import com.megamart.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class ProductController {
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @GetMapping("/products")
    public ResponseEntity<ProductResponseDto> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        Page<ProductDTO> products = productService.getAllProducts(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ProductResponseDto.success(products, "Products retrieved successfully"));
    }
    
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
            .map(product -> ResponseEntity.ok(ProductResponseDto.success(product, "Product retrieved successfully")))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ProductResponseDto.error("Product not found with id: " + id)));
    }
    
    @PostMapping("/products")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody ProductDTO productDTO) {
        ProductDTO createdProduct = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponseDto.success(createdProduct, "Product created successfully"));
    }
    
    @PutMapping("/products/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(ProductResponseDto.success(updatedProduct, "Product updated successfully"));
    }
    
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponseDto> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ProductResponseDto.success(null, "Product deleted successfully"));
    }
    
    @GetMapping("/products/category/{category}")
    public ResponseEntity<ProductResponseDto> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<ProductDTO> products = productService.getProductsByCategory(category, page, size);
        return ResponseEntity.ok(ProductResponseDto.success(products, "Products by category retrieved successfully"));
    }
    
    @GetMapping("/products/search")
    public ResponseEntity<ProductResponseDto> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        ProductSearchRequest request = new ProductSearchRequest();
        request.setQuery(keyword);
        request.setCategory(category);
        request.setBrand(brand);
        request.setMinPrice(minPrice != null ? BigDecimal.valueOf(minPrice) : null);
        request.setMaxPrice(maxPrice != null ? BigDecimal.valueOf(maxPrice) : null);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);
        
        Page<ProductDTO> products = productService.searchProducts(request);
        return ResponseEntity.ok(ProductResponseDto.success(products, "Products search completed successfully"));
    }
    
    @GetMapping("/products/in-stock")
    public ResponseEntity<ProductResponseDto> getProductsInStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<ProductDTO> products = productService.getProductsInStock(page, size);
        return ResponseEntity.ok(ProductResponseDto.success(products, "In-stock products retrieved successfully"));
    }
    

    @GetMapping("/categories")
    public ResponseEntity<ProductResponseDto> getAllCategories() {
        List<Category> categories = productService.getAllCategoryEntities();
        return ResponseEntity.ok(ProductResponseDto.success(categories, "Categories retrieved successfully"));
    }
    
    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponseDto> createCategory(@RequestBody CategoryDto categoryDto) {
        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        Category createdCategory = productService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponseDto.success(createdCategory, "Category created successfully"));
    }
    
    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponseDto> updateCategory(@PathVariable Long id, @RequestBody CategoryDto categoryDto) {
        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        Category updatedCategory = productService.updateCategory(id, category);
        return ResponseEntity.ok(ProductResponseDto.success(updatedCategory, "Category updated successfully"));
    }
    
    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponseDto> deleteCategory(@PathVariable Long id) {
        productService.deleteCategory(id);
        return ResponseEntity.ok(ProductResponseDto.success(null, "Category deleted successfully"));
    }
    
    @GetMapping("/brands")
    public ResponseEntity<ProductResponseDto> getAllBrands() {
        List<String> brands = productService.getAllBrands();
        return ResponseEntity.ok(ProductResponseDto.success(brands, "Brands retrieved successfully"));
    }
    
    @GetMapping("/brands/category/{category}")

    public ResponseEntity<ProductResponseDto> getBrandsByCategory(@PathVariable String category) {
        List<String> brands = productService.getBrandsByCategory(category);
        return ResponseEntity.ok(ProductResponseDto.success(brands, "Brands for category retrieved successfully"));
    }
    
    @PostMapping("/products/{id}/stock")
    public ResponseEntity<ProductResponseDto> updateStock(@PathVariable Long id, @RequestBody StockUpdateRequestDto request) {
        System.out.println("ProductController.updateStock called for product " + id);
        System.out.println("Request body: " + request);
        
        if (request.getStockChange() == null) {
            return ResponseEntity.badRequest().body(ProductResponseDto.error("stockChange is required"));
        }
        
        ProductDTO updatedProduct = productService.updateStock(id, request.getStockChange());
        return ResponseEntity.ok(ProductResponseDto.success(updatedProduct, "Stock updated successfully"));
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Product Service is running on port 9096");
    }
    

}
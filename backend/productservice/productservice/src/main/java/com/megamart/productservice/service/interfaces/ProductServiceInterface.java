package com.megamart.productservice.service.interfaces;

import com.megamart.productservice.dto.ProductDTO;
import com.megamart.productservice.dto.ProductSearchRequest;
import com.megamart.productservice.entity.Category;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface ProductServiceInterface {
    Page<ProductDTO> getAllProducts(int page, int size, String sortBy, String sortDirection);
    Optional<ProductDTO> getProductById(Long id);
    ProductDTO createProduct(ProductDTO productDTO);
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
    void deleteProduct(Long id);
    Page<ProductDTO> getProductsByCategory(String category, int page, int size);
    Page<ProductDTO> searchProducts(ProductSearchRequest request);
    List<String> getAllCategories();
    List<String> getAllBrands();
    Page<ProductDTO> getProductsInStock(int page, int size);
    
    // Category methods
    List<Category> getAllCategoryEntities();
    Category createCategory(Category category);
    Category updateCategory(Long id, Category category);
    void deleteCategory(Long id);
}
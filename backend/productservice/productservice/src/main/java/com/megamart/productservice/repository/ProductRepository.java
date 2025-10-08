package com.megamart.productservice.repository;

import com.megamart.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByCategory(String category, Pageable pageable);
    
    Page<Product> findByBrand(String brand, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.title LIKE %:query% OR p.description LIKE %:query%")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);
    
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    Page<Product> findByAvailabilityStatus(String status, Pageable pageable);
    
    Page<Product> findByStockGreaterThan(Integer stock, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE " +
           "(:query IS NULL OR p.title LIKE %:query% OR p.description LIKE %:query%) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:brand IS NULL OR p.brand = :brand) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findProductsWithFilters(
        @Param("query") String query,
        @Param("category") String category,
        @Param("brand") String brand,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    
    @Query("SELECT DISTINCT p.category FROM Product p")
    List<String> findAllCategories();
    
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL")
    List<String> findAllBrands();
    
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.category = :category AND p.brand IS NOT NULL")
    List<String> findBrandsByCategory(@Param("category") String category);
}
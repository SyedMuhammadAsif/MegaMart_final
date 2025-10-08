package com.megamart.productservice.service;

import com.megamart.productservice.dto.ProductDTO;
import com.megamart.productservice.dto.ProductSearchRequest;
import com.megamart.productservice.entity.Category;
import com.megamart.productservice.entity.Product;
import com.megamart.productservice.exception.ProductNotFoundException;
import com.megamart.productservice.repository.CategoryRepository;
import com.megamart.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setTitle("Test Product");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setStock(10);
        product.setCategory("Electronics");

        productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setTitle("Test Product");
        productDTO.setPrice(BigDecimal.valueOf(99.99));
        productDTO.setStock(10);
        productDTO.setCategory("Electronics");
    }

    @Test
    void getAllProducts_ShouldReturnPagedProducts() {
        Page<Product> productPage = new PageImpl<>(Arrays.asList(product));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

        Page<ProductDTO> result = productService.getAllProducts(0, 10, "id", "asc");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Product", result.getContent().get(0).getTitle());
        verify(productRepository).findAll(any(Pageable.class));
    }

    @Test
    void getProductById_WhenExists_ShouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Optional<ProductDTO> result = productService.getProductById(1L);

        assertTrue(result.isPresent());
        assertEquals("Test Product", result.get().getTitle());
        verify(productRepository).findById(1L);
    }

    @Test
    void getProductById_WhenNotExists_ShouldReturnEmpty() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<ProductDTO> result = productService.getProductById(1L);

        assertFalse(result.isPresent());
        verify(productRepository).findById(1L);
    }

    @Test
    void createProduct_ShouldSaveAndReturnProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDTO result = productService.createProduct(productDTO);

        assertNotNull(result);
        assertEquals("Test Product", result.getTitle());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_WhenExists_ShouldUpdateAndReturn() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDTO result = productService.updateProduct(1L, productDTO);

        assertNotNull(result);
        assertEquals("Test Product", result.getTitle());
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_WhenNotExists_ShouldThrowException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> {
            productService.updateProduct(1L, productDTO);
        });

        verify(productRepository).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_WhenExists_ShouldDelete() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));

        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_WhenNotExists_ShouldThrowException() {
        when(productRepository.existsById(1L)).thenReturn(false);

        assertThrows(ProductNotFoundException.class, () -> {
            productService.deleteProduct(1L);
        });

        verify(productRepository).existsById(1L);
        verify(productRepository, never()).deleteById(1L);
    }

    @Test
    void updateStock_WithValidChange_ShouldUpdateStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDTO result = productService.updateStock(1L, 5);

        assertNotNull(result);
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateStock_WithNegativeResult_ShouldThrowException() {
        product.setStock(5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () -> {
            productService.updateStock(1L, -10);
        });

        verify(productRepository).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void searchProducts_ShouldReturnFilteredResults() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setQuery("test");
        request.setCategory("Electronics");
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("id");
        request.setSortDirection("asc");

        Page<Product> productPage = new PageImpl<>(Arrays.asList(product));
        when(productRepository.findProductsWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(productPage);

        Page<ProductDTO> result = productService.searchProducts(request);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository).findProductsWithFilters(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void createCategory_ShouldSaveAndReturnCategory() {
        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("Electronic products");

        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category result = productService.createCategory(category);

        assertNotNull(result);
        assertEquals("Electronics", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }
}
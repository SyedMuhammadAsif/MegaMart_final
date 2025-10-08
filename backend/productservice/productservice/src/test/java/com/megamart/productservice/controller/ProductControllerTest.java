package com.megamart.productservice.controller;

import com.megamart.productservice.dto.ProductDTO;
import com.megamart.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
        productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setTitle("Test Product");
        productDTO.setPrice(BigDecimal.valueOf(99.99));
        productDTO.setStock(10);
    }

    @Test
    void getAllProducts_ShouldReturnProducts() throws Exception {
        Page<ProductDTO> page = new PageImpl<>(Arrays.asList(productDTO));
        when(productService.getAllProducts(anyInt(), anyInt(), anyString(), anyString())).thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).getAllProducts(0, 30, "id", "asc");
    }

    @Test
    void getProductById_WhenExists_ShouldReturnProduct() throws Exception {
        when(productService.getProductById(1L)).thenReturn(Optional.of(productDTO));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).getProductById(1L);
    }

    @Test
    void getProductById_WhenNotExists_ShouldReturn404() throws Exception {
        when(productService.getProductById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createProduct_ShouldReturnCreatedProduct() throws Exception {
        when(productService.createProduct(any(ProductDTO.class))).thenReturn(productDTO);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Product\",\"price\":99.99}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).createProduct(any(ProductDTO.class));
    }

    @Test
    void updateProduct_ShouldReturnUpdatedProduct() throws Exception {
        when(productService.updateProduct(eq(1L), any(ProductDTO.class))).thenReturn(productDTO);

        mockMvc.perform(put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated Product\",\"price\":149.99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).updateProduct(eq(1L), any(ProductDTO.class));
    }

    @Test
    void deleteProduct_ShouldReturnSuccess() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).deleteProduct(1L);
    }

    @Test
    void searchProducts_ShouldReturnFilteredProducts() throws Exception {
        Page<ProductDTO> page = new PageImpl<>(Arrays.asList(productDTO));
        when(productService.searchProducts(any())).thenReturn(page);

        mockMvc.perform(get("/api/products/search")
                .param("keyword", "test")
                .param("category", "electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).searchProducts(any());
    }

    @Test
    void updateStock_ShouldReturnUpdatedProduct() throws Exception {
        when(productService.updateStock(eq(1L), eq(5))).thenReturn(productDTO);

        mockMvc.perform(post("/api/products/1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stockChange\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).updateStock(1L, 5);
    }

    @Test
    void updateStock_WithNullStockChange_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/products/1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(productService, never()).updateStock(anyLong(), anyInt());
    }
}
package com.megamart.productservice.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ProductDTOTest {

    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        productDTO = new ProductDTO();
    }

    @Test
    void testGettersAndSetters() {
        productDTO.setId(1L);
        productDTO.setTitle("Test Product");
        productDTO.setDescription("Test Description");
        productDTO.setCategory("Electronics");
        productDTO.setPrice(BigDecimal.valueOf(99.99));
        productDTO.setStock(10);
        productDTO.setBrand("TestBrand");
        productDTO.setSku("TEST-001");
        productDTO.setAvailabilityStatus("In Stock");
        productDTO.setThumbnail("test.jpg");
        productDTO.setTags(Arrays.asList("tag1", "tag2"));
        productDTO.setImages(Arrays.asList("img1.jpg", "img2.jpg"));

        assertEquals(1L, productDTO.getId());
        assertEquals("Test Product", productDTO.getTitle());
        assertEquals("Test Description", productDTO.getDescription());
        assertEquals("Electronics", productDTO.getCategory());
        assertEquals(BigDecimal.valueOf(99.99), productDTO.getPrice());
        assertEquals(10, productDTO.getStock());
        assertEquals("TestBrand", productDTO.getBrand());
        assertEquals("TEST-001", productDTO.getSku());
        assertEquals("In Stock", productDTO.getAvailabilityStatus());
        assertEquals("test.jpg", productDTO.getThumbnail());
        assertEquals(2, productDTO.getTags().size());
        assertEquals(2, productDTO.getImages().size());
    }

    @Test
    void testConstructors() {
        ProductDTO emptyDTO = new ProductDTO();
        assertNull(emptyDTO.getId());
        assertNull(emptyDTO.getTitle());

        ProductDTO fullDTO = new ProductDTO();
        fullDTO.setId(1L);
        fullDTO.setTitle("Product");
        fullDTO.setDescription("Description");
        fullDTO.setCategory("Category");
        fullDTO.setPrice(BigDecimal.valueOf(50.0));
        
        assertEquals(1L, fullDTO.getId());
        assertEquals("Product", fullDTO.getTitle());
        assertEquals("Description", fullDTO.getDescription());
    }

    @Test
    void testNullValues() {
        productDTO.setTitle(null);
        productDTO.setPrice(null);
        productDTO.setStock(null);

        assertNull(productDTO.getTitle());
        assertNull(productDTO.getPrice());
        assertNull(productDTO.getStock());
    }
}
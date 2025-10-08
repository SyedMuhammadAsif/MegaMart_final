package com.megamart.productservice.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
    }

    @Test
    void testGettersAndSetters() {
        product.setId(1L);
        product.setTitle("Test Product");
        product.setDescription("Test Description");
        product.setCategory("Electronics");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setDiscountPercentage(10.0);
        product.setRating(4.5);
        product.setStock(100);
        product.setBrand("TestBrand");
        product.setSku("TEST-001");
        product.setAvailabilityStatus("In Stock");
        product.setThumbnail("test.jpg");

        assertEquals(1L, product.getId());
        assertEquals("Test Product", product.getTitle());
        assertEquals("Test Description", product.getDescription());
        assertEquals("Electronics", product.getCategory());
        assertEquals(BigDecimal.valueOf(99.99), product.getPrice());
        assertEquals(10.0, product.getDiscountPercentage());
        assertEquals(4.5, product.getRating());
        assertEquals(100, product.getStock());
        assertEquals("TestBrand", product.getBrand());
        assertEquals("TEST-001", product.getSku());
        assertEquals("In Stock", product.getAvailabilityStatus());
        assertEquals("test.jpg", product.getThumbnail());
    }

    @Test
    void testConstructors() {
        Product emptyProduct = new Product();
        assertNull(emptyProduct.getId());
        assertNull(emptyProduct.getTitle());

        Product fullProduct = new Product();
        fullProduct.setId(1L);
        fullProduct.setTitle("Product");
        fullProduct.setDescription("Description");
        fullProduct.setCategory("Category");
        fullProduct.setPrice(BigDecimal.valueOf(50.0));

        assertEquals(1L, fullProduct.getId());
        assertEquals("Product", fullProduct.getTitle());
    }

    @Test
    void testRelationships() {
        ProductTag tag = new ProductTag();
        tag.setTag("electronics");
        product.setTags(Arrays.asList(tag));

        ProductImage image = new ProductImage();
        image.setImageUrl("image.jpg");
        product.setImages(Arrays.asList(image));

        assertEquals(1, product.getTags().size());
        assertEquals(1, product.getImages().size());
        assertEquals("electronics", product.getTags().get(0).getTag());
        assertEquals("image.jpg", product.getImages().get(0).getImageUrl());
    }

    @Test
    void testDefaultValues() {
        assertEquals(0.0, product.getDiscountPercentage());
        assertEquals(0.0, product.getRating());
        assertEquals(0, product.getStock());
        assertEquals("In Stock", product.getAvailabilityStatus());
    }
}
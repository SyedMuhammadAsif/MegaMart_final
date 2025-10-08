package com.megamart.productservice.service;

import com.megamart.productservice.dto.ProductDTO;
import com.megamart.productservice.dto.ProductReviewDTO;
import com.megamart.productservice.dto.ProductSearchRequest;
import com.megamart.productservice.entity.Product;
import com.megamart.productservice.entity.ProductImage;
import com.megamart.productservice.entity.ProductReview;
import com.megamart.productservice.entity.ProductTag;
import com.megamart.productservice.entity.Category;
import com.megamart.productservice.exception.CategoryNotFoundException;
import com.megamart.productservice.exception.ProductNotFoundException;
import com.megamart.productservice.repository.CategoryRepository;
import com.megamart.productservice.repository.ProductRepository;
import com.megamart.productservice.service.interfaces.ProductServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProductService implements ProductServiceInterface {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }
    
    public Page<ProductDTO> getAllProducts(int page, int size, String sortBy, String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return productRepository.findAll(pageable).map(this::convertToDTO);
    }
    
    public Optional<ProductDTO> getProductById(Long id) {
        return productRepository.findById(id).map(this::convertToDTO);
    }
    
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = convertToEntity(productDTO);
        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }
    
    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        
        updateProductFields(existingProduct, productDTO);
        Product updatedProduct = productRepository.save(existingProduct);
        return convertToDTO(updatedProduct);
    }
    
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
    
    public Page<ProductDTO> getProductsByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByCategory(category, pageable).map(this::convertToDTO);
    }
    
    public Page<ProductDTO> searchProducts(ProductSearchRequest request) {
        Sort sort = request.getSortDirection().equalsIgnoreCase("desc") ? 
            Sort.by(request.getSortBy()).descending() : Sort.by(request.getSortBy()).ascending();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        
        return productRepository.findProductsWithFilters(
            request.getQuery(),
            request.getCategory(),
            request.getBrand(),
            request.getMinPrice(),
            request.getMaxPrice(),
            pageable
        ).map(this::convertToDTO);
    }
    
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }
    
    public List<String> getAllBrands() {
        return productRepository.findAllBrands();
    }
    
    public List<String> getBrandsByCategory(String category) {
        return productRepository.findBrandsByCategory(category);
    }
    
    public Page<ProductDTO> getProductsInStock(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByStockGreaterThan(0, pageable).map(this::convertToDTO);
    }
    
    @Transactional
    public ProductDTO updateStock(Long id, Integer stockChange) {
        System.out.println("Updating stock for product " + id + " with change: " + stockChange);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        
        int oldStock = product.getStock();
        int newStock = oldStock + stockChange;
        
        System.out.println("Product " + id + " stock: " + oldStock + " -> " + newStock);
        
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative. Current: " + oldStock + ", Change: " + stockChange);
        }
        
        product.setStock(newStock);
        Product updatedProduct = productRepository.save(product);
        
        System.out.println("Stock updated successfully for product " + id + ". New stock: " + updatedProduct.getStock());
        
        return convertToDTO(updatedProduct);
    }
    
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setTitle(product.getTitle());
        dto.setDescription(product.getDescription());
        dto.setCategory(product.getCategory());
        dto.setPrice(product.getPrice());
        dto.setDiscountPercentage(product.getDiscountPercentage());
        dto.setRating(product.getRating());
        dto.setStock(product.getStock());
        dto.setBrand(product.getBrand());
        dto.setSku(product.getSku());
        dto.setAvailabilityStatus(product.getAvailabilityStatus());
        dto.setThumbnail(product.getThumbnail());
        
        if (product.getTags() != null) {
            dto.setTags(product.getTags().stream()
                .map(ProductTag::getTag)
                .toList());
        }
        
        if (product.getImages() != null) {
            dto.setImages(product.getImages().stream()
                .map(ProductImage::getImageUrl)
                .toList());
        }
        
        if (product.getReviews() != null) {
            dto.setReviews(product.getReviews().stream()
                .map(this::convertReviewToDTO)
                .toList());
        }
        
        return dto;
    }
    
    public List<Category> getAllCategoryEntities() {
        return categoryRepository.findAll();
    }
    
    @Transactional
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }
    
    @Transactional
    public Category updateCategory(Long id, Category category) {
        Category existingCategory = categoryRepository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
        
        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        existingCategory.setSlug(category.getSlug());
        
        return categoryRepository.save(existingCategory);
    }
    
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }
    
    private Product convertToEntity(ProductDTO dto) {
        Product product = new Product();
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice());
        product.setDiscountPercentage(dto.getDiscountPercentage());
        product.setRating(dto.getRating());
        product.setStock(dto.getStock());
        product.setBrand(dto.getBrand());
        product.setSku(dto.getSku());
        product.setAvailabilityStatus(dto.getAvailabilityStatus());
        product.setThumbnail(dto.getThumbnail());
        
        // Handle images
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            List<ProductImage> productImages = new ArrayList<>();
            for (int i = 0; i < dto.getImages().size(); i++) {
                ProductImage image = new ProductImage();
                image.setImageUrl(dto.getImages().get(i));
                image.setIsPrimary(i == 0); // First image is primary
                image.setProduct(product);
                productImages.add(image);
            }
            product.setImages(productImages);
        }
        
        return product;
    }
    
    private void updateProductFields(Product product, ProductDTO dto) {
        updateBasicFields(product, dto);
        updateImages(product, dto);
    }
    
    private void updateBasicFields(Product product, ProductDTO dto) {
        if (dto.getTitle() != null) product.setTitle(dto.getTitle());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getCategory() != null) product.setCategory(dto.getCategory());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());
        if (dto.getDiscountPercentage() != null) product.setDiscountPercentage(dto.getDiscountPercentage());
        if (dto.getRating() != null) product.setRating(dto.getRating());
        if (dto.getStock() != null) product.setStock(dto.getStock());
        if (dto.getBrand() != null) product.setBrand(dto.getBrand());
        if (dto.getSku() != null) product.setSku(dto.getSku());
        if (dto.getAvailabilityStatus() != null) product.setAvailabilityStatus(dto.getAvailabilityStatus());
        if (dto.getThumbnail() != null) product.setThumbnail(dto.getThumbnail());
    }
    
    private void updateImages(Product product, ProductDTO dto) {
        if (dto.getImages() == null) return;
        
        clearExistingImages(product);
        addNewImages(product, dto.getImages());
    }
    
    private void clearExistingImages(Product product) {
        if (product.getImages() != null) {
            product.getImages().clear();
        } else {
            product.setImages(new ArrayList<>());
        }
    }
    
    private void addNewImages(Product product, List<String> imageUrls) {
        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage image = new ProductImage();
            image.setImageUrl(imageUrls.get(i));
            image.setIsPrimary(i == 0);
            image.setProduct(product);
            product.getImages().add(image);
        }
    }
    
    private ProductReviewDTO convertReviewToDTO(ProductReview review) {
        ProductReviewDTO dto = new ProductReviewDTO();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewerName(review.getReviewerName());
        dto.setReviewerEmail(review.getReviewerEmail());
        dto.setReviewDate(review.getReviewDate());
        return dto;
    }
}
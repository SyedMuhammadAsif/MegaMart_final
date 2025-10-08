import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { CommonModule, ViewportScroller } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductService } from '../../services/product-service';
import { Product } from '../../models/product';



@Component({
  selector: 'app-product-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
  changeDetection: ChangeDetectionStrategy.Default
})


export class ProductList implements OnInit {
  // Product data
  allProducts: Product[] = [];
  filteredProducts: Product[] = [];
  displayedProducts: Product[] = [];
  
  // Filter options
  availableBrands: string[] = [];
  selectedBrands: string[] = [];
  inStockOnly: boolean = false;
  minRating: number = 0;
  minPrice: number = 0;
  maxPrice: number = 10000;
  
  // Filter state management - Angular way for proper data binding
  tempSelectedBrands: Set<string> = new Set();
  tempInStockOnly: boolean = false;
  tempMinRating: number = 0;
  tempMinPrice: number = 0;
  tempMaxPrice: number = 10000;
  
  // Sort options
  sortBy: string = '';
  sortOrder: string = 'asc';
  sortOptions = [
    { value: '', label: 'No Sorting' },
    { value: 'price', label: 'Price' },
    { value: 'rating', label: 'Rating' },
    { value: 'title', label: 'Name' }
  ];
  
  //pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;
  totalPages: number = 0;
  
  // Chunked loading optimization
 

  
  // Loading states
  loading: boolean = false;
  loadingMore: boolean = false;
  selectedCategory: string = '';
  categoryDisplayName: string = '';
  


  constructor(
    private productService: ProductService, 
    private route: ActivatedRoute,
    private viewPortScroller: ViewportScroller,private router:Router
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe({
      next:(params)=>{
      if (params['categoryName']) {
        this.selectedCategory = params['categoryName'];
        this.categoryDisplayName = this.formatCategoryName(params['categoryName']);
        this.loadCategoryProducts(this.selectedCategory);
        this.loadCategoryBrands(this.selectedCategory);
      }
    this.viewPortScroller.scrollToPosition([0, 0]);
    },
  error:(error)=>{

    console.log('Error in loading the products ',error);

  }
  });
 
  }

  
  loadCategoryProducts(category: string): void {
    this.loading = true;
    this.loadCategoryPage(category, 0); // Load first page
  }

  loadCategoryBrands(category: string): void {
    console.log('Loading brands for category:', category);
    this.productService.getBrandsByCategory(category).subscribe({
      next: (brands) => {
        console.log('Received brands:', brands);
        this.availableBrands = brands;
        // Fallback: if no brands found, load all brands
        if (brands.length === 0) {
          console.log('No brands found for category, loading all brands');
          this.productService.getBrands().subscribe(allBrands => {
            this.availableBrands = allBrands;
          });
        }
      },
      error: (error) => {
        console.error('Error loading category brands:', error);
      }
    });
  }

  // NEW: Load specific page from backend
  loadCategoryPage(category: string, page: number): void {
    console.log('Requesting category:', category, 'page:', page, 'size:', this.itemsPerPage, 'sort:', this.sortBy, this.sortOrder);
    
    // Use search API with category filter to get sorting support
    this.productService.searchProducts({
      category: category,
      page: page,
      size: this.itemsPerPage,
      sortBy: this.sortBy || 'id',
      sortDirection: this.sortOrder || 'asc'
    }).subscribe({
      next: (response: any) => {
        console.log('Backend search response:', response);
        console.log('Products in response:', response.products?.length || 0);
        console.log('Total elements:', response.total);
        this.allProducts = response.products || [];
        this.filteredProducts = this.allProducts; // Set filtered products for display count
        this.totalPages = Math.ceil((response.total || 0) / this.itemsPerPage);
        this.currentPage = page + 1; // Backend uses 0-based, frontend uses 1-based
        this.displayedProducts = this.allProducts;
        this.initializeFilters(); // Initialize price range filters
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.loading = false;
      }
    });
  }


  // REMOVED: processProducts - no longer needed with backend pagination

  


  formatCategoryName(category: string): string {
    return category
      .split('-')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  private clearCache(): void {
    this.allProducts = [];
  }

  initializeFilters(): void {
    // Set price range from loaded products
    if (this.allProducts.length > 0) {
      const prices = this.allProducts.map(p => p.price || 0);
      this.minPrice = Math.min(...prices);
      this.maxPrice = Math.max(...prices);
      this.tempMinPrice = this.minPrice;
      this.tempMaxPrice = this.maxPrice;
    }
  }

  resetFilters(): void {
    this.selectedBrands = [];
    this.tempSelectedBrands.clear();
    this.inStockOnly = false;
    this.tempInStockOnly = false;
    this.minRating = 0;
    this.tempMinRating = 0;
    this.currentPage = 1;
  }

  applyFilters(): void {
    // Apply current filter values using proper Angular data binding
    this.selectedBrands = Array.from(this.tempSelectedBrands);
    this.inStockOnly = this.tempInStockOnly;
    this.minRating = this.tempMinRating;
    this.minPrice = this.tempMinPrice;
    this.maxPrice = this.tempMaxPrice;

    this.filteredProducts = this.allProducts.filter(product => {
      // Brand filter
      if (this.selectedBrands.length > 0 && 
          !this.selectedBrands.includes(product.brand || '')) {
        return false;
      }
      
      // Stock filter
      if (this.inStockOnly && 
          (!product.stock || product.stock <= 0)) {
        return false;
      }
      
      // Rating filter
      if (product.rating && product.rating < this.minRating) {
        return false;
      }
      
      // Price filter
      if (product.price && 
          (product.price < this.minPrice || product.price > this.maxPrice)) {
        return false;
      }
      
      return true;
    });
    
    this.applySorting();
    this.updatePagination();
  }

  applySorting(): void {
    if (!this.sortBy) return;
    
    this.filteredProducts.sort((a, b) => {
      let valueA: any, valueB: any;
      
      switch (this.sortBy) {
        case 'price':
          valueA = a.price || 0;
          valueB = b.price || 0;
          break;
        case 'rating':
          valueA = a.rating || 0;
          valueB = b.rating || 0;
          break;
        case 'title':
          valueA = a.title || '';
          valueB = b.title || '';
          break;
        default:
          return 0;
      }
      
      if (this.sortOrder === 'desc') {
        return valueB > valueA ? 1 : -1;
      } else {
        return valueA > valueB ? 1 : -1;
      }
    });
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredProducts.length / this.itemsPerPage);
    this.currentPage = Math.min(this.currentPage, this.totalPages || 1);
    
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.displayedProducts = this.filteredProducts.slice(startIndex, endIndex);
    
    this.viewPortScroller.scrollToPosition([0, 0]);

  }

  onBrandChange(brand: string, event: Event): void {
    const target = event.target as HTMLInputElement;
    const checked = target.checked;
    
    if (checked) {
      this.tempSelectedBrands.add(brand);
    } else {
      this.tempSelectedBrands.delete(brand);
    }
  }

  // Helper method for template to check if brand is selected
  isBrandSelected(brand: string): boolean {
    return this.tempSelectedBrands.has(brand);
  }

  onSortChange(): void {
    // OLD: Client-side sorting
    // CHANGED: Reload from backend with sort parameters
    this.loading = true;
    this.loadCategoryPage(this.selectedCategory, 0); // Reset to first page with new sort
  }

  clearFilters(): void {
    // Clear all filter values
    this.tempSelectedBrands.clear();
    this.selectedBrands = [];
    this.tempInStockOnly = false;
    this.inStockOnly = false;
    this.tempMinRating = 0;
    this.minRating = 0;
    
    // Reset price range to original values
    if (this.allProducts.length > 0) {
      const prices = this.allProducts.map(p => p.price || 0);
      this.minPrice = Math.min(...prices);
      this.maxPrice = Math.max(...prices);
    } else {
      this.minPrice = 0;
      this.maxPrice = 10000;
    }
    this.tempMinPrice = this.minPrice;
    this.tempMaxPrice = this.maxPrice;
    
    // Reset to show all products
    this.filteredProducts = this.allProducts;
    this.updatePagination();
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.loading = true;
      // CHANGED: Hit backend for each page change
      this.loadCategoryPage(this.selectedCategory, page - 1); // Convert to 0-based for backend
    }
  }

  getPaginationArray(): number[] {
    const pages: number[] = [];
    const start = Math.max(1, this.currentPage - 2);
    const end = Math.min(this.totalPages, this.currentPage + 2);
    
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }

  getStars(rating: number): string[] {
    const stars: string[] = [];
    const fullStars = Math.floor(rating);
    
    
    for (let i = 0; i < fullStars; i++) {
      stars.push('★');
    }
    while (stars.length < 5) {
      stars.push('☆');
    }
    return stars;
  }

  // Helper method for template
  getMin(a: number, b: number): number {
    return Math.min(a, b);
  }

  trackByBrand(index: number, brand: string): string {
    return brand;
  }
}
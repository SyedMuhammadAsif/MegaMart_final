import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductService } from '../../services/product-service';
import { Product } from '../../models/product';

@Component({
  selector: 'app-search-results',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './search-results.html',
  styleUrl: './search-results.css'
})
export class SearchResults implements OnInit {
  searchQuery: string = '';
  searchResults: Product[] = [];
  loading: boolean = false;
  noResults: boolean = false;

  constructor(
    private route: ActivatedRoute,private router:Router,
    private productService: ProductService
  ) {}

  ngOnInit(): void {
    // Scroll to top
    window.scrollTo({ top: 0, behavior: 'smooth' });
    
    // Get search query from URL params
    this.route.queryParams.subscribe(params => {
      this.searchQuery = params['q'] || '';
      this.loadSearchResults();
    });

   

    
  }

  private loadSearchResults(): void {
    if (!this.searchQuery.trim()) {
      this.searchResults = [];
      this.noResults = true;
      this.loading = false;
      return;
    }

    this.loading = true;
    this.noResults = false;
    
    this.productService.searchProducts({
      keyword: this.searchQuery,
      page: 0,
      size: 50 
    }).subscribe({
      next: (response) => {
        this.searchResults = response.products || [];
        this.noResults = this.searchResults.length === 0;
        this.loading = false;
        console.log('Backend search results:', this.searchResults);
      },
      error: (error) => {
        console.error('Error loading search results:', error);
        this.searchResults = [];
        this.noResults = true;
        this.loading = false;
      }
    });
  }

  getDiscountedPrice(product: Product): number {
    if (product.discountPercentage && product.discountPercentage > 0) {
      return product.price * (1 - product.discountPercentage / 100);
    }
    return product.price;
  }

  formatPrice(price: number): string {
    return price.toFixed(2);
  }

  hasDiscount(product: Product): boolean {
    return !!(product.discountPercentage && product.discountPercentage > 0);
  }

  getRatingStars(rating: number): string[] {
    const stars = [];
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 >= 0.5;
    
    for (let i = 0; i < fullStars; i++) {
      stars.push('full');
    }
    
    if (hasHalfStar) {
      stars.push('half');
    }
    
    while (stars.length < 5) {
      stars.push('empty');
    }
    
    return stars;
  }
} 
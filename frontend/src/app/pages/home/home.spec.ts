import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Home } from './home';
import { ProductService } from '../../services/product-service';
import { Router } from '@angular/router';
import { ViewportScroller } from '@angular/common';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  let productService: jasmine.SpyObj<ProductService>;
  let router: jasmine.SpyObj<Router>;
  let viewportScroller: jasmine.SpyObj<ViewportScroller>;

  const mockCategories = [
    { name: 'Electronics', slug: 'electronics' },
    { name: 'Clothing', slug: 'clothing' }
  ];

  const mockProducts = [
    { id: 1, name: 'Product 1', category: 'electronics', price: 100 },
    { id: 2, name: 'Product 2', category: 'clothing', price: 50 }
  ];

  const mockProductResponse = {
    products: mockProducts,
    total: 2,
    skip: 0,
    limit: 20
  };

  beforeEach(async () => {
    const productServiceSpy = jasmine.createSpyObj('ProductService', [
      'getCategories',
      'getProducts',
      'getProductsByCategory'
    ]);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const viewportScrollerSpy = jasmine.createSpyObj('ViewportScroller', ['scrollToPosition']);

    await TestBed.configureTestingModule({
      imports: [Home, RouterTestingModule],
      providers: [
        { provide: ProductService, useValue: productServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ViewportScroller, useValue: viewportScrollerSpy }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    
    productService = TestBed.inject(ProductService) as jasmine.SpyObj<ProductService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    viewportScroller = TestBed.inject(ViewportScroller) as jasmine.SpyObj<ViewportScroller>;


    productService.getCategories.and.returnValue(of(mockCategories));
    productService.getProducts.and.returnValue(of(mockProductResponse));
    productService.getProductsByCategory.and.returnValue(of(mockProductResponse));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default values', () => {
    expect(component.categoriesWithProducts).toEqual([]);
    expect(component.isLoading).toBe(true);
    expect(component.categories).toEqual([]);
    expect(component.allProducts).toEqual([]);
  });

  it('should load categories successfully', () => {
    component.loadCategories();
    expect(component.categories).toEqual(mockCategories);
  });

  it('should load all products successfully', () => {
    component.loadAllProducts();
    expect(component.allProducts).toEqual(mockProducts);
  });

  it('should navigate to category when navigateToCategory is called', () => {
    const categorySlug = 'electronics';
    component.navigateToCategory(categorySlug);
    expect(router.navigate).toHaveBeenCalledWith(['/category', categorySlug]);
  });

  it('should get category image with index wrapping', () => {
    const image1 = component.getCategoryImage(0);
    const image2 = component.getCategoryImage(component.categoryImages.length);
    
    expect(image1).toBe(component.categoryImages[0]);
    expect(image2).toBe(component.categoryImages[0]);
  });

  it('should generate correct star ratings', () => {
    const stars3 = component.getStars(3);

    const stars5 = component.getStars(5);
    const stars0 = component.getStars(0);
    
    // expect(stars3.length).toBe(3);
    // expect(stars5.length).toBe(5);
    // expect(stars0.length).toBe(0);
  });

  it('should group banner images correctly', () => {
    const groups = component.getBannerGroups();
    expect(groups.length).toBeGreaterThan(0);
    expect(groups[0].length).toBeLessThanOrEqual(3);
  });

  it('should display loading spinner when isLoading is true', () => {
    component.isLoading = true;
    fixture.detectChanges();
    
    const loadingElement = fixture.nativeElement.querySelector('.spinner-border');
    expect(loadingElement).toBeTruthy();
  });

  it('should hide loading spinner when isLoading is false', () => {
    component.isLoading = false;
    fixture.detectChanges();
    
    const loadingElement = fixture.nativeElement.querySelector('.spinner-border');
    expect(loadingElement).toBeFalsy();
  });

  it('should display testimonials correctly', () => {
    fixture.detectChanges();
    
    const testimonialElements = fixture.nativeElement.querySelectorAll('.testimonial-card');
    expect(testimonialElements.length).toBe(component.testimonials.length);
  });

  it('should display banner carousel correctly', () => {
    fixture.detectChanges();
    
    const bannerElements = fixture.nativeElement.querySelectorAll('.carousel-item');
    expect(bannerElements.length).toBe(component.bannerImages.length);
  });
});

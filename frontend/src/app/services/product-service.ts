import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EnvVariables } from '../env/env-variables';

export interface ProductApiResponse {
  products: any[];
  total?: number;
  skip?: number;
  limit?: number;
}


@Injectable({
  providedIn: 'root'
})
export class ProductService {

  
  //Category Images
  images:string[]=["https://www.centuryply.com/assets/img/blog/25-08-22/blog-home-decoration-3.jpg",
"https://cdn.dummyjson.com/product-images/fragrances/calvin-klein-ck-one/1.webp",
"https://cdn.dummyjson.com/product-images/furniture/annibale-colombo-bed/1.webp",
"https://cdn.dummyjson.com/product-images/groceries/apple/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/home-decoration/decoration-swing/2.webp",
"https://cdn.dummyjson.com/product-images/kitchen-accessories/bamboo-spatula/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/mens-shirts/blue-&-black-check-shirt/1.webp",
"https://cdn.dummyjson.com/product-images/mens-shoes/nike-air-jordan-1-red-and-black/1.webp",
"https://cdn.dummyjson.com/product-images/mens-watches/brown-leather-belt-watch/2.webp",
"https://cdn.dummyjson.com/product-images/mobile-accessories/amazon-echo-plus/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/motorcycle/generic-motorcycle/2.webp",
"https://cdn.dummyjson.com/product-images/skin-care/attitude-super-leaves-hand-soap/3.webp",
"https://cdn.dummyjson.com/product-images/smartphones/iphone-5s/2.webp",
"https://cdn.dummyjson.com/product-images/sports-accessories/american-football/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/sunglasses/black-sun-glasses/3.webp",
"https://cdn.dummyjson.com/product-images/tablets/ipad-mini-2021-starlight/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/tops/blue-frock/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/vehicle/300-touring/6.webp",
"https://cdn.dummyjson.com/product-images/womens-bags/blue-women's-handbag/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/womens-dresses/black-women's-gown/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/womens-jewellery/green-crystal-earring/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/womens-shoes/black-&-brown-slipper/thumbnail.webp",
"https://cdn.dummyjson.com/product-images/womens-watches/iwc-ingenieur-automatic-steel/thumbnail.webp"
];

  
  constructor(private http:HttpClient){}

  private baseUrl = `${EnvVariables.productServiceUrl}`;

 

  // Search results functionality
  private searchResultsSource = new BehaviorSubject<number[]>([]);
  searchResults = this.searchResultsSource.asObservable();
  setSearchResults(productIds: number[]): void {
    this.searchResultsSource.next(productIds);
  }

  searchProducts(params: {
    keyword?: string;
    category?: string;
    brand?: string;
    minPrice?: number;
    maxPrice?: number;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  }): Observable<ProductApiResponse> {
    const searchParams = new URLSearchParams();
    
    if (params.keyword) searchParams.set('keyword', params.keyword);
    if (params.category) searchParams.set('category', params.category);
    if (params.brand) searchParams.set('brand', params.brand);
    if (params.minPrice !== undefined) searchParams.set('minPrice', params.minPrice.toString());
    if (params.maxPrice !== undefined) searchParams.set('maxPrice', params.maxPrice.toString());
    if (params.page !== undefined) searchParams.set('page', params.page.toString());
    if (params.size !== undefined) searchParams.set('size', params.size.toString());
    if (params.sortBy) searchParams.set('sortBy', params.sortBy);
    if (params.sortDirection) searchParams.set('sortDirection', params.sortDirection);

    return this.http.get<any>(`${this.baseUrl}/products/search?${searchParams.toString()}`).pipe(
      map(response => ({ 
        products: response.data?.content || response.data || [],
        total: response.data?.totalElements || 0
      }))
    );
  }

  getBrands(): Observable<string[]> {
    return this.http.get<any>(`${this.baseUrl}/brands`).pipe(
      map(response => response.data || [])
    );
  }

  // Get brands for specific category
  getBrandsByCategory(category: string): Observable<string[]> {
    return this.http.get<any>(`${this.baseUrl}/brands/category/${category}`).pipe(
      map(response => response.data || [])
    );
  }

  // Categories from local products, shaped as { name, slug }

getCategories(): Observable<{ name: string; slug: string; }[]> {
  return this.http.get<any>(`${this.baseUrl}/categories`).pipe(
    map(response => {
      console.log('Backend categories response:', response); // DEBUG
      const categories = response.data || [];
      console.log('Categories count:', categories.length); // DEBUG
      return categories.map((category: any) => ({
        name: category.name || '',
        slug: category.slug || category.name || ''
      }));
    })
  );
}

  // Back-compat alias used by some components
  getCategory(): Observable<{ name: string; slug: string; }[]> {
    return this.getCategories();
  }

  // Products list shaped to { products } to match existing consumers
  getProducts(page: number = 0, size: number = 1000, sortBy: string = 'id', sortDirection: string = 'asc'): Observable<ProductApiResponse> {
    const params = `page=${page}&size=${size}&sortBy=${sortBy}&sortDirection=${sortDirection}`;
    return this.http.get<any>(`${this.baseUrl}/products?${params}`).pipe(
      map(response => ({ 
        products: response.data?.content || response.data || [],
        total: response.data?.totalElements || 0
      }))
    );
  }

  // Get products by category slug, shaped to { products }
  getProductsByCategory(categorySlug: string, page: number = 0, size: number = 10): Observable<ProductApiResponse> {
    const params = `page=${page}&size=${size}`;
    const url = `${this.baseUrl}/products/category/${categorySlug}?${params}`;
    console.log('API Call URL:', url); // DEBUG
    return this.http.get<any>(url).pipe(
      tap((response: any) => console.log('Category API response:', response.data?.content?.length || 0, 'products')),
      map((response: any) => ({ 
        products: response.data?.content || response.data || [],
        total: response.data?.totalElements || 0
      }))
    );
  }

  // Back-compat alias
  getCategoryProducts(category: string, page: number = 0, size: number = 10): Observable<ProductApiResponse> {
    return this.getProductsByCategory(category, page, size);
  }

  getCategoryImages(index:number){
    return this.images[index];
  }

  getProductById(id:number){
    return this.http.get<any>(`${this.baseUrl}/products/${id}`).pipe(
      map(response => response.data)
    );
  }

  private titleCase(input: string): string {
    return (input || '')
      .split('-')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');

     
  }
}
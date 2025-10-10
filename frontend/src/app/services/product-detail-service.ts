import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable, of, tap } from 'rxjs';
import { Product } from '../models/product';
import { EnvVariables } from '../env/env-variables';



@Injectable({
  providedIn: 'root'
})
export class ProductDetailService {
  private baseUrl = `${EnvVariables.productServiceUrl}/products`;

  
  private allProductsCache: Product[] | null = null;

 constructor(private http: HttpClient) {}


getProductById(id: number): Observable<Product> { // Explicitly type return as Product
    
    return this.http.get<any>(`${this.baseUrl}/${id}`).pipe(
      map(response => response.data), // Extract data from Spring Boot response
      tap(product => console.log('Fetched product by ID:', product))
    );
  }



 private getAllProducts(): Observable<Product[]> {
    if (this.allProductsCache) {
      return of(this.allProductsCache);
    } else {
      
      return this.http.get<any>(`${this.baseUrl}?page=0&size=1000`).pipe(
        map(response => response.data?.content || response.data || []),
        tap(products => {
          this.allProductsCache = products;
          console.log('Cached all products:', products);
        })
      );
    }
  }


   getSimilarProducts(category: string, excludeProductId?: number, limit: number = 8): Observable<Product[]> {
    return this.getAllProducts().pipe(
      map(allProds => {
        let filteredProducts: Product[] = [];
        if (category === 'all') {
            filteredProducts = allProds;
        } else {
            filteredProducts = allProds.filter(p =>
                p.category === category && p.id !== excludeProductId
            );
        }
        return filteredProducts.slice(0, limit);
      }),
      tap(similarProds => console.log(`Fetched ${similarProds.length} similar products for category '${category}' (excluding ID ${excludeProductId}):`, similarProds))
    );
  }
}
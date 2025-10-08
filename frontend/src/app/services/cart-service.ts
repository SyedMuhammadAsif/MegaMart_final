import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { CartItem, Cart } from '../models/cart-items';
import { Product } from '../models/product';
import { ToastService } from './toast-service';
import { AuthService } from './auth.service';
import { EnvVariables } from '../env/env-variables';

export interface MicroserviceCartItem {
  id: number;
  productId: number;
  quantity: number;
  lineTotal: number;
}

export interface MicroserviceCartResponse {
  id: number;
  userId: number;
  total: number;
  items: MicroserviceCartItem[];
  totalItems: number;
  totalPrice: number;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private cartApiUrl = `${EnvVariables.cartServiceUrl}/cart`;
  private productApiUrl = `${EnvVariables.productServiceUrl}/products`;
  private cartSubject = new BehaviorSubject<Cart>({ items: [], totalItems: 0, totalPrice: 0 });

  public cart$ = this.cartSubject.asObservable();

  get currentCart(): Cart {
    return this.cartSubject.value;
  }

  constructor(private http: HttpClient, private toastService: ToastService, private authService: AuthService) {
    this.loadCart();
    
    // Listen to authentication state changes
    this.authService.currentUser$.subscribe(user => {
      console.log('Auth state changed, refreshing cart for user:', user);
      this.loadCart();
    });
  }

  private getCurrentUserId(): number | null {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.email) {
      console.log('No current user or email found');
      return null;
    }
    
    // Get user-admin service user ID by email (synchronously from cache if available)
    const cachedUserId = sessionStorage.getItem(`userAdminId_${currentUser.email}`);
    if (cachedUserId) {
      console.log('Using cached user-admin service userId:', cachedUserId);
      return Number(cachedUserId);
    }
    
    // If not cached, fetch and cache it
    this.fetchAndCacheUserAdminId(currentUser.email);
    
    // Fallback to auth service ID for now
    console.log('Using auth service userId as fallback:', currentUser.id);
    return currentUser.id ? Number(currentUser.id) : null;
  }
  
  private fetchAndCacheUserAdminId(email: string): void {
    this.http.get<any[]>(`${EnvVariables.userServiceUrl}/users`).subscribe({
      next: (users) => {
        const matchedUser = users?.find(u => u.email === email);
        if (matchedUser?.id) {
          sessionStorage.setItem(`userAdminId_${email}`, matchedUser.id.toString());
          console.log('Fetched and cached user-admin service ID:', matchedUser.id);
          
          // Migrate cart from auth service ID to user-admin service ID
          const currentUser = this.authService.getCurrentUser();
          if (currentUser?.id && currentUser.id !== matchedUser.id) {
            this.migrateCart(currentUser.id, matchedUser.id);
          } else {
            this.loadCart();
          }
        }
      },
      error: (error) => {
        console.error('Failed to fetch user-admin service ID:', error);
      }
    });
  }
  
  private migrateCart(fromUserId: number, toUserId: number): void {
    console.log(`Migrating cart from user ${fromUserId} to user ${toUserId}`);
    
    // Get cart from old user ID
    this.http.get<MicroserviceCartResponse>(`${this.cartApiUrl}/${fromUserId}`).subscribe({
      next: (oldCart) => {
        if (oldCart?.items?.length > 0) {
          console.log('Found cart items to migrate:', oldCart.items.length);
          
          // Add each item to new user's cart
          const migrationPromises = oldCart.items.map(item => 
            this.http.post(`${this.cartApiUrl}/${toUserId}/items`, {
              productId: item.productId,
              quantity: item.quantity
            }).toPromise().catch(err => {
              console.error('Failed to migrate cart item:', item, err);
              return null;
            })
          );
          
          Promise.all(migrationPromises).then(() => {
            console.log('Cart migration completed');
            // Clear old cart
            this.http.delete(`${this.cartApiUrl}/${fromUserId}`).subscribe({
              next: () => console.log('Old cart cleared'),
              error: (err) => console.error('Failed to clear old cart:', err)
            });
            // Load new cart
            this.loadCart();
          });
        } else {
          console.log('No cart items to migrate');
          this.loadCart();
        }
      },
      error: (error) => {
        console.error('Failed to get old cart for migration:', error);
        this.loadCart();
      }
    });
  }

  refreshCart(): void {
    this.loadCart();
  }

  loadCart(): void {
    const userId = this.getCurrentUserId();
    console.log('Loading cart for userId:', userId);
    
    if (!userId) {
      this.cartSubject.next({ items: [], totalItems: 0, totalPrice: 0 });
      return;
    }
    
    console.log('Making cart API call to:', `${this.cartApiUrl}/${userId}`);
    this.http.get<MicroserviceCartResponse>(`${this.cartApiUrl}/${userId}`).pipe(
      switchMap(cartResponse => {
        if (!cartResponse || !cartResponse.items || cartResponse.items.length === 0) {
          return of({ items: [], totalItems: 0, totalPrice: 0 });
        }

        // Get product details for each cart item
        const productRequests = cartResponse.items.map(item =>
          this.http.get<any>(`${this.productApiUrl}/${item.productId}`).pipe(
            map(response => response.data || response), // Handle both formats
            catchError(() => of({ id: item.productId, title: 'Product', price: 0 })) // Fallback
          )
        );

        return forkJoin(productRequests).pipe(
          map(products => {
            const cartItems: CartItem[] = cartResponse.items.map((item, index) => ({
              id: item.id.toString(),
              ProductID: item.productId,
              Quantity: item.quantity,
              TotalPrice: Number(item.lineTotal),
              CartItemID: item.id,
              user_id: userId.toString(),
              Product: products[index]
            }));
            
            return {
              items: cartItems,
              totalItems: cartResponse.totalItems || cartItems.length,
              totalPrice: Number(cartResponse.totalPrice || cartResponse.total || 0)
            };
          })
        );
      }),
      catchError(error => {
        console.error('Cart microservice error:', error);
        return of({ items: [], totalItems: 0, totalPrice: 0 });
      })
    ).subscribe({
      next: (cart) => this.cartSubject.next(cart),
      error: (error) => {
        console.error('Error loading cart:', error);
        this.cartSubject.next({ items: [], totalItems: 0, totalPrice: 0 });
      }
    });
  }

  addToCart(productId: number, quantity: number = 1): Observable<any> {
    console.log('CartService.addToCart called with:', { productId, quantity });
    
    // Check localStorage token
    const token = localStorage.getItem('token');
    console.log('Token in localStorage:', token ? 'EXISTS' : 'NOT FOUND');
    console.log('Token preview:', token ? token.substring(0, 50) + '...' : 'none');
    
    const userId = this.getCurrentUserId();
    console.log('Current userId:', userId);
    
    if (!userId) {
      console.log('No userId found, showing error');
      this.toastService.showError('Please login to add items to cart');
      return of(null);
    }

    const addItemRequest = {
      productId: productId,
      quantity: quantity
    };
    
    const url = `${this.cartApiUrl}/${userId}/items`;
    console.log('Making POST request to:', url);
    console.log('Request payload:', addItemRequest);

    return this.http.post(url, addItemRequest).pipe(
      tap((response) => {
        console.log('Cart API response:', response);
        this.loadCart();
        this.toastService.showSuccess('Item added to cart successfully!');
      }),
      catchError(error => {
        console.error('Error adding to cart:', error);
        this.toastService.showError('Failed to add item to cart');
        return of(null);
      })
    );
  }

  removeFromCart(cartItemId: string): Observable<any> {
    const userId = this.getCurrentUserId();
    
    if (!userId) {
      this.toastService.showError('Please login to manage cart');
      return of(null);
    }

    return this.http.delete(`${this.cartApiUrl}/${userId}/items/${cartItemId}`).pipe(
      tap(() => {
        this.loadCart();
        this.toastService.showInfo('Item removed from cart');
      }),
      catchError(error => {
        console.error('Error removing from cart:', error);
        this.toastService.showError('Failed to remove item from cart');
        return of(null);
      })
    );
  }

  updateQuantity(cartItemId: string, newQuantity: number): Observable<any> {
    const userId = this.getCurrentUserId();
    
    if (!userId) {
      this.toastService.showError('Please login to manage cart');
      return of(null);
    }

    if (newQuantity <= 0) {
      return this.removeFromCart(cartItemId);
    }

    const updateRequest = {
      quantity: newQuantity
    };

    return this.http.patch(`${this.cartApiUrl}/${userId}/items/${cartItemId}`, updateRequest).pipe(
      tap(() => this.loadCart()),
      catchError(error => {
        console.error('Error updating cart quantity:', error);
        this.toastService.showError('Failed to update quantity');
        return of(null);
      })
    );
  }

  clearCart(): Observable<any> {
    const userId = this.getCurrentUserId();
    
    if (!userId) {
      this.toastService.showError('Please login to manage cart');
      return of(null);
    }

    return this.http.delete(`${this.cartApiUrl}/${userId}`).pipe(
      tap(() => {
        this.loadCart();
        this.toastService.showInfo('Cart cleared successfully');
      }),
      catchError(error => {
        console.error('Error clearing cart:', error);
        this.toastService.showError('Failed to clear cart');
        return of(null);
      })
    );
  }

  getCurrentCart(): Cart {
    return this.cartSubject.value;
  }

  getCartItemCount(): Observable<number> {
    return this.cart$.pipe(
      map(cart => cart.items.length) // Return number of unique products instead of total quantity
    );
  }

  getCartTotal(): Observable<number> {
    return this.cart$.pipe(map(cart => cart.totalPrice));
  }

  private calculateCartTotals(items: CartItem[]): Cart {
    const totalItems = items.reduce((sum, item) => sum + item.Quantity, 0);
    const totalPrice = items.reduce((sum, item) => sum + item.TotalPrice, 0);

    return {
      items,
      totalItems,
      totalPrice: Number(totalPrice.toFixed(2))
    };
  }

  isProductInCart(productId: number): Observable<boolean> {
    return this.cart$.pipe(
      map(cart => cart.items.some(item => item.ProductID === productId))
    );
  }

  getProductQuantityInCart(productId: number): Observable<number> {
    return this.cart$.pipe(
      map(cart => {
        const item = cart.items.find(item => item.ProductID === productId);
        return item ? item.Quantity : 0;
      })
    );
  }
} 
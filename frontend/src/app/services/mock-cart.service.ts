import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';
import { CartItem, Cart } from '../models/cart-items';
import { Product } from '../models/product';
import { ToastService } from './toast-service';
import { EnvVariables } from '../env/env-variables';

@Injectable({
  providedIn: 'root'
})
export class MockCartService {
  private cartSubject = new BehaviorSubject<Cart>({ items: [], totalItems: 0, totalPrice: 0 });
  public cart$ = this.cartSubject.asObservable();

  get currentCart(): Cart {
    return this.cartSubject.value;
  }

  constructor(private http: HttpClient, private toastService: ToastService) {
    this.loadCart();
  }

  private getCurrentUserId(): string {
    return localStorage.getItem('userId') || '246293';
  }

  refreshCart(): void {
    this.loadCart();
  }

  loadCart(): void {
    const userId = this.getCurrentUserId();
    
    // Load cart items from localStorage or JSON server
    this.http.get<CartItem[]>(`${EnvVariables.apiBaseUrl}/cart?user_id=${userId}`).pipe(
      map(items => {
        const totalItems = items.reduce((sum, item) => sum + item.Quantity, 0);
        const totalPrice = items.reduce((sum, item) => sum + item.TotalPrice, 0);
        
        return {
          items,
          totalItems,
          totalPrice: Number(totalPrice.toFixed(2))
        };
      }),
      catchError(() => of({ items: [], totalItems: 0, totalPrice: 0 }))
    ).subscribe(cart => this.cartSubject.next(cart));
  }

  addToCart(productId: number, quantity: number = 1): Observable<any> {
    const userId = this.getCurrentUserId();
    
    if (!userId) {
      this.toastService.showError('Please login to add items to cart');
      return of(null);
    }

    // Get product details first
    return this.http.get<any>(`${EnvVariables.apiBaseUrl}/products/${productId}`).pipe(
      map(response => response.data || response),
      tap((product: Product) => {
        const currentCart = this.cartSubject.value;
        const existingItem = currentCart.items.find(item => item.ProductID === productId);

        if (existingItem) {
          // Update existing item
          const newQuantity = existingItem.Quantity + quantity;
          const newTotalPrice = (product.price || 0) * newQuantity;

          this.http.put(`${EnvVariables.apiBaseUrl}/cart/${existingItem.id}`, {
            ...existingItem,
            Quantity: newQuantity,
            TotalPrice: newTotalPrice
          }).subscribe(() => {
            this.loadCart();
            this.toastService.showSuccess('Item added to cart successfully!');
          });
        } else {
          // Create new cart item
          const newCartItem: Omit<CartItem, 'id'> = {
            ProductID: productId,
            Quantity: quantity,
            TotalPrice: (product.price || 0) * quantity,
            CartItemID: Date.now(),
            user_id: userId,
            Product: product
          };

          this.http.post(`${EnvVariables.apiBaseUrl}/cart`, newCartItem).subscribe(() => {
            this.loadCart();
            this.toastService.showSuccess('Item added to cart successfully!');
          });
        }
      }),
      catchError(error => {
        console.error('Error adding to cart:', error);
        this.toastService.showError('Failed to add item to cart');
        return of(null);
      })
    );
  }

  removeFromCart(cartItemId: string): Observable<any> {
    return this.http.delete(`${EnvVariables.apiBaseUrl}/cart/${cartItemId}`).pipe(
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
    if (newQuantity <= 0) {
      return this.removeFromCart(cartItemId);
    }

    const currentCart = this.cartSubject.value;
    const item = currentCart.items.find(item => item.id === cartItemId);

    if (!item) {
      return of(null);
    }

    const newTotalPrice = (item.Product?.price || 0) * newQuantity;

    return this.http.put(`${EnvVariables.apiBaseUrl}/cart/${cartItemId}`, {
      ...item,
      Quantity: newQuantity,
      TotalPrice: newTotalPrice
    }).pipe(
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
    const currentCart = this.cartSubject.value;
    const userItems = currentCart.items.filter(item => item.user_id === userId);

    if (userItems.length === 0) {
      return of(true);
    }

    const deleteRequests = userItems.map(item => 
      this.http.delete(`${EnvVariables.apiBaseUrl}/cart/${item.id}`)
    );

    return of(true).pipe(
      tap(() => {
        deleteRequests.forEach(req => req.subscribe());
        this.loadCart();
        this.toastService.showInfo('Cart cleared successfully');
      })
    );
  }

  getCurrentCart(): Cart {
    return this.cartSubject.value;
  }

  getCartItemCount(): Observable<number> {
    return this.cart$.pipe(map(cart => cart.items.length));
  }

  getCartTotal(): Observable<number> {
    return this.cart$.pipe(map(cart => cart.totalPrice));
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
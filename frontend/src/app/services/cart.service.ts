import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EnvVariables } from '../env/env-variables';
import { AuthService } from './auth.service';

export interface CartItem {
  id: number;
  productId: number;
  quantity: number;
  lineTotal: number;
}

export interface Cart {
  id: number;
  userId: number;
  total: number;
  items: CartItem[];
  totalItems: number;
  totalPrice: number;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private baseUrl = `${EnvVariables.cartServiceUrl}`;
  private cartSubject = new BehaviorSubject<Cart | null>(null);
  public cart$ = this.cartSubject.asObservable();

  constructor(private http: HttpClient, private authService: AuthService) {}

  private getUserId(): number {
    const user = this.authService.getCurrentUser();
    if (!user?.id) {
      throw new Error('User not logged in');
    }
    return user.id;
  }

  getCart(): Observable<Cart> {
    const userId = this.getUserId();
    return this.http.get<Cart>(`${this.baseUrl}/cart/${userId}`).pipe(
      tap(cart => this.cartSubject.next(cart))
    );
  }

  addToCart(productId: number, quantity: number): Observable<Cart> {
    const userId = this.getUserId();
    const request = { productId, quantity };
    return this.http.post<Cart>(`${this.baseUrl}/cart/${userId}/items`, request).pipe(
      tap(cart => this.cartSubject.next(cart))
    );
  }

  updateQuantity(itemId: number, quantity: number): Observable<Cart> {
    const userId = this.getUserId();
    const request = { quantity };
    return this.http.patch<Cart>(`${this.baseUrl}/cart/${userId}/items/${itemId}`, request).pipe(
      tap(cart => this.cartSubject.next(cart))
    );
  }

  removeFromCart(itemId: number): Observable<Cart> {
    const userId = this.getUserId();
    return this.http.delete<Cart>(`${this.baseUrl}/cart/${userId}/items/${itemId}`).pipe(
      tap(cart => this.cartSubject.next(cart))
    );
  }

  clearCart(): Observable<void> {
    const userId = this.getUserId();
    return this.http.delete<void>(`${this.baseUrl}/cart/${userId}`).pipe(
      tap(() => this.cartSubject.next(null))
    );
  }

  getCartItemCount(): Observable<number> {
    return this.cart$.pipe(
      map(cart => cart?.totalItems || 0)
    );
  }

  getCartTotal(): Observable<number> {
    return this.cart$.pipe(
      map(cart => cart?.totalPrice || 0)
    );
  }

  // Initialize cart when user logs in
  initializeCart(): void {
    if (this.authService.isLoggedIn()) {
      this.getCart().subscribe();
    }
  }

  // Clear cart when user logs out
  clearCartData(): void {
    this.cartSubject.next(null);
  }
}
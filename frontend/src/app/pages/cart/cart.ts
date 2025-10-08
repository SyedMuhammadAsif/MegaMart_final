import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { CartService } from '../../services/cart-service';
import { Cart as CartModel, CartItem } from '../../models/cart-items';

@Component({
  selector: 'app-cart',
  imports: [CommonModule, FormsModule],
  templateUrl: './cart.html',
  styleUrl: './cart.css',
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 9999;
    }
    .modal-content {
      background: white;
      border-radius: 8px;
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
      max-width: 400px;
      width: 90%;
    }
    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem;
      border-bottom: 1px solid #dee2e6;
    }
    .modal-body {
      padding: 1rem;
    }
    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
      padding: 1rem;
      border-top: 1px solid #dee2e6;
    }
    .btn-close {
      background: none;
      border: none;
      font-size: 1.5rem;
      cursor: pointer;
    }
  `]
})
export class CartComponent implements OnInit, OnDestroy {
  cart: CartModel = { items: [], totalItems: 0, totalPrice: 0 };
  private subscription = new Subscription();
  showClearModal = false;
  successMessage = '';

  constructor(
    private cartService: CartService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const cartSub = this.cartService.cart$.subscribe({
      next: (cart) => {
        this.cart = cart;
        console.log('Cart updated:', cart);
      },
      error: (error) => {
        console.error('Error loading cart:', error);
      }
    });
    this.subscription.add(cartSub);
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  updateQuantity(item: CartItem, newQuantity: number): void {
    if (newQuantity < 1) {
      this.removeItem(item);
      return;
    }

    if (!item.id) {
      console.error('Cart item missing ID');
      return;
    }

    this.cartService.updateQuantity(item.id, newQuantity).subscribe({
      next: () => {
        console.log('Quantity updated successfully');
      },
      error: (error) => {
        console.error('Error updating quantity:', error);
      }
    });
  }

  removeItem(item: CartItem): void {
    if (!item.id) {
      console.error('Cart item missing ID');
      return;
    }

    this.cartService.removeFromCart(item.id).subscribe({
      next: () => {
        console.log('Item removed successfully');
        this.showSuccessMessage('Item removed from cart');
      },
      error: (error) => {
        console.error('Error removing item:', error);
      }
    });
  }

  showSuccessMessage(message: string): void {
    this.successMessage = message;
    setTimeout(() => {
      this.successMessage = '';
    }, 3000);
  }

  openClearModal(): void {
    this.showClearModal = true;
  }

  closeClearModal(): void {
    this.showClearModal = false;
  }

  clearCart(): void {
    this.cartService.clearCart().subscribe({
      next: () => {
        console.log('Cart cleared successfully');
        this.showClearModal = false;
        this.showSuccessMessage('Cart cleared successfully');
      },
      error: (error) => {
        console.error('Error clearing cart:', error);
        this.showClearModal = false;
      }
    });
  }

  continueShopping(): void {
    this.router.navigate(['/']);
  }

  proceedToCheckout(): void {
    if (this.cart.items.length === 0) {
      return;
    }
    this.router.navigate(['/checkout/address']);
  }

  goToOrderTracking(): void {
    this.router.navigate(['/orders']);
  }

  incrementQuantity(item: CartItem): void {
    const newQuantity = item.Quantity + 1;
    this.updateQuantity(item, newQuantity);
  }

  decrementQuantity(item: CartItem): void {
    const newQuantity = item.Quantity - 1;
    this.updateQuantity(item, newQuantity);
  }

  getProductImage(item: CartItem): string {
    return item.Product?.images?.[0] || 'assets/placeholder.jpg';
  }

  formatPrice(price: number): string {
    return `$${price.toFixed(2)}`;
  }
}
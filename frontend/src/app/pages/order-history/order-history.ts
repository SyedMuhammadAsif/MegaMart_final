import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Order } from '../../models/payment';
import { AuthService } from '../../services/auth.service';
import { EnvVariables } from '../../env/env-variables';

import { forkJoin, of, Observable, firstValueFrom } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-history.html',
  styleUrl: './order-history.css'
})
export class OrderHistoryComponent implements OnInit {
  orders: Order[] = [];
  isLoading = true;
  error: string = '';

  // Cancel order modal
  showCancelModal = false;
  selectedOrderForCancellation: Order | null = null;
  cancellationReason = '';
  customReason = '';

  constructor(
    private router: Router,
    private http: HttpClient,
    private userAuth: AuthService,

  ) {}

  ngOnInit(): void {
    // Check if user is logged in
    if (!this.userAuth.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    
    this.loadOrders();
  }

  private loadOrders(): void {
    // Show loading state
    this.isLoading = true;

    // Get current user from AuthService
    const currentUser = this.userAuth.getCurrentUser();
    if (!currentUser?.email) {
      console.log('No current user or email found, redirecting to login');
      this.router.navigate(['/login']);
      return;
    }
    
    // Get user-admin service user ID by email from database
    firstValueFrom(this.http.get<any[]>(`${EnvVariables.userServiceUrl}/users`))
      .then(users => {
        const matchedUser = users?.find(u => u.email === currentUser.email);
        const userId = matchedUser?.id;
        
        console.log('🔍 Current user from AuthService:', currentUser);
        console.log('🔍 Matched user-admin service user:', matchedUser);
        console.log('🔍 Using userId for orders:', userId);
        
        if (!userId) {
          console.log('No matching user found in user-admin service');
          this.error = 'User not found';
          this.isLoading = false;
          return;
        }
        
        console.log('Fetching orders from:', `${EnvVariables.orderServiceUrl}/orders/user/${userId}`);
        return firstValueFrom(this.http.get<any>(`${EnvVariables.orderServiceUrl}/orders/user/${userId}`));
      })
      .then((response) => {
        if (!response) return [];
        console.log('Raw response from order service:', response);
        const orders = response?.content || response || [];
        console.log('User orders:', orders);
        
        let filteredOrders = (orders || []).filter((order: any) => {
          return order.visibleToCustomer !== false;
        });
        
        if (filteredOrders.length === 0) {
          console.log('No orders found for user');
        }
        
        const orderDetailsPromises = filteredOrders.map((order: any) => {
          return firstValueFrom(this.http.get<any>(`${EnvVariables.orderServiceUrl}/orders/${order.id}`))
            .then((detailedOrder) => {
              const paymentType = 
                detailedOrder.paymentMethod?.type ||
                detailedOrder.paymentType ||
                detailedOrder.payment_method ||
                detailedOrder.payment_type ||
                detailedOrder.paymentMethodType ||
                'unknown';
              
              return {
                ...order,
                paymentMethod: {
                  type: paymentType.toString().toLowerCase(),
                  cardNumber: detailedOrder.paymentMethod?.cardNumber || '',
                  cardholderName: detailedOrder.paymentMethod?.cardholderName || '',
                  upiId: detailedOrder.paymentMethod?.upiId || ''
                },
                paymentStatus: detailedOrder.paymentStatus?.toLowerCase() || order.paymentStatus
              };
            })
            .catch((error) => {
              console.warn('Failed to fetch details for order:', order.id, error);
              return {
                ...order,
                paymentMethod: {
                  type: 'unknown',
                  cardNumber: '',
                  cardholderName: '',
                  upiId: ''
                }
              };
            });
        });
        
        return Promise.all(orderDetailsPromises);
      })
      .then((detailedOrders) => {
        if (detailedOrders) {
          this.orders = detailedOrders;
          this.orders.sort((a, b) => {
            const dateA = a.orderDate ? new Date(a.orderDate).getTime() : 0;
            const dateB = b.orderDate ? new Date(b.orderDate).getTime() : 0;
            return dateB - dateA;
          });
        }
        this.isLoading = false;
      })
      .catch((error) => {
        console.error('Error loading orders:', error);
        this.error = 'Failed to load order history';
        this.isLoading = false;
      });
  }

  viewOrderDetails(orderId: any): void {
    if (orderId) {
      this.router.navigate(['/order-tracking', orderId.toString()]);
    }
  }

  formatPrice(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'pending': 'warning',
      'confirmed': 'info',
      'processing': 'primary',
      'shipped': 'success',
      'delivered': 'success',
      'cancelled': 'danger'
    };
    return colors[status] || 'secondary';
  }

  getStatusText(status: string): string {
    if (!status) return 'Unknown';
    return status.charAt(0).toUpperCase() + status.slice(1);
  }

  continueShopping(): void {
    this.router.navigate(['/']);
  }

  openCancelModal(order: Order): void {
    this.selectedOrderForCancellation = order;
    this.cancellationReason = '';
    this.customReason = '';
    this.showCancelModal = true;
  }

  cancelOrder(): void {
    if (!this.selectedOrderForCancellation || !this.selectedOrderForCancellation.id) return;

    const finalReason = this.cancellationReason === 'Other' ? this.customReason : this.cancellationReason;
    
    if (!finalReason) {
      alert('Please provide a reason for cancellation');
      return;
    }

    const updatedOrder: Order = {
      ...this.selectedOrderForCancellation,
      orderStatus: 'cancelled' as const,
      cancelledAt: new Date().toISOString(),
      cancelledBy: 'customer',
      cancellationReason: finalReason,
      visibleToAdmin: true,
      visibleToCustomer: true
    };

    this.http.put(`${EnvVariables.orderServiceUrl}/orders/${this.selectedOrderForCancellation.id}`, updatedOrder).subscribe({
      next: () => {
        const isCOD = (this.selectedOrderForCancellation?.paymentMethod?.type || '').toLowerCase?.() === 'cod';
        
        if (!isCOD) {
          alert(`Your order has been cancelled. A refund of ${this.formatPrice(this.selectedOrderForCancellation?.total || 0)} will be processed within 7 business days.`);
        } else {
          alert('Your order has been cancelled. As the payment method was Cash on Delivery, no refund is applicable.');
        }
        
        const idx = this.orders.findIndex(o => o.id === this.selectedOrderForCancellation?.id);
        if (idx !== -1) {
          this.orders[idx] = updatedOrder;
        }
        
        this.closeCancelModal();
      },
      error: (error) => {
        console.error('Error cancelling order:', error);
        alert('Failed to cancel order. Please try again.');
      }
    });
  }

  closeCancelModal(): void {
    this.showCancelModal = false;
    this.selectedOrderForCancellation = null;
    this.cancellationReason = '';
    this.customReason = '';
  }

  canCancelOrder(order: Order): boolean {
    const cancellableStatuses = ['confirmed', 'processing'];
    return cancellableStatuses.includes(order.orderStatus);
  }
}
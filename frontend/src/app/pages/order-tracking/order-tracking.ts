import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Order, OrderTracking, PaymentType } from '../../models/payment';
import { OrderProcessingService } from '../../services/order-processing.service';
import { PaymentToastComponent } from '../payment-toast/payment-toast';
import { EnvVariables } from '../../env/env-variables';
@Component({
  selector: 'app-order-tracking',
  standalone: true,
  imports: [CommonModule, PaymentToastComponent],
  templateUrl: './order-tracking.html',
  styleUrl: './order-tracking.css',
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
      max-width: 500px;
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
export class OrderTrackingComponent implements OnInit {
  order: Order | null = null;
  orderNumber: string = '';
  isLoading = true;
  error: string = '';
  trackingHistory: OrderTracking[] = [];
  showTrackingHistory = false;
  successMessage = '';
  showCancelModal = false;

  trackingSteps = [
    { id: 'confirmed', title: 'Order Confirmed', description: 'We have received your order', completed: false, current: false },
    { id: 'processing', title: 'Processing', description: 'Your order is being prepared', completed: false, current: false },
    { id: 'shipped', title: 'Shipped', description: 'Your order is on its way', completed: false, current: false },
    { id: 'delivered', title: 'Delivered', description: 'Your order has been delivered', completed: false, current: false }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private orderProcessing: OrderProcessingService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.orderNumber = params['orderNumber'];
      if (this.orderNumber) {
        this.loadOrder();
      } else {
        this.error = 'No order number provided';
        this.isLoading = false;
      }
    });
  }

  showSuccessMessage(message: string): void {
    this.successMessage = message;
    setTimeout(() => {
      this.successMessage = '';
    }, 3000);
  }

  openCancelModal(): void {
    this.showCancelModal = true;
  }

  closeCancelModal(): void {
    this.showCancelModal = false;
  }

  private loadOrder(): void {
    this.isLoading = true;

    // Only use microservice - no fallback to JSON server
    this.http.get<any>(`${EnvVariables.orderServiceUrl}/orders/${this.orderNumber}`).subscribe({
      next: (orderResponse) => {
        console.log('Raw backend response:', JSON.stringify(orderResponse, null, 2));
        this.order = {
          id: orderResponse.id?.toString(),
          orderNumber: `ORD-${orderResponse.id}`,
          orderStatus: orderResponse.orderStatus?.toLowerCase(),
          total: orderResponse.total,
          orderDate: orderResponse.orderDate,
          customerInfo: { 
            fullName: orderResponse.shippingAddress?.fullName || 'Customer', 
            email: orderResponse.shippingAddress?.email || '', 
            phone: orderResponse.shippingAddress?.phone || '' 
          },
          shippingAddress: orderResponse.shippingAddress || {},
          items: orderResponse.orderItems || [],
          subtotal: orderResponse.subtotal || (orderResponse.total * 0.9),
          tax: orderResponse.tax || (orderResponse.total * 0.1),
          shipping: orderResponse.shipping || 0,
          paymentMethod: {
            type: (orderResponse.paymentMethod?.type?.toLowerCase() as PaymentType) || 'cod',
            cardNumber: orderResponse.paymentMethod?.cardNumber || '',
            expiryMonth: orderResponse.paymentMethod?.expiryMonth || '',
            expiryYear: orderResponse.paymentMethod?.expiryYear || '',
            cvv: '',
            cardholderName: orderResponse.paymentMethod?.cardholderName || '',
            upiId: orderResponse.paymentMethod?.upiId || ''
          },
          paymentStatus: orderResponse.paymentStatus?.toLowerCase() || 'unknown',
          estimatedDelivery: orderResponse.estimatedDelivery || '3-5 business days'
        };
        this.updateTrackingSteps();
        this.loadTrackingHistory();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading order:', error);
        this.error = 'Order not found';
        this.isLoading = false;
      }
    });
  }

  private updateTrackingSteps(): void {
    if (!this.order) return;

    const statusMap: { [key: string]: number } = {
      'pending': 0,
      'confirmed': 1,
      'processing': 2,
      'shipped': 3,
      'delivered': 4
    };

    const currentStatusIndex = statusMap[this.order.orderStatus] || 0;
    
    this.trackingSteps.forEach((step, index) => {
      step.current = false;
      step.completed = index < currentStatusIndex;
      
      if (index === currentStatusIndex) {
        step.current = true;
        step.description = this.getCurrentStepDescription(step.id);
      }
      
      if (this.order?.orderStatus === 'delivered') {
        step.completed = true;
        step.current = false;
      }
    });
  }

  private loadTrackingHistory(): void {
    if (!this.order?.id) return;
    
    this.http.get<any>(`${EnvVariables.orderServiceUrl}/orders/${this.order.id}/tracking`).subscribe({
      next: (response) => {
        this.trackingHistory = response.trackingHistory || [];
      },
      error: (error) => {
        console.error('Error loading tracking history:', error);
      }
    });
  }

  toggleTrackingHistory(): void {
    this.showTrackingHistory = !this.showTrackingHistory;
  }

  continueShopping(): void {
    this.router.navigate(['/']);
  }

  cancelOrder(): void {
    if (!this.order || !this.order.id) return;
    
    this.http.put(`${EnvVariables.orderServiceUrl}/orders/${this.order.id}/cancel`, {}).subscribe({
      next: (response) => {
        console.log('Order cancelled successfully:', response);
        this.showCancelModal = false;
        this.showSuccessMessage('Order cancelled successfully');
        this.loadOrder();
      },
      error: (error) => {
        console.error('Error cancelling order:', error);
        this.error = 'Failed to cancel order. Please try again.';
        this.showCancelModal = false;
      }
    });
  }

  canCancelOrder(): boolean {
    if (!this.order) return false;
    return ['pending', 'confirmed', 'processing'].includes(this.order.orderStatus || '');
  }

  formatPrice(price: number): string {
    if (!price || isNaN(price)) return '$0.00';
    return `$${price.toFixed(2)}`;
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return 'Invalid Date';
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
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

  getPaymentStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'pending': 'warning',
      'completed': 'success',
      'failed': 'danger',
      'refunded': 'info'
    };
    return colors[status] || 'secondary';
  }

  private getCurrentStepDescription(stepId: string): string {
    const descriptions: { [key: string]: string } = {
      'confirmed': 'Your order is confirmed and being processed',
      'processing': 'Your order is currently being prepared for shipping',
      'shipped': 'Your order is on its way to you',
      'delivered': 'Your order has been successfully delivered'
    };
    return descriptions[stepId] || 'Processing your order';
  }
}
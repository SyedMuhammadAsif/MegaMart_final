import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { Order, OrderTracking, ProcessingLocation } from '../models/payment';
import { AdminAuthService } from './admin-auth.service';
import { EnvVariables } from '../env/env-variables';

interface OrderServiceResponse {
  success: boolean;
  data: any;
  message: string;
}

interface CreateOrderFromCartRequest {
  address: {
    fullName: string;
    addressLine1: string;
    city: string;
    state: string;
    postalCode: string;
    country: string;
    phone: string;
  };
  paymentMethod: {
    type: string;
    cardNumber?: string;
    expiryMonth?: string;
    expiryYear?: string;
    cvv?: string;
    cardholderName?: string;
  };
}
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class OrderProcessingService {
  private orderUpdateSubject = new BehaviorSubject<Order | null>(null);
  public orderUpdate$ = this.orderUpdateSubject.asObservable();

  private readonly ORDER_SERVICE_URL = `${EnvVariables.orderServiceUrl}/orders`;
  private readonly PAYMENT_SERVICE_URL = `${EnvVariables.orderServiceUrl}/payments`;

  constructor(
    private http: HttpClient,
    private adminAuth: AdminAuthService
  ) {}

  /**
   * Create order from cart (microservice integration)
   */
  createOrderFromCart(userId: string, orderData: CreateOrderFromCartRequest): Observable<any> {
    const encodedUserId = encodeURIComponent(userId);
    const url = `${this.ORDER_SERVICE_URL}/from-cart/${encodedUserId}`;
    console.log('Order service URL:', url);
    return this.http.post(url, orderData);
  }

  /**
   * Get all orders (microservice)
   */
  getAllOrders(page: number = 0, size: number = 10): Observable<any> {
    return this.http.get(`${this.ORDER_SERVICE_URL}?page=${page}&size=${size}`);
  }

  /**
   * Get user orders (microservice)
   */
  getUserOrders(userId: string, page: number = 0, size: number = 10): Observable<any> {
    return this.http.get(`${this.ORDER_SERVICE_URL}/user/${userId}?page=${page}&size=${size}`);
  }

  /**
   * Get order by ID (microservice)
   */
  getOrderById(orderId: number): Observable<any> {
    return this.http.get(`${this.ORDER_SERVICE_URL}/${orderId}`);
  }

  /**
   * Update order status (microservice)
   */
  updateOrderStatusMicroservice(orderId: number, status: string): Observable<any> {
    return this.http.put(`${this.ORDER_SERVICE_URL}/${orderId}/status?status=${status}`, {});
  }

  /**
   * Cancel order (microservice)
   */
  cancelOrder(orderId: number): Observable<any> {
    return this.http.put(`${this.ORDER_SERVICE_URL}/${orderId}/cancel`, {});
  }

  /**
   * Get order tracking (microservice)
   */
  getOrderTrackingMicroservice(orderId: number): Observable<any> {
    return this.http.get(`${this.ORDER_SERVICE_URL}/${orderId}/tracking`);
  }

  /**
   * Process payment (microservice)
   */
  processPayment(paymentData: any): Observable<any> {
    return this.http.post(`${this.PAYMENT_SERVICE_URL}/process`, paymentData);
  }

  /**
   * Get all processing locations from database
   */
  getProcessingLocations(): Observable<ProcessingLocation[]> {
    return this.http.get<ProcessingLocation[]>(`${EnvVariables.orderServiceUrl}/processing-locations`);
  }

  /**
   * Update order status with location tracking
   */
  updateOrderStatus(
    orderId: string, 
    newStatus: Order['orderStatus'], 
    locationId?: string,
    notes?: string
  ): Observable<Order> {
    const currentAdmin = this.adminAuth.getCurrentAdmin();
    const timestamp = new Date().toISOString();

    // Get current order to update
    return new Observable(observer => {
      this.http.get<Order>(`${this.ORDER_SERVICE_URL}/${orderId}`).subscribe({
        next: (order) => {
          // Get location details if provided
          if (locationId) {
            this.getProcessingLocations().subscribe({
              next: (locations) => {
                const location = locations.find(loc => loc.id === locationId);
                this.updateOrderWithLocation(order, newStatus, location, notes, currentAdmin, timestamp, observer);
              },
              error: (error) => {
                console.error('Error fetching locations:', error);
                this.updateOrderWithLocation(order, newStatus, undefined, notes, currentAdmin, timestamp, observer);
              }
            });
          } else {
            this.updateOrderWithLocation(order, newStatus, undefined, notes, currentAdmin, timestamp, observer);
          }
        },
        error: (error) => {
          console.error('Error fetching order for update:', error);
          observer.error(error);
        }
      });
    });
  }

  /**
   * Helper method to update order with location
   */
  private updateOrderWithLocation(
    order: Order,
    newStatus: Order['orderStatus'],
    location: ProcessingLocation | undefined,
    notes: string | undefined,
    currentAdmin: any,
    timestamp: string,
    observer: any
  ): void {
    // Create tracking entry
    const trackingEntry: OrderTracking = {
      status: newStatus,
      location: location ? `${location.name}, ${location.city}` : undefined,
      description: this.getStatusDescription(newStatus, location),
      timestamp: timestamp,
      updatedBy: currentAdmin?.name || 'System'
    };

    // Update order with new status and tracking info
    const updatedOrder: Order = {
      ...order,
      orderStatus: newStatus,
      currentLocation: location,
      processingNotes: notes || order.processingNotes,
      lastUpdated: timestamp,
      updatedBy: currentAdmin?.name || 'System',
      trackingHistory: [
        ...(order.trackingHistory || []),
        trackingEntry
      ]
    };

    // Save updated order
    this.http.put<Order>(`${this.ORDER_SERVICE_URL}/${order.id}`, updatedOrder).subscribe({
      next: (savedOrder) => {
        console.log('Order status updated:', savedOrder.orderNumber, newStatus);
        this.orderUpdateSubject.next(savedOrder);
        observer.next(savedOrder);
        observer.complete();
      },
      error: (error) => {
        console.error('Error saving order update:', error);
        observer.error(error);
      }
    });
  }

  /**
   * Get status description based on status and location
   */
  private getStatusDescription(status: Order['orderStatus'], location?: ProcessingLocation): string {
    const statusDescriptions: { [key: string]: string } = {
      'pending': 'Order received and pending confirmation',
      'confirmed': 'Order confirmed and payment verified',
      'processing': location ? 
        `Order is being processed at ${location.name}` : 
        'Order is being processed',
      'shipped': location ? 
        `Order shipped from ${location.name}` : 
        'Order has been shipped',
      'delivered': 'Order has been delivered to customer',
      'cancelled': 'Order has been cancelled'
    };

    return statusDescriptions[status] || 'Status updated';
  }

  /**
   * Get order tracking history
   */
  getOrderTracking(orderId: string): Observable<OrderTracking[]> {
    return new Observable(observer => {
      this.http.get<Order>(`${this.ORDER_SERVICE_URL}/${orderId}`).subscribe({
        next: (order) => {
          observer.next(order.trackingHistory || []);
          observer.complete();
        },
        error: (error) => {
          console.error('Error fetching order tracking:', error);
          observer.error(error);
        }
      });
    });
  }

  /**
   * Add processing notes to order
   */
  addProcessingNotes(orderId: string, notes: string): Observable<Order> {
    const currentAdmin = this.adminAuth.getCurrentAdmin();
    const timestamp = new Date().toISOString();

    return new Observable(observer => {
      this.http.get<Order>(`${this.ORDER_SERVICE_URL}/${orderId}`).subscribe({
        next: (order) => {
          const updatedOrder: Order = {
            ...order,
            processingNotes: notes,
            lastUpdated: timestamp,
            updatedBy: currentAdmin?.name || 'System'
          };

          this.http.put<Order>(`${this.ORDER_SERVICE_URL}/${orderId}`, updatedOrder).subscribe({
            next: (savedOrder) => {
              console.log('Processing notes added to order:', savedOrder.orderNumber);
              this.orderUpdateSubject.next(savedOrder);
              observer.next(savedOrder);
              observer.complete();
            },
            error: (error) => {
              console.error('Error saving processing notes:', error);
              observer.error(error);
            }
          });
        },
        error: (error) => {
          console.error('Error fetching order for notes update:', error);
          observer.error(error);
        }
      });
    });
  }

  /**
   * Get orders by status
   */
  getOrdersByStatus(status: Order['orderStatus']): Observable<Order[]> {
    return this.http.get<any>(`${this.ORDER_SERVICE_URL}?page=0&size=1000`).pipe(
      map(response => (response.content || []).filter((order: any) => order.orderStatus === status))
    );
  }

  /**
   * Get orders that need attention (pending, processing)
   */
  getOrdersNeedingAttention(): Observable<Order[]> {
    return this.http.get<any>(`${this.ORDER_SERVICE_URL}?page=0&size=1000`).pipe(
      map(response => (response.content || []).filter((order: any) => 
        ['pending', 'processing'].includes(order.orderStatus)
      ))
    );
  }

  /**
   * Get order statistics
   */
  getOrderStatistics(): Observable<{
    total: number;
    pending: number;
    processing: number;
    shipped: number;
    delivered: number;
    cancelled: number;
  }> {
    return this.http.get<any>(`${this.ORDER_SERVICE_URL}?page=0&size=1000`).pipe(
      map(response => response.content || []),
      map(orders => {
        const stats = {
          total: orders.length,
          pending: 0,
          processing: 0,
          shipped: 0,
          delivered: 0,
          cancelled: 0
        };

        (orders as any[]).forEach((order: any) => {
          switch (order.orderStatus) {
            case 'pending': stats.pending++; break;
            case 'processing': stats.processing++; break;
            case 'shipped': stats.shipped++; break;
            case 'delivered': stats.delivered++; break;
            case 'cancelled': stats.cancelled++; break;
          }
        });

        return stats;
      })
    );
  }
} 
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EnvVariables } from '../env/env-variables';
import { AuthService } from './auth.service';

export interface OrderItem {
  productId: number;
  quantity: number;
  lineTotal: number;
}

export interface OrderAddress {
  fullName: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  phone: string;
  isDefault?: boolean;
}

export interface OrderPaymentMethod {
  type: string;
  cardNumber?: string;
  cardholderName?: string;
  expiryMonth?: string;
  expiryYear?: string;
  cvv?: string;
  upiId?: string;
  isDefault?: boolean;
}

export interface CreateOrderRequest {
  userId: number;
  total: number;
  paymentType: string;
  items: OrderItem[];
  addressId?: number;
  newAddress?: OrderAddress;
  paymentMethodId?: number;
  newPaymentMethod?: OrderPaymentMethod;
}

export interface Order {
  id: number;
  userId: number;
  total: number;
  paymentType: string;
  orderStatus: string;
  paymentStatus: string;
  orderDate: string;
  createdAt: string;
  updatedAt: string;
  shippingAddress: any;
  orderItems: any[];
  payment: any;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private baseUrl = `${EnvVariables.orderServiceUrl}`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  private getUserId(): number {
    const user = this.authService.getCurrentUser();
    if (!user?.id) {
      throw new Error('User not logged in');
    }
    return user.id;
  }

  createOrder(orderData: Omit<CreateOrderRequest, 'userId'>): Observable<Order> {
    const userId = this.getUserId();
    const request: CreateOrderRequest = { ...orderData, userId };
    return this.http.post<Order>(`${this.baseUrl}/orders`, request);
  }

  getOrders(): Observable<Order[]> {
    const userId = this.getUserId();
    return this.http.get<Order[]>(`${this.baseUrl}/orders/user/${userId}`);
  }

  getOrder(orderId: number): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/orders/${orderId}`);
  }

  cancelOrder(orderId: number): Observable<Order> {
    return this.http.put<Order>(`${this.baseUrl}/orders/${orderId}/cancel`, {});
  }

  trackOrder(orderId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/orders/${orderId}/tracking`);
  }

  // Admin methods
  getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.baseUrl}/orders`);
  }

  updateOrderStatus(orderId: number, status: string): Observable<Order> {
    return this.http.put<Order>(`${this.baseUrl}/orders/${orderId}/status`, { status });
  }

  processPayment(orderId: number, paymentData: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/payments/${orderId}/process`, paymentData);
  }

  refundPayment(orderId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/payments/${orderId}/refund`, {});
  }
}
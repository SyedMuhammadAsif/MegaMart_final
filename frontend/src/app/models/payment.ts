import { Address } from './address';
import { CartItem } from './cart-items';

export type PaymentType = 'card' | 'upi' | 'cod';

export interface PaymentMethod {
  type: PaymentType;
  cardNumber: string;
  expiryMonth: string;
  expiryYear: string;
  cvv: string;
  cardholderName: string;
  upiId?: string;
}

export interface CustomerInfo {
  fullName: string;
  email: string;
  phone: string;
}

export interface OrderTracking {
  status: 'pending' | 'confirmed' | 'processing' | 'shipped' | 'delivered' | 'cancelled';
  location?: string;
  description: string;
  timestamp: string;
  updatedBy?: string;
  processingNotes?: string;
  createdAt?: string;
}

export interface ProcessingLocation {
  id: string;
  name: string;
  address: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  type: 'warehouse' | 'processing_center' | 'shipping_center';
}

export interface Order {
  id?: string;
  orderNumber?: string;
  userId?: string; 
  customerInfo: CustomerInfo;
  shippingAddress: any; 
  items: any[]; 
  subtotal: number;
  tax: number;
  shipping: number;
  total: number;
  paymentMethod: PaymentMethod;
  orderStatus: 'pending' | 'confirmed' | 'processing' | 'shipped' | 'delivered' | 'cancelled';
  paymentStatus: 'pending' | 'completed' | 'failed' | 'refunded';
  orderDate: string;
  estimatedDelivery: string;
  trackingHistory?: OrderTracking[];
  currentLocation?: ProcessingLocation;
  processingNotes?: string;
  lastUpdated?: string;
  updatedBy?: string;
  cancellationReason?: string;
  cancelledAt?: string;
  cancelledBy?: 'customer' | 'admin';
  visibleToAdmin?: boolean;
  visibleToCustomer?: boolean;
  isArchived?: boolean;
  archivedAt?: string;
  archivedReason?: string;
  archivedBy?: 'admin' | 'customer' | 'system';
  autoDeleteDate?: string;
}

export interface OrderResponse {
  success: boolean;
  order?: Order;
  trackingNumber?: string;
  message?: string;
} 

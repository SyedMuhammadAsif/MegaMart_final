import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, of, forkJoin } from 'rxjs';
import { tap, catchError, switchMap } from 'rxjs/operators';
import { AdminAuthService } from '../../../services/admin-auth.service';
import { OrderProcessingService } from '../../../services/order-processing.service';
import { EnvVariables } from '../../../env/env-variables';

interface Order {
  id?: string;
  orderNumber?: string;
  customerInfo: {
    fullName: string;
    email: string;
    phone: string;
  };
  shippingAddress: {
    fullName: string;
    email: string;
    phone: string;
    addressLine1: string;
    addressLine2?: string;
    city: string;
    state: string;
    postalCode: string;
    country: string;
  };
  items: OrderItem[];
  total: number;
  paymentMethod: {
    type: string;
    cardNumber: string;
    expiryMonth: string;
    expiryYear: string;
    cvv: string;
    cardholderName: string;
    upiId: string;
  };
  orderStatus: string;
  paymentStatus: string;
  orderDate: string;
  estimatedDelivery: string;
  // Enhanced tracking information
  trackingHistory?: any[];
  currentLocation?: any;
  processingNotes?: string;
  lastUpdated?: string;
  updatedBy?: string;
  // Archiving system
  isArchived?: boolean;
  archivedAt?: string;
  archivedReason?: string;
  archivedBy?: 'admin' | 'customer' | 'system';
  autoDeleteDate?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  visibleToAdmin?: boolean;
  visibleToCustomer?: boolean;
}

interface OrderItem {
  CartItemID: number;
  ProductID: number;
  Quantity: number;
  TotalPrice: number;
  id: string;
  Product: {
    id: string;
    title: string;
    price: number;
    thumbnail?: string;
    category: string;
    brand: string;
  };
}

interface OrderFilters {
  status: string;
  paymentStatus: string;
  dateFrom: string;
  dateTo: string;
  customer: string;
}

@Component({
  selector: 'app-admin-orders',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-orders.html',
  styleUrls: ['./admin-orders.css', '../admin-global.css']
})
export class AdminOrdersComponent implements OnInit {
  // Orders data
  allOrders: Order[] = [];
  filteredOrders: Order[] = [];
  
  // Pagination
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 0;
  paginatedOrders: Order[] = [];
  
  // Filters
  filters: OrderFilters = {
    status: '',
    paymentStatus: '',
    dateFrom: '',
    dateTo: '',
    customer: ''
  };
  
  // Available filter options
  orderStatuses = ['confirmed', 'processing', 'shipped', 'delivered', 'cancelled'];
  paymentStatuses = ['completed', 'pending', 'failed', 'refunded'];
  
  // Sorting
  sortField = 'orderDate';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Loading states
  isLoading = false;
  isUpdating = false;
  
  // Order details modal
  showOrderDetails = false;
  selectedOrder: Order | null = null;
  
  // Order processing
  showProcessingModal = false;
  selectedOrderForProcessing: Order | null = null;
  newStatus = '';
  selectedLocation = '';
  processingNotes = '';
  processingLocations: any[] = [];

  // Remove order modal
  showRemoveModal = false;
  selectedOrderForRemoval: Order | null = null;
  removalReason = '';
  sendNotification = true;
  successNotification = '';
  
  showSuccessNotification(message: string) {
    this.successNotification = message;
    setTimeout(() => {
      this.successNotification = '';
    }, 3000);
  }

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    public adminAuth: AdminAuthService,
    private orderProcessing: OrderProcessingService
  ) {}

  ngOnInit(): void {
    this.loadOrders();
    this.loadProcessingLocations();
    
    // Check for customer filter from query params
    this.route.queryParams.subscribe(params => {
      if (params['customer']) {
        this.filters.customer = params['customer'];
        this.applyFilters();
      }
    });
  }

  /**
   * Load processing locations
   */
  loadProcessingLocations(): void {
    this.http.get<any[]>(`${EnvVariables.orderServiceUrl}/processing-locations`).subscribe({
      next: (locations) => {
        this.processingLocations = locations;
        console.log('Processing locations loaded:', locations);
      },
      error: (error) => {
        console.error('Error loading processing locations:', error);
        // Add fallback locations for testing
        this.processingLocations = [
          { id: 1, name: 'Main Warehouse', city: 'New York' },
          { id: 2, name: 'West Coast Center', city: 'Los Angeles' },
          { id: 3, name: 'Central Hub', city: 'Chicago' }
        ];
      }
    });
  }

  /**
   * Load all orders from the database
   */
  loadOrders(): void {
    this.isLoading = true;
    
    console.log('Making request to:', `${EnvVariables.orderServiceUrl}/orders`);
    console.log('Admin token:', this.adminAuth.getToken() ? 'Present' : 'Missing');
    
    this.http.get<any>(`${EnvVariables.orderServiceUrl}/orders?page=0&size=1000`).subscribe({
      next: (response) => {
        console.log('=== ORDER SERVICE RESPONSE ===');
        console.log('Full response:', response);
        console.log('Response type:', typeof response);
        console.log('Response keys:', Object.keys(response || {}));
        
        const orders = response.content || response || [];
        console.log('Orders extracted:', orders);
        console.log('Orders length:', orders.length);
        
        if (orders.length > 0) {
          console.log('=== FIRST ORDER DETAILS ===');
          console.log('First order:', JSON.stringify(orders[0], null, 2));
          console.log('First order keys:', Object.keys(orders[0]));
          
          const firstOrder = orders[0];
          console.log('=== ADDRESS FIELD CHECK ===');
          console.log('shippingFullName:', firstOrder.shippingFullName);
          console.log('shipping_full_name:', firstOrder.shipping_full_name);
          console.log('shippingAddressLine1:', firstOrder.shippingAddressLine1);
          console.log('shipping_address_line1:', firstOrder.shipping_address_line1);
          console.log('shippingCity:', firstOrder.shippingCity);
          console.log('shipping_city:', firstOrder.shipping_city);
          console.log('================================');
        }
        this.processOrdersData(orders);
      },
      error: (error) => {
        console.error('Error loading orders:', error);
        console.error('Error details:', error.status, error.statusText, error.message);
        // Fallback to empty orders array
        this.allOrders = [];
        this.filteredOrders = [];
        this.paginatedOrders = [];
        this.isLoading = false;
      }
    });
  }

  private processOrdersData(orders: any[]): void {
    console.log('processOrdersData called with orders:', orders);
    if (orders.length > 0) {
      console.log('Sample order structure:', JSON.stringify(orders[0], null, 2));
    }
    
    // Convert microservice order format to admin format and fetch user/address data
    const orderPromises = orders.map(order => {
      console.log('=== RAW ORDER DATA ===');
      console.log('Order ID:', order.id);
      console.log('Full order object:', order);
      console.log('shippingAddress from API:', order.shippingAddress);
      console.log('======================');
      
      const baseOrder = {
        id: order.id?.toString(),
        orderNumber: `ORD-${order.id}`,
        customerInfo: {
          fullName: 'Loading...',
          email: 'Loading...',
          phone: 'Loading...'
        },
        items: order.orderItems?.map((item: any) => ({
          CartItemID: item.id,
          ProductID: item.productId,
          Quantity: item.quantity,
          TotalPrice: item.lineTotal,
          id: item.id?.toString(),
          Product: {
            id: item.productId?.toString(),
            title: `Product ${item.productId}`,
            price: item.lineTotal / item.quantity,
            category: 'General',
            brand: 'Unknown'
          }
        })) || [],
        total: order.total,
        paymentMethod: {
          type: order.paymentType?.toLowerCase() || 'unknown',
          cardNumber: '',
          cardholderName: '',
          expiryMonth: '',
          expiryYear: '',
          cvv: '',
          upiId: ''
        },
        orderStatus: order.orderStatus?.toLowerCase() || 'pending',
        paymentStatus: order.paymentStatus?.toLowerCase() || 'pending',
        orderDate: order.orderDate || order.createdAt,
        estimatedDelivery: order.estimatedDelivery || '',
        visibleToAdmin: true,
        shippingAddress: {
          fullName: order.shippingFullName || order.shipping_full_name || 'Unknown',
          email: 'Loading...',
          phone: order.shippingPhone || order.shipping_phone || 'N/A',
          addressLine1: order.shippingAddressLine1 || order.shipping_address_line1 || 'Address not available',
          addressLine2: order.shippingAddressLine2 || order.shipping_address_line2 || '',
          city: order.shippingCity || order.shipping_city || 'Unknown',
          state: order.shippingState || order.shipping_state || 'Unknown',
          postalCode: order.shippingPostalCode || order.shipping_postal_code || 'Unknown',
          country: order.shippingCountry || order.shipping_country || 'Unknown'
        }
      };

      console.log('Processing order:', order.id, 'User ID:', order.userId, 'Address ID:', order.addressId);
      console.log('Order payment status from backend:', order.paymentStatus);
      console.log('Order payment type from backend:', order.paymentType);
      
      // Use same approach as admin customers - get all users and find by ID
      const userPromise = order.userId ? 
        this.http.get<any[]>(`${EnvVariables.userServiceUrl}/users`).toPromise().then(users => {
          const user = users?.find(u => u.id == order.userId);
          return user || {
            name: order.shippingFullName || order.shipping_full_name || 'Unknown Customer',
            email: 'unknown@email.com',
            phone: order.shippingPhone || order.shipping_phone || 'N/A'
          };
        }).catch(error => {
          console.error('Error fetching users:', error);
          return {
            name: order.shippingFullName || order.shipping_full_name || 'Unknown Customer',
            email: 'unknown@email.com',
            phone: order.shippingPhone || order.shipping_phone || 'N/A'
          };
        }) : Promise.resolve({
          name: order.shippingFullName || order.shipping_full_name || 'Unknown Customer',
          email: 'unknown@email.com',
          phone: order.shippingPhone || order.shipping_phone || 'N/A'
        });
      
      // Use shipping address from order if available, otherwise fetch from user service
      const addressPromise = order.shippingAddress && order.shippingAddress.fullName ? 
        Promise.resolve(order.shippingAddress) : 
        (order.shippingAddressId ? 
          this.http.get<any>(`${EnvVariables.userServiceUrl}/addresses/${order.shippingAddressId}`).toPromise().catch(error => {
            console.error('Error fetching address:', error);
            return {
              fullName: 'Unknown',
              email: '',
              phone: '',
              addressLine1: 'Address not found',
              addressLine2: '',
              city: 'Unknown',
              state: 'Unknown',
              postalCode: 'Unknown',
              country: 'Unknown'
            };
          }) : Promise.resolve({
            fullName: 'No address',
            email: '',
            phone: '',
            addressLine1: 'No address provided',
            addressLine2: '',
            city: 'Unknown',
            state: 'Unknown',
            postalCode: 'Unknown',
            country: 'Unknown'
          }));
      
      return Promise.all([userPromise, addressPromise]).then(([userData, addressData]) => {
        // Use shipping address data for customer info since it has the correct information
        baseOrder.customerInfo = {
          fullName: order.shippingFullName || order.shipping_full_name || userData.name || userData.fullName || 'Unknown Customer',
          email: userData.email || 'unknown@email.com',
          phone: order.shippingPhone || order.shipping_phone || userData.phone || 'N/A'
        };
        
        // Use shipping address from API response
        if (order.shippingAddress) {
          baseOrder.shippingAddress = {
            fullName: order.shippingAddress.fullName || 'Unknown',
            email: userData.email || 'unknown@email.com',
            phone: order.shippingAddress.phone || 'N/A',
            addressLine1: order.shippingAddress.addressLine1 || 'Address not available',
            addressLine2: order.shippingAddress.addressLine2 || '',
            city: order.shippingAddress.city || 'Unknown',
            state: order.shippingAddress.state || 'Unknown',
            postalCode: order.shippingAddress.postalCode || 'Unknown',
            country: order.shippingAddress.country || 'Unknown'
          };
        } else {
          baseOrder.shippingAddress = {
            fullName: addressData.fullName || userData.name || 'Unknown',
            email: addressData.email || userData.email || 'unknown@email.com',
            phone: addressData.phone || userData.phone || 'N/A',
            addressLine1: addressData.addressLine1 || 'Address not available',
            addressLine2: addressData.addressLine2 || '',
            city: addressData.city || 'Unknown',
            state: addressData.state || 'Unknown',
            postalCode: addressData.postalCode || 'Unknown',
            country: addressData.country || 'Unknown'
          };
        }
        
        console.log('Final customer info for order', order.id, ':', baseOrder.customerInfo);
        console.log('Final shipping address for order', order.id, ':', baseOrder.shippingAddress);
        console.log('Final payment status for order', order.id, ':', baseOrder.paymentStatus);
        return baseOrder;
      });
    });

    // Wait for all user/address fetches to complete
    Promise.all(orderPromises).then(convertedOrders => {
      console.log('All converted orders with addresses:', convertedOrders);
      
      // Sort orders: active first, cancelled at bottom
      const statusRank = (s: string) => s === 'cancelled' ? 2 : 0;
      this.allOrders = convertedOrders.sort((a, b) => {
        const rA = statusRank(a.orderStatus);
        const rB = statusRank(b.orderStatus);
        if (rA !== rB) return rA - rB;
        return new Date(b.orderDate).getTime() - new Date(a.orderDate).getTime();
      });
      this.applyFilters();
      this.isLoading = false;
      console.log('Orders loaded with addresses:', this.allOrders.length);
    }).catch(error => {
      console.error('Error processing orders with addresses:', error);
      this.isLoading = false;
    });
  }

  /**
   * Apply filters to orders
   */
  applyFilters(): void {
    this.filteredOrders = this.allOrders.filter(order => {
      // Status filter
      if (this.filters.status && order.orderStatus !== this.filters.status) {
        return false;
      }
      
      // Payment status filter
      if (this.filters.paymentStatus && order.paymentStatus !== this.filters.paymentStatus) {
        return false;
      }
      
      // Date range filter
      if (this.filters.dateFrom) {
        const orderDate = new Date(order.orderDate);
        const fromDate = new Date(this.filters.dateFrom);
        if (orderDate < fromDate) return false;
      }
      
      if (this.filters.dateTo) {
        const orderDate = new Date(order.orderDate);
        const toDate = new Date(this.filters.dateTo);
        toDate.setHours(23, 59, 59, 999); // End of day
        if (orderDate > toDate) return false;
      }
      
      // Customer filter
      if (this.filters.customer) {
        const searchTerm = this.filters.customer.toLowerCase();
        const customerMatch = 
          order.customerInfo.fullName.toLowerCase().includes(searchTerm) ||
          order.customerInfo.email.toLowerCase().includes(searchTerm) ||
          (order.orderNumber?.toLowerCase().includes(searchTerm) || false);
        if (!customerMatch) return false;
      }
      
      return true;
    });
    
    this.sortOrders();
    this.updatePagination();
  }

  /**
   * Sort orders by specified field and direction
   */
  sortOrders(): void {
    this.filteredOrders.sort((a, b) => {
      let aValue: any = a[this.sortField as keyof Order];
      let bValue: any = b[this.sortField as keyof Order];
      
      // Handle nested properties
      if (this.sortField === 'customerName') {
        aValue = a.customerInfo.fullName;
        bValue = b.customerInfo.fullName;
      } else if (this.sortField === 'customerEmail') {
        aValue = a.customerInfo.email;
        bValue = b.customerInfo.email;
      }
      
      // Convert to comparable values
      if (this.sortField === 'orderDate' || this.sortField === 'estimatedDelivery') {
        aValue = new Date(aValue).getTime();
        bValue = new Date(bValue).getTime();
      } else if (typeof aValue === 'string') {
        aValue = aValue.toLowerCase();
        bValue = bValue.toLowerCase();
      }
      
      if (aValue < bValue) return this.sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }

  /**
   * Update pagination based on filtered orders
   */
  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredOrders.length / this.itemsPerPage);
    if (this.currentPage > this.totalPages) {
      this.currentPage = 1;
    }
    
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.paginatedOrders = this.filteredOrders.slice(startIndex, endIndex);
  }

  /**
   * Change current page
   */
  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePagination();
    }
  }

  /**
   * Change items per page
   */
  changeItemsPerPage(newSize: number): void {
    this.itemsPerPage = newSize;
    this.currentPage = 1;
    this.updatePagination();
  }

  /**
   * Sort by field
   */
  sortBy(field: string): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.sortOrders();
    this.updatePagination();
  }

  /**
   * Get sort icon for column headers
   */
  getSortIcon(field: string): string {
    if (this.sortField !== field) return '';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.filters = {
      status: '',
      paymentStatus: '',
      dateFrom: '',
      dateTo: '',
      customer: ''
    };
    this.applyFilters();
  }

  /**
   * Update order status with enhanced tracking
   */
  updateOrderStatus(order: Order, newStatus: string): void {
    this.isUpdating = true;
    
    if (!order.id) return;
    
    // Try microservice first
    this.orderProcessing.updateOrderStatusMicroservice(parseInt(order.id), newStatus).subscribe({
      next: (savedOrder) => {
        console.log('Order status updated via microservice:', savedOrder.id, newStatus);
        
        // Update in local array
        const index = this.allOrders.findIndex(o => o.id === savedOrder.id?.toString());
        if (index !== -1) {
          this.allOrders[index] = { ...this.allOrders[index], orderStatus: newStatus };
        }
        
        this.applyFilters();
        this.isUpdating = false;
      },
      error: (error) => {
        console.error('Error updating order status via microservice, trying JSON server:', error);
        
        // Fallback to JSON server update
        const updatedOrder = { ...order, orderStatus: newStatus };
        this.http.put(`${EnvVariables.apiBaseUrl}/orders/${order.id}`, updatedOrder).subscribe({
          next: (savedOrder) => {
            console.log('Order status updated via JSON server:', order.orderNumber, newStatus);
            
            const index = this.allOrders.findIndex(o => o.id === order.id);
            if (index !== -1) {
              this.allOrders[index] = updatedOrder;
            }
            
            this.applyFilters();
            this.isUpdating = false;
          },
          error: (fallbackError) => {
            console.error('Error updating order status via JSON server:', fallbackError);
            this.isUpdating = false;
          }
        });
      }
    });
  }

  /**
   * Open order processing modal
   */
  openProcessingModal(order: Order): void {
    this.selectedOrderForProcessing = order;
    this.newStatus = order.orderStatus;
    this.selectedLocation = order.currentLocation?.id || '';
    this.processingNotes = order.processingNotes || '';
    this.showProcessingModal = true;
  }

  /**
   * Get valid status transitions for current order
   */
  getValidStatusTransitions(): string[] {
    if (!this.selectedOrderForProcessing) return [];
    
    const currentStatus = this.selectedOrderForProcessing.orderStatus;
    const validTransitions: { [key: string]: string[] } = {
      'confirmed': ['processing'],
      'processing': ['shipped'],
      'shipped': ['delivered'],
      'delivered': [], // No further transitions
      'cancelled': [], // No further transitions
      'pending': ['confirmed', 'cancelled']
    };
    
    return validTransitions[currentStatus] || [];
  }

  /**
   * Process order with location and notes
   */
  processOrder(): void {
    if (!this.selectedOrderForProcessing || !this.selectedOrderForProcessing.id) return;

    // Validate status flow
    const currentStatus = this.selectedOrderForProcessing.orderStatus;
    const newStatus = this.newStatus;
    
    // Define valid status transitions
    const validTransitions: { [key: string]: string[] } = {
      'confirmed': ['processing'],
      'processing': ['shipped'],
      'shipped': ['delivered'],
      'delivered': [], // No further transitions
      'cancelled': [], // No further transitions
      'pending': ['confirmed', 'cancelled']
    };
    
    const allowedTransitions = validTransitions[currentStatus] || [];
    if (!allowedTransitions.includes(newStatus)) {
      alert(`Invalid status transition: ${currentStatus} → ${newStatus}. Allowed transitions: ${allowedTransitions.join(', ')}`);
      return;
    }

    this.isUpdating = true;
    
    // Use microservice method with location and notes
    let url = `${EnvVariables.orderServiceUrl}/orders/${this.selectedOrderForProcessing.id}/status?status=${this.newStatus.toUpperCase()}`;
    if (this.selectedLocation) url += `&locationId=${this.selectedLocation}`;
    if (this.processingNotes) url += `&notes=${encodeURIComponent(this.processingNotes)}`;
    
    console.log('Updating order status with URL:', url);
    this.http.put(url, {}).subscribe({
      next: (savedOrder) => {
        console.log('Order processed via microservice:', (savedOrder as any).id);
        
        // Update in local array
        const index = this.allOrders.findIndex(o => o.id === this.selectedOrderForProcessing?.id);
        if (index !== -1) {
          this.allOrders[index].orderStatus = this.newStatus;
        }
        
        this.applyFilters();
        this.isUpdating = false;
        this.closeProcessingModal();
      },
      error: (error) => {
        console.error('Error processing order:', error);
        this.isUpdating = false;
      }
    });
  }

  /**
   * Close processing modal
   */
  closeProcessingModal(): void {
    this.showProcessingModal = false;
    this.selectedOrderForProcessing = null;
    this.newStatus = '';
    this.selectedLocation = '';
    this.processingNotes = '';
  }

  /**
   * Open remove order modal
   */
  openRemoveModal(order: Order): void {
    this.selectedOrderForRemoval = order;
    this.removalReason = '';
    this.sendNotification = order.orderStatus !== 'delivered';
    this.showRemoveModal = true;
  }

  /**
   * Remove order
   */
  removeOrder(): void {
    if (!this.selectedOrderForRemoval || !this.selectedOrderForRemoval.id) return;

    this.isUpdating = true;
    this.showRemoveModal = false;
    
    // If order is delivered, just remove it without notification
    if (this.selectedOrderForRemoval.orderStatus === 'delivered') {
      this.http.delete(`${EnvVariables.apiBaseUrl}/orders/${this.selectedOrderForRemoval.id}`).subscribe({
        next: () => {
          console.log('Order removed (delivered):', this.selectedOrderForRemoval?.orderNumber);
          this.allOrders = this.allOrders.filter(o => o.id !== this.selectedOrderForRemoval?.id);
          this.applyFilters();
          this.closeRemoveModal();
          this.isUpdating = false;
        },
        error: (error) => {
          console.error('Error removing delivered order:', error);
          this.isUpdating = false;
        }
      });
      return;
    }

    // For non-delivered orders, send notification FIRST, then remove order
    if (this.sendNotification) {
      // Send notification first, then remove order
      this.sendCustomerNotification().subscribe({
        next: () => {
          console.log('Notification sent successfully, now removing order');
          this.removeOrderFromDatabase();
        },
        error: (error) => {
          console.error('Error sending notification:', error);
          // Even if notification fails, still remove the order
          this.removeOrderFromDatabase();
        }
      });
    } else {
      // No notification needed, just remove order
      this.removeOrderFromDatabase();
    }
  }

  /**
   * Archive order instead of deleting it
   */
  private removeOrderFromDatabase(): void {
    if (!this.selectedOrderForRemoval || !this.selectedOrderForRemoval.id) return;

    // Calculate auto-delete date (30 days from now)
    const autoDeleteDate = new Date();
    autoDeleteDate.setDate(autoDeleteDate.getDate() + 30);

    // Archive the order instead of deleting
    const updatedOrder = {
      ...this.selectedOrderForRemoval,
      orderStatus: 'cancelled',
      isArchived: true,
      archivedAt: new Date().toISOString(),
      archivedReason: this.removalReason,
      archivedBy: 'admin',
      autoDeleteDate: autoDeleteDate.toISOString(),
      cancelledAt: new Date().toISOString(),
      cancelledBy: 'admin',
      cancellationReason: this.removalReason
    };

    const restock$ = this.selectedOrderForRemoval.orderStatus !== 'delivered' 
      ? this.restockOrderItems(this.selectedOrderForRemoval)
      : of(null);

    const orderId = this.selectedOrderForRemoval.id as string;
    const orderNumber = this.selectedOrderForRemoval.orderNumber || '';

    // Use Order Service delete endpoint
    this.http.delete(`${EnvVariables.orderServiceUrl}/orders/${orderId}`).subscribe({
      next: () => {
        console.log('Order deleted by admin:', orderNumber);
        this.showSuccessNotification('Order deleted successfully');
        this.loadOrders();
        this.isUpdating = false;
      },
      error: (error) => {
        console.error('Error deleting order:', error);
        this.isUpdating = false;
      }
    });
  }

  /**
   * Send notification to customer about order removal
   */
  private sendCustomerNotification(): Observable<any> {
    if (!this.selectedOrderForRemoval) {
      return new Observable(observer => observer.error('No order selected for removal'));
    }

    const notification = {
      id: Date.now().toString(),
      orderNumber: this.selectedOrderForRemoval.orderNumber,
      customerEmail: this.selectedOrderForRemoval.customerInfo.email,
      customerName: this.selectedOrderForRemoval.customerInfo.fullName,
      type: 'order_removed',
      title: 'Order Cancelled - Refund Processed',
      message: `Dear ${this.selectedOrderForRemoval.customerInfo.fullName}, your order ${this.selectedOrderForRemoval.orderNumber} has been cancelled due to: ${this.removalReason || 'unforeseen circumstances'}. A refund of ${this.formatCurrency(this.selectedOrderForRemoval.total)} will be processed within 3-5 business days.`,
      reason: this.removalReason,
      amount: this.selectedOrderForRemoval.total,
      createdAt: new Date().toISOString(),
      read: false
    };

    console.log('📧 Sending customer notification:', notification);

    // Save notification to database and return Observable
    return this.http.post(`${EnvVariables.apiBaseUrl}/notifications`, notification).pipe(
      tap((response) => {
        console.log('Customer notification sent successfully:', response);
      }),
      catchError((error) => {
        console.error('Error sending customer notification:', error);
        throw error;
      })
    );
  }

  /**
   * Close remove modal
   */
  closeRemoveModal(): void {
    this.showRemoveModal = false;
    this.selectedOrderForRemoval = null;
    this.removalReason = '';
    this.sendNotification = true;
  }

  /**
   * View order details
   */
  viewOrderDetails(order: Order): void {
    this.selectedOrder = order;
    this.showOrderDetails = true;
  }

  /**
   * Close order details modal
   */
  closeOrderDetails(): void {
    this.showOrderDetails = false;
    this.selectedOrder = null;
  }

  /**
   * Format currency
   */
  formatCurrency(amount: number): string {
    return `$${amount.toFixed(2)}`;
  }

  /**
   * Format date
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  /**
   * Get status badge class
   */
  getStatusBadgeClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'confirmed': return 'badge bg-primary';
      case 'processing': return 'badge bg-warning';
      case 'shipped': return 'badge bg-info';
      case 'delivered': return 'badge bg-success';
      case 'cancelled': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  }

  /**
   * Get payment status badge class
   */
  getPaymentStatusBadgeClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'completed': return 'badge bg-success';
      case 'pending': return 'badge bg-warning';
      case 'failed': return 'badge bg-danger';
      case 'refunded': return 'badge bg-info';
      default: return 'badge bg-secondary';
    }
  }

  /**
   * Admin logout
   */
  onAdminLogout(): void {
    this.adminAuth.logout().subscribe({
      next: () => {
        this.router.navigate(['/'], { replaceUrl: true });
      },
      error: () => {
        this.router.navigate(['/'], { replaceUrl: true });
      }
    });
  }

  /**
   * Track by function for ngFor optimization
   */
  trackByOrderId(index: number, order: Order): string {
    return order.id || '';
  }

  /**
   * Get count of orders by status
   */
  getOrderCountByStatus(status: string): number {
    return this.allOrders.filter(order => order.orderStatus === status).length;
  }

  /**
   * Get total orders count
   */
  getTotalOrdersCount(): number {
    return this.allOrders.length;
  }

  /** Restock all items of a given order (if not delivered) */
  private restockOrderItems(order: Order): Observable<any> {
    if (!order || order.orderStatus === 'delivered' || !Array.isArray(order.items)) {
      return of(null);
    }
    const ops = order.items.map((it: any) => {
      const productId = Number(it?.ProductID || it?.Product?.id);
      const qty = Number(it?.Quantity || 0);
      if (!productId || qty <= 0) { return of(null); }
      const url = `${EnvVariables.apiBaseUrl}/products/${productId}`;
      return this.http.get<any>(url).pipe(
        switchMap((product) => {
          const next = Number(product?.stock || 0) + qty;
          return this.http.put(url, { ...product, stock: next });
        })
      );
    });
    return ops.length ? forkJoin(ops) : of(null);
  }
} 
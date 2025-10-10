import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AdminAuthService } from '../../../services/admin-auth.service';
import { EnvVariables } from '../../../env/env-variables';

interface Customer {
  id: number;
  userId: string;
  name: string | null;
  username: string;
  email: string;
  phone?: string | null;
  dateOfBirth?: string | null;
  gender?: string | null;
  role: string;
  createdAt: string;
  updatedAt: string;
  lastLogin?: string | null;
  totalOrders?: number;
  totalSpent?: number;
}

interface CustomerFilters {
  name: string;
  email: string;
  gender: string;
  city: string;
  dateFrom: string;
  dateTo: string;
}

interface CustomerStats {
  totalCustomers: number;
  newThisMonth: number;
  totalRevenue: number;
  averageOrderValue: number;
  topSpenders: Customer[];
}

@Component({
  selector: 'app-admin-customers',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-customers.html',
  styleUrls: ['./admin-customers.css', '../admin-global.css']
})
export class AdminCustomersComponent implements OnInit {
  // Customer data
  allCustomers: Customer[] = [];
  filteredCustomers: Customer[] = [];
  
  // Pagination
  currentPage = 1;
  itemsPerPage = 15;
  totalPages = 0;
  paginatedCustomers: Customer[] = [];
  
  // Filters
  filters: CustomerFilters = {
    name: '',
    email: '',
    gender: '',
    city: '',
    dateFrom: '',
    dateTo: ''
  };
  
  // Available filter options
  genderOptions = ['male', 'female', 'other'];
  
  // Sorting
  sortField = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Loading states
  isLoading = false;
  
  // Customer details modal
  showCustomerDetails = false;
  selectedCustomer: Customer | null = null;
  
  // Statistics
  customerStats: CustomerStats = {
    totalCustomers: 0,
    newThisMonth: 0,
    totalRevenue: 0,
    averageOrderValue: 0,
    topSpenders: []
  };

  constructor(
    private http: HttpClient,
    private router: Router,
    public adminAuth: AdminAuthService
  ) {}

  ngOnInit(): void {
    this.loadCustomers();
  }

  /**
   * Load all customers from the database
   */
  loadCustomers(): void {
    this.isLoading = true;
    
    // Load customers and orders in parallel
    Promise.all([
      firstValueFrom(this.http.get<any[]>(`${EnvVariables.userServiceUrl}/users`)),
      firstValueFrom(this.http.get<any>(`${EnvVariables.orderServiceUrl}/orders?page=0&size=1000`))
    ]).then(([allUsers, ordersResponse]) => {
      console.log('Raw users API response:', allUsers);
      console.log('Raw orders API response:', ordersResponse);
      
      if (allUsers && allUsers.length > 0) {
        // Filter customers
        this.allCustomers = allUsers.filter(user => user.role === 'customer');
        console.log('Customers after filtering:', this.allCustomers.length);
        console.log('Customer data:', this.allCustomers);
      } else {
        console.log('No users received from API');
        this.allCustomers = [];
      }
      
      // Extract orders from response
      const orders = ordersResponse?.content || ordersResponse || [];
      console.log('Orders data:', orders);
      
      this.calculateCustomerStats(this.allCustomers, orders);
      this.applyFilters();
      this.isLoading = false;
    }).catch(error => {
      console.error('Error loading customers and orders:', error);
      this.isLoading = false;
    });
  }

  /**
   * Calculate customer statistics
   */
  calculateCustomerStats(customers: Customer[], orders: any[]): void {
    // Calculate order statistics for each customer
    customers.forEach(customer => {
      const customerOrders = orders.filter(order => order.customerInfo?.email === customer.email);
      customer.totalOrders = customerOrders.length;
      customer.totalSpent = customerOrders.reduce((sum, order) => sum + (order.total || 0), 0);
    });

    // Calculate statistics
    this.customerStats.totalCustomers = customers.length;
    this.customerStats.totalRevenue = orders.reduce((sum, order) => sum + (order.total || 0), 0);
    this.customerStats.averageOrderValue = orders.length > 0 ? this.customerStats.totalRevenue / orders.length : 0;
    
    // New customers this month
    const thisMonth = new Date();
    thisMonth.setDate(1);
    this.customerStats.newThisMonth = customers.filter(customer => {
      if (customer.createdAt) {
        return new Date(customer.createdAt) >= thisMonth;
      }
      return false;
    }).length;
    
    // Top 5 spenders
    this.customerStats.topSpenders = customers
      .filter(customer => (customer.totalSpent || 0) > 0)
      .sort((a, b) => (b.totalSpent || 0) - (a.totalSpent || 0))
      .slice(0, 5);
  }

  /**
   * Apply filters to customers
   */
  applyFilters(): void {
    console.log('Applying filters to customers:', this.allCustomers.length);
    this.filteredCustomers = this.allCustomers.filter(customer => {
      // Name filter
      if (this.filters.name) {
        const searchName = (customer.name || customer.username || '').toLowerCase();
        if (!searchName.includes(this.filters.name.toLowerCase())) {
          return false;
        }
      }
      
      // Email filter
      if (this.filters.email && !customer.email.toLowerCase().includes(this.filters.email.toLowerCase())) {
        return false;
      }
      
      // Gender filter
      if (this.filters.gender && customer.gender !== this.filters.gender) {
        return false;
      }
      
      // City filter - not available in current API
      // Skip city filter for now
      
      // Date range filter
      if (this.filters.dateFrom && customer.createdAt) {
        const customerDate = new Date(customer.createdAt);
        const fromDate = new Date(this.filters.dateFrom);
        if (customerDate < fromDate) return false;
      }
      
      if (this.filters.dateTo && customer.createdAt) {
        const customerDate = new Date(customer.createdAt);
        const toDate = new Date(this.filters.dateTo);
        toDate.setHours(23, 59, 59, 999);
        if (customerDate > toDate) return false;
      }
      
      return true;
    });
    
    console.log('Customers after filtering:', this.filteredCustomers.length);
    this.sortCustomers();
    this.updatePagination();
    console.log('Paginated customers:', this.paginatedCustomers.length);
  }

  /**
   * Sort customers by specified field and direction
   */
  sortCustomers(): void {
    this.filteredCustomers.sort((a, b) => {
      let aValue: any = a[this.sortField as keyof Customer];
      let bValue: any = b[this.sortField as keyof Customer];
      
      // Handle nested properties
      if (this.sortField === 'city') {
        aValue = '';
        bValue = '';
      }
      
      // Convert to comparable values
      if (this.sortField === 'createdAt') {
        aValue = aValue ? new Date(aValue).getTime() : 0;
        bValue = bValue ? new Date(bValue).getTime() : 0;
      } else if (typeof aValue === 'string') {
        aValue = aValue.toLowerCase();
        bValue = bValue ? bValue.toLowerCase() : '';
      }
      
      if (aValue < bValue) return this.sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }

  /**
   * Update pagination based on filtered customers
   */
  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredCustomers.length / this.itemsPerPage);
    if (this.currentPage > this.totalPages) {
      this.currentPage = 1;
    }
    
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.paginatedCustomers = this.filteredCustomers.slice(startIndex, endIndex);
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
    this.sortCustomers();
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
      name: '',
      email: '',
      gender: '',
      city: '',
      dateFrom: '',
      dateTo: ''
    };
    this.applyFilters();
  }

  /**
   * View customer details
   */
  viewCustomerDetails(customer: Customer): void {
    this.selectedCustomer = customer;
    this.showCustomerDetails = true;
  }

  /**
   * Close customer details modal
   */
  closeCustomerDetails(): void {
    this.showCustomerDetails = false;
    this.selectedCustomer = null;
  }

  /**
   * Navigate to customer's orders
   */
  viewCustomerOrders(customer: Customer): void {
    // Navigate to orders page with customer email filter
    this.router.navigate(['/admin/orders'], { 
      queryParams: { customer: customer.email } 
    });
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
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString();
  }

  /**
   * Get gender badge class
   */
  getGenderBadgeClass(gender?: string | null): string {
    switch (gender?.toLowerCase()) {
      case 'male': return 'badge bg-primary';
      case 'female': return 'badge bg-info';
      case 'other': return 'badge bg-secondary';
      default: return 'badge bg-light text-dark';
    }
  }

  /**
   * Get customer tier based on total spent
   */
  getCustomerTier(totalSpent: number): { name: string, class: string } {
    if (totalSpent >= 1000) return { name: 'VIP', class: 'badge bg-warning' };
    if (totalSpent >= 500) return { name: 'Gold', class: 'badge bg-success' };
    if (totalSpent >= 100) return { name: 'Silver', class: 'badge bg-info' };
    return { name: 'Bronze', class: 'badge bg-secondary' };
  }

  /**
   * Admin logout
   */
  onAdminLogout(): void {
    this.adminAuth.logout();
    this.router.navigate(['/']);
  }

  /**
   * Track by function for ngFor optimization
   */
  trackByCustomerId(index: number, customer: Customer): number {
    return customer.id;
  }
} 
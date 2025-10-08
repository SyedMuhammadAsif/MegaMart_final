import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { CartService } from '../../services/cart-service';
import { Address } from '../../models/address';
import { Cart } from '../../models/cart-items';
import { ManageProfileService } from '../../services/manage-profile.service';
import { UserAddress, User } from '../../models/user';

@Component({
  selector: 'app-checkout-address',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './checkout-address.html',
  styleUrl: './checkout-address.css'
})
export class CheckoutAddressComponent implements OnInit {
  cart: Cart = { items: [], totalItems: 0, totalPrice: 0 };
  
  // Address being entered for "new address" flow
  address: Address = {
    fullName: '',
    email: '',
    phone: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'India',
    isDefault: false
  };

  // Saved addresses
  userAddresses: UserAddress[] = [];
  selectedAddressId: string | 'new' | '' = '';
  useNewAddress = false;
  currentUserEmail: string = '';

  // India states and cities for cascading selection
  indiaStates = [
    { code: 'AP', name: 'Andhra Pradesh', cities: ['Visakhapatnam', 'Vijayawada', 'Guntur', 'Nellore', 'Kurnool'] },
    { code: 'DL', name: 'Delhi', cities: ['New Delhi', 'Delhi', 'North Delhi', 'South Delhi', 'East Delhi'] },
    { code: 'KA', name: 'Karnataka', cities: ['Bangalore', 'Mysore', 'Hubli', 'Mangalore', 'Belgaum'] },
    { code: 'MH', name: 'Maharashtra', cities: ['Mumbai', 'Pune', 'Nagpur', 'Thane', 'Nashik'] },
    { code: 'TN', name: 'Tamil Nadu', cities: ['Chennai', 'Coimbatore', 'Madurai', 'Salem', 'Tiruchirappalli'] },
    { code: 'UP', name: 'Uttar Pradesh', cities: ['Lucknow', 'Kanpur', 'Ghaziabad', 'Agra', 'Varanasi'] },
    { code: 'GJ', name: 'Gujarat', cities: ['Ahmedabad', 'Surat', 'Vadodara', 'Rajkot', 'Bhavnagar'] },
    { code: 'RJ', name: 'Rajasthan', cities: ['Jaipur', 'Jodhpur', 'Udaipur', 'Kota', 'Ajmer'] },
    { code: 'WB', name: 'West Bengal', cities: ['Kolkata', 'Howrah', 'Durgapur', 'Asansol', 'Siliguri'] },
    { code: 'HR', name: 'Haryana', cities: ['Gurgaon', 'Faridabad', 'Panipat', 'Yamunanagar', 'Rohtak'] }
  ];

  availableCities: string[] = [];

  // Form validation errors
  formErrors: { [key: string]: string } = {};

  constructor(
    private router: Router,
    private cartService: CartService,
    private manageProfileService: ManageProfileService
  ) {}

  ngOnInit(): void {
    // Load cart data when component starts
    this.cartService.cart$.subscribe(cart => {
      this.cart = cart;
      
      // If cart is empty, redirect to cart page
      if (cart.items.length === 0) {
        this.router.navigate(['/cart']);
      }
    });

    // Load user saved addresses
    this.manageProfileService.getUserProfile().subscribe({
      next: (user: User) => {
        this.currentUserEmail = user.email;
        this.userAddresses = user.addresses || [];
        if (this.userAddresses.length > 0) {
          // Default to first saved address
          this.selectedAddressId = this.userAddresses[0].id || '';
          this.useNewAddress = false;
        } else {
          // No saved addresses → show new address form
          this.selectedAddressId = 'new';
          this.useNewAddress = true;
          this.address.email = this.currentUserEmail;
        }
      },
      error: () => {
        // Fallback to new address form if profile fails (shouldn't happen due to guard)
        this.selectedAddressId = 'new';
        this.useNewAddress = true;
      }
    });
  }

  // Cascading selection methods
  onStateChange(): void {
    this.address.city = '';
    this.address.postalCode = '';
    const selectedState = this.indiaStates.find(s => s.code === this.address.state);
    if (selectedState) {
      this.availableCities = selectedState.cities;
    } else {
      this.availableCities = [];
    }
    this.clearFormErrors();
  }

  onCityChange(): void {
    this.address.postalCode = '';
    this.clearFormErrors();
  }

  private clearFormErrors(): void {
    this.formErrors = {};
  }

  // Validation methods
  private validateForm(): boolean {
    this.formErrors = {};
    let isValid = true;

    // Full Name validation
    if (!this.address.fullName || this.address.fullName.trim().length < 2) {
      this.formErrors['fullName'] = 'Full name must be at least 2 characters';
      isValid = false;
    }

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!this.address.email || !emailRegex.test(this.address.email)) {
      this.formErrors['email'] = 'Please enter a valid email address';
      isValid = false;
    }

    // Phone validation (10 digits for India)
    if (!this.address.phone || !/^[0-9]{10}$/.test(this.address.phone)) {
      this.formErrors['phone'] = 'Phone number must be exactly 10 digits';
      isValid = false;
    }

    // Address Line 1 validation
    if (!this.address.addressLine1 || this.address.addressLine1.trim().length < 5) {
      this.formErrors['addressLine1'] = 'Address must be at least 5 characters';
      isValid = false;
    }

    // State validation
    if (!this.address.state) {
      this.formErrors['state'] = 'Please select a state';
      isValid = false;
    }

    // City validation
    if (!this.address.city) {
      this.formErrors['city'] = 'Please select a city';
      isValid = false;
    }

    // PIN Code validation (6 digits for India)
    if (!this.address.postalCode || !/^[0-9]{6}$/.test(this.address.postalCode)) {
      this.formErrors['postalCode'] = 'PIN code must be exactly 6 digits';
      isValid = false;
    }

    return isValid;
  }

  // Check if field has error
  hasError(fieldName: string): boolean {
    return !!this.formErrors[fieldName];
  }

  // Get error message for field
  getErrorMessage(fieldName: string): string {
    return this.formErrors[fieldName] || '';
  }

  onAddressChoiceChange(value: string): void {
    if (value === 'new') {
      this.useNewAddress = true;
      this.selectedAddressId = 'new';
      // Prefill email for convenience
      this.address.email = this.currentUserEmail;
    } else {
      this.useNewAddress = false;
      this.selectedAddressId = value;
    }
  }

  onSubmit(form: NgForm): void {
    // This submit is only for new address flow
    if (!this.useNewAddress) {
      return;
    }

    if (this.validateForm()) {
      // Don't save to profile during checkout - let order service handle it
      const addressForCheckout: Address = {
        ...this.address,
        email: this.currentUserEmail
      };
      // Persist for payment step
      sessionStorage.setItem('checkoutAddress', JSON.stringify(addressForCheckout));
      this.router.navigate(['/checkout/payment']);
    } else {
      // Form validation failed
      this.markFormGroupTouched(form);
    }
  }

  useSelectedAddress(): void {
    if (!this.selectedAddressId || this.selectedAddressId === 'new') {
      return;
    }
    const found = this.userAddresses.find(a => a.id === this.selectedAddressId);
    if (!found) return;

    const addressForCheckout: Address = {
      fullName: found.fullName,
      email: this.currentUserEmail,
      phone: found.phone,
      addressLine1: found.addressLine1,
      addressLine2: found.addressLine2 || '',
      city: found.city,
      state: found.state,
      postalCode: found.postalCode,
      country: found.country,
      isDefault: !!found.isDefault
    };

    sessionStorage.setItem('checkoutAddress', JSON.stringify(addressForCheckout));
    this.router.navigate(['/checkout/payment']);
  }

  private markFormGroupTouched(form: NgForm): void {
    Object.keys(form.controls).forEach(key => {
      form.controls[key].markAsTouched();
    });
  }

  goBackToCart(): void {
    this.router.navigate(['/cart']);
  }

  formatPrice(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  // Format phone number to only allow digits
  formatPhoneNumber(event: any): void {
    let value = event.target.value.replace(/\D/g, ''); // Remove non-digits
    value = value.substring(0, 10); // Limit to 10 digits
    this.address.phone = value;
    event.target.value = value;
  }

  // Format PIN code to only allow digits
  formatPinCode(event: any): void {
    let value = event.target.value.replace(/\D/g, ''); // Remove non-digits
    value = value.substring(0, 6); // Limit to 6 digits
    this.address.postalCode = value;
    event.target.value = value;
  }
} 

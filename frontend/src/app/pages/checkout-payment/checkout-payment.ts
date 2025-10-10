import { Component, OnInit, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CartService } from '../../services/cart-service';
import { ProductService } from '../../services/product-service';
import { ToastService } from '../../services/toast-service';
import { AuthService } from '../../services/auth.service';
import { Address } from '../../models/address';
import { PaymentMethod, Order, OrderResponse, PaymentType } from '../../models/payment';
import { Cart } from '../../models/cart-items';
import { User, UserPaymentMethod } from '../../models/user';

import { OrderProcessingService } from '../../services/order-processing.service';
import { EnvVariables } from '../../env/env-variables';

@Component({
  selector: 'app-checkout-payment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './checkout-payment.html',
  styleUrl: './checkout-payment.css'
})
export class CheckoutPaymentComponent implements OnInit {
  cart: Cart = { items: [], totalItems: 0, totalPrice: 0 };
  shippingAddress: Address | null = null;
  isProcessing = false;
  currentUser: User | null = null;

  // Payment type selection
  selectedPaymentType: PaymentType = 'card';

  // Form for new payment method
  paymentForm: any;

  // Existing payment methods
  userPaymentMethods: UserPaymentMethod[] = [];
  selectedPaymentMethodId: string | 'new' | '' = '';
  useNewPaymentMethod = false;

  // CVV input for saved cards
  savedCardCVV: string = '';
  showCVVInput: boolean = false;

  // Overlay states
  isOverlayVisible = false;
  overlayStage: 'loading' | 'success' = 'loading';
  overlayTitle = '';
  overlaySubtitle = '';

  // Payment method being processed
  payment: PaymentMethod = {
    type: 'card' as PaymentType, // Will be updated dynamically
    cardNumber: '',
    expiryMonth: '',
    expiryYear: '',
    cvv: '',
    cardholderName: '',
    upiId: ''
  };

  // Form options
  months = [
    '01', '02', '03', '04', '05', '06',
    '07', '08', '09', '10', '11', '12'
  ];
  years: string[] = [];

  // Validation errors
  formErrors: { [key: string]: string } = {};

  constructor(
    private router: Router,
    private cartService: CartService,
    private productService: ProductService,
    private http: HttpClient,
    private toastService: ToastService,
    private ngZone: NgZone,
    private fb: FormBuilder,

    private orderProcessingService: OrderProcessingService,
    private authService: AuthService
  ) {
    // Generate years for expiry (current year + 10 years)
    const currentYear = new Date().getFullYear();
    for (let i = 0; i < 11; i++) {
      this.years.push((currentYear + i).toString());
    }

    // Create the simple form here - much easier for beginners!
    this.paymentForm = this.fb.group({
      cardholderName: [''],
      cardNumber: [''],
      expiryMonth: [''],
      expiryYear: [''],
      cvv: [''],
      upiId: ['']
    });
  }

  ngOnInit(): void {
    // Load current user data
    this.loadCurrentUser();

    // Load cart data
    this.cartService.cart$.subscribe(cart => {
      this.cart = cart;
      
      // If cart is empty, redirect to cart page
      if (cart.items.length === 0) {
        this.router.navigate(['/cart']);
        return;
      }
    });

    // Load shipping address from session storage
    const savedAddress = sessionStorage.getItem('checkoutAddress');
    if (!savedAddress) {
      // If no address, redirect back to address step
      this.router.navigate(['/checkout/address']);
      return;
    }
    
    this.shippingAddress = JSON.parse(savedAddress);
  }

  /**
   * Load current user data to check existing payment methods
   */
  private loadCurrentUser(): void {
    // Simplified user loading - no saved payment methods for now
    this.currentUser = null;
    this.userPaymentMethods = [];
    this.selectedPaymentType = 'card';
    this.selectedPaymentMethodId = 'new';
    this.useNewPaymentMethod = true;
    this.showCVVInput = false;
  }

  /**
   * Load selected payment method details into the form
   */
  private loadSelectedPaymentMethod(): void {
    if (this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
      const selectedMethod = this.userPaymentMethods.find(method => method.id === this.selectedPaymentMethodId);
      if (selectedMethod) {
        // Only load details that match the current payment type
        if (this.selectedPaymentType === 'card' && selectedMethod.type === 'card') {
          // Load card details
          this.payment = {
            type: 'card',
            cardNumber: selectedMethod.cardNumber || '',
            expiryMonth: selectedMethod.expiryMonth || '',
            expiryYear: selectedMethod.expiryYear || '',
            cvv: '', // Don't pre-fill CVV for security
            cardholderName: selectedMethod.cardholderName || '',
            upiId: ''
          };
          
          // Update form with selected method details
          this.paymentForm.patchValue({
            cardholderName: this.payment.cardholderName,
            cardNumber: this.payment.cardNumber,
            expiryMonth: this.payment.expiryMonth,
            expiryYear: this.payment.expiryYear,
            upiId: ''
          });
        } else if (this.selectedPaymentType === 'upi' && selectedMethod.type === 'upi') {
          // Load UPI details
          this.payment = {
            type: 'upi',
            cardNumber: '',
            expiryMonth: '',
            expiryYear: '',
            cvv: '',
            cardholderName: '',
            upiId: selectedMethod.upiId || ''
          };
          
          // Update form with selected UPI method details
          this.paymentForm.patchValue({
            cardholderName: '',
            cardNumber: '',
            expiryMonth: '',
            expiryYear: '',
            cvv: '',
            upiId: this.payment.upiId
          });
        }
        
        console.log('Loaded payment method details:', this.payment);
      }
    }
  }

  /**
   * Handle payment method selection change
   */
  onPaymentMethodChoiceChange(value: string): void {
    console.log('Payment method selection changed to:', value);
    console.log('Previous state - selectedPaymentMethodId:', this.selectedPaymentMethodId, 'useNewPaymentMethod:', this.useNewPaymentMethod);
    
    if (value === 'new') {
      this.useNewPaymentMethod = true;
      this.selectedPaymentMethodId = 'new';
      this.showCVVInput = false;
      this.savedCardCVV = '';
      console.log('Switched to new payment method');
      // Clear form for new payment method
      this.paymentForm.reset();
      this.payment = {
        type: this.selectedPaymentType,
        cardNumber: '',
        expiryMonth: '',
        expiryYear: '',
        cvv: '',
        cardholderName: '',
        upiId: ''
      };
    } else {
      this.useNewPaymentMethod = false;
      this.selectedPaymentMethodId = value;
      
      // Only show CVV input for saved card methods, not UPI
      const selectedMethod = this.userPaymentMethods.find(method => method.id === value);
      this.showCVVInput = this.selectedPaymentType === 'card' && selectedMethod?.type === 'card';
      this.savedCardCVV = '';
      
      console.log('Switched to existing payment method:', value, 'Type:', selectedMethod?.type, 'Show CVV:', this.showCVVInput);
      this.loadSelectedPaymentMethod();
    }
    
    console.log('New state - selectedPaymentMethodId:', this.selectedPaymentMethodId, 'useNewPaymentMethod:', this.useNewPaymentMethod);
  }

  /**
   * Get the selected payment method object
   */
  getSelectedPaymentMethod(): UserPaymentMethod | undefined {
    if (this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
      return this.userPaymentMethods.find(method => method.id === this.selectedPaymentMethodId);
    }
    return undefined;
  }

  /**
   * Get the last 4 digits of the selected card
   */
  getSelectedCardLastFour(): string {
    const method = this.getSelectedPaymentMethod();
    if (method && method.type === 'card' && method.cardNumber) {
      return method.cardNumber.slice(-4);
    }
    return '****';
  }

  /**
   * Handle CVV input for saved cards
   */
  onSavedCardCVVInput(event: any): void {
    this.savedCardCVV = event.target.value;
  }

  /**
   * Check if the entered card details match any existing payment method
   * @param cardDetails - The card details to validate
   * @returns boolean - true if card details match existing ones, false otherwise
   */
  private validateExistingCardDetails(cardDetails: any): boolean {
    if (!this.currentUser?.paymentMethods) {
      return false; // No existing payment methods
    }

    const existingCard = this.currentUser.paymentMethods.find(method => 
      method.type === 'card' &&
      method.cardNumber?.replace(/\s/g, '') === cardDetails.cardNumber &&
      method.expiryMonth === cardDetails.expiryMonth &&
      method.expiryYear === cardDetails.expiryYear &&
      method.cardholderName?.toLowerCase() === cardDetails.cardholderName?.toLowerCase()
    );

    return !!existingCard;
  }

  /**
   * Save new payment method to user's account
   * @param paymentMethod - The payment method to save
   */
  private saveNewPaymentMethod(paymentMethod: UserPaymentMethod): void {
    if (!this.currentUser) {
      return;
    }

    // Initialize paymentMethods array if it doesn't exist
    if (!this.currentUser.paymentMethods) {
      this.currentUser.paymentMethods = [];
    }

    // Add unique ID and timestamp to the payment method
    const newPaymentMethod: UserPaymentMethod = {
      ...paymentMethod,
      id: Date.now().toString(),
      isDefault: this.currentUser.paymentMethods.length === 0 // First payment method is default
    };

    // Add to user's payment methods
    this.currentUser.paymentMethods.push(newPaymentMethod);

    // Payment method saving disabled for now
    console.log('Payment method saving disabled');
  }

  // Simple form submission - easy to understand!
  onSubmit(): void {
    // Validate form based on payment type
    if (this.selectedPaymentType === 'cod') {
      // Cash on delivery - no validation needed
      this.processPaymentMicroservice();
      return;
    }

    // For card/UPI payments, check if using existing method or new one
    if (this.selectedPaymentType === 'card' || this.selectedPaymentType === 'upi') {
      if (this.useNewPaymentMethod || this.userPaymentMethods.length === 0) {
        // Using new payment method - validate form
        if (this.paymentForm.invalid) {
          this.markFormGroupTouched(this.paymentForm);
          return;
        }

        // Update payment object with form values
        this.syncPaymentObject();

        // Validate payment form before proceeding
        if (!this.validatePaymentForm()) {
          // Show validation errors
          this.toastService.showError('Please fix the validation errors');
          return;
        }

        // Validate specific payment type requirements
        if (this.selectedPaymentType === 'card') {
          const cvvValue = this.paymentForm.get('cvv')?.value || '';
          if (!this.paymentForm.get('cardholderName')?.value ||
              !this.paymentForm.get('cardNumber')?.value ||
              !this.paymentForm.get('expiryMonth')?.value ||
              !this.paymentForm.get('expiryYear')?.value ||
              !cvvValue || cvvValue.length !== 3) {
            this.toastService.showError('Please fill in all required card fields (CVV must be 3 digits)');
            return;
          }
        } else if (this.selectedPaymentType === 'upi') {
          if (!this.paymentForm.get('upiId')?.value) {
            this.toastService.showError('Please enter your UPI ID');
            return;
          }
          if (!this.isValidUPI(this.paymentForm.get('upiId')?.value)) {
            this.toastService.showError('Please enter a valid UPI ID (must end with @gpay, @phonepe, or @paytm)');
            return;
          }
        }

      } else {
        // Using existing payment method - validate selection
        if (!this.selectedPaymentMethodId || this.selectedPaymentMethodId === 'new') {
          this.toastService.showError('Please select a payment method');
          return;
        }

        // Get the selected payment method details
        const selectedMethod = this.userPaymentMethods.find(method => method.id === this.selectedPaymentMethodId);
        if (!selectedMethod) {
          this.toastService.showError('Selected payment method not found');
          return;
        }

        // Update payment object with selected method details
        this.payment = {
          type: selectedMethod.type as PaymentType,
          cardNumber: selectedMethod.cardNumber || '',
          expiryMonth: selectedMethod.expiryMonth || '',
          expiryYear: selectedMethod.expiryYear || '',
          cvv: '', // CVV is not stored for security
          cardholderName: selectedMethod.cardholderName || '',
          upiId: selectedMethod.upiId || ''
        };

        // For card payments, we still need CVV from user
        if (this.selectedPaymentType === 'card') {
          const cvv = this.savedCardCVV.replace(/\D/g, '').substring(0, 3);
          if (!cvv || cvv.length !== 3) {
            this.toastService.showError('CVV must be exactly 3 digits');
            return;
          }
          this.payment.cvv = cvv;
        }
      }
    }

    // Use microservice integration for order processing
    this.processPaymentMicroservice();
  }

  
  isValidUPI(upiId: string): boolean {
    if (!upiId) return false;
    
    // Convert to lowercase for checking
    const lowerUpiId = upiId.toLowerCase();
    
    // Check if it ends with any of the valid UPI providers
    return lowerUpiId.endsWith('@gpay') || 
           lowerUpiId.endsWith('@phonepe') || 
           lowerUpiId.endsWith('@paytm');
  }



  /**
   * Process payment using microservices
   */
  private processPaymentMicroservice(): void {
    this.isProcessing = true;
    this.isOverlayVisible = true;
    this.overlayStage = 'loading';
    
    const isPaid = this.selectedPaymentType !== 'cod';
    this.overlayTitle = isPaid ? 'Processing Payment…' : 'Processing Order…';
    this.overlaySubtitle = 'Please wait while we process your order';

    const currentUser = this.authService.getCurrentUser();
    console.log('Current user from AuthService:', currentUser);
    
    // Get user-admin service user ID by email
    this.http.get<any[]>(`${EnvVariables.userServiceUrl}/users`).toPromise()
      .then(users => {
        const matchedUser = users?.find(u => u.email === currentUser?.email);
        const userId = matchedUser?.id?.toString() || '1';
        
        // Cache the user-admin service user ID for cart service
        if (currentUser?.email && matchedUser?.id) {
          sessionStorage.setItem(`userAdminId_${currentUser.email}`, matchedUser.id.toString());
        }
        
        console.log('Auth service user:', currentUser);
        console.log('Matched user-admin service user:', matchedUser);
        console.log('Using user-admin service userId for order:', userId);
        
        // Create order with user-admin service user ID
    
    const orderData = {
      address: {
        fullName: this.shippingAddress!.fullName,
        addressLine1: this.shippingAddress!.addressLine1,
        city: this.shippingAddress!.city,
        state: this.shippingAddress!.state,
        postalCode: this.shippingAddress!.postalCode,
        country: this.shippingAddress!.country || 'USA',
        phone: this.shippingAddress!.phone
      },
      paymentMethod: {
        type: this.selectedPaymentType.toUpperCase(),
        cardNumber: this.selectedPaymentType === 'card' ? (this.payment.cardNumber || '').replace(/\s/g, '') : '',
        cardholderName: this.selectedPaymentType === 'card' ? (this.payment.cardholderName || '') : '',
        expiryMonth: this.selectedPaymentType === 'card' ? (this.payment.expiryMonth || '').padStart(2, '0') : '',
        expiryYear: this.selectedPaymentType === 'card' ? (this.payment.expiryYear || '') : '',
        cvv: this.selectedPaymentType === 'card' ? (this.payment.cvv || this.savedCardCVV || '').replace(/\D/g, '').substring(0, 3) : '',
        upiId: this.selectedPaymentType === 'upi' ? (this.payment.upiId || '') : ''
      }
    };

        console.log('Sending order data:', JSON.stringify(orderData, null, 2));
        console.log('Making API call to:', `${EnvVariables.orderServiceUrl}/orders/from-cart/${userId}`);
        this.orderProcessingService.createOrderFromCart(userId, orderData).subscribe({
          next: (response) => {
            console.log('Order created successfully:', response);
            this.overlayStage = 'success';
            this.overlayTitle = 'Order Placed Successfully';
            this.overlaySubtitle = 'Your order has been confirmed';
            
            this.toastService.showSuccess('Order placed successfully!');
            
            // Clear cart and navigate
            this.cartService.clearCart().subscribe(() => {
              sessionStorage.removeItem('checkoutAddress');
              setTimeout(() => {
                this.router.navigate(['/order-tracking', response.id]);
              }, 2000);
            });
          },
          error: (error) => {
            console.error('Error creating order:', error);
            this.toastService.showError('Failed to place order. Please try again.');
            this.isOverlayVisible = false;
            this.isProcessing = false;
          }
        });
      })
      .catch(error => {
        console.error('Error fetching user-admin users:', error);
        this.toastService.showError('Failed to place order. Please try again.');
        this.isOverlayVisible = false;
        this.isProcessing = false;
      });
  }



  /**
   * Save payment method if it's new and user is logged in
   */
  private savePaymentMethodIfNew(): void {
    if (!this.currentUser) {
      return; // User not logged in, skip saving
    }

    if (this.selectedPaymentType === 'card') {
      const cardDetailsForValidation = {
        cardNumber: this.payment.cardNumber?.replace(/\s/g, '') || '', // Remove spaces for comparison
        expiryMonth: this.payment.expiryMonth || '',
        expiryYear: this.payment.expiryYear || '',
        cardholderName: this.payment.cardholderName || ''
      };
      
      const isExistingCard = this.validateExistingCardDetails(cardDetailsForValidation);
      
      if (!isExistingCard) {
        // Save new card details
        const newCardMethod: UserPaymentMethod = {
          type: 'card',
          cardNumber: cardDetailsForValidation.cardNumber,
          cardholderName: cardDetailsForValidation.cardholderName,
          expiryMonth: cardDetailsForValidation.expiryMonth,
          expiryYear: cardDetailsForValidation.expiryYear
        };
        
        this.saveNewPaymentMethod(newCardMethod);
      }
    } else if (this.selectedPaymentType === 'upi') {
      const existingUPI = this.currentUser.paymentMethods?.find(method => 
        method.type === 'upi' && method.upiId === this.payment.upiId
      );
      
      if (!existingUPI && this.payment.upiId) {
        // Save new UPI details
        const newUPIMethod: UserPaymentMethod = {
          type: 'upi',
          upiId: this.payment.upiId
        };
        
        this.saveNewPaymentMethod(newUPIMethod);
      }
    }
    // COD doesn't need to be saved as it's not a reusable payment method
  }









  /**
   * Handle payment type change
   */
  onPaymentTypeChange(type: PaymentType): void {
    console.log('Payment type changed to:', type);
   
    
    this.selectedPaymentType = type;
    
    // Clear CVV when switching types
    this.savedCardCVV = '';
    
    // Reset form when switching payment types
    this.paymentForm.reset();
    
    // Check if the currently selected payment method matches the new payment type
    if (this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
      const selectedMethod = this.userPaymentMethods.find(method => method.id === this.selectedPaymentMethodId);
      
      if (selectedMethod && selectedMethod.type !== type) {
        // The selected method doesn't match the new payment type
        // Check if there's a saved method of the new type
        const matchingMethod = this.userPaymentMethods.find(method => method.type === type);
        
        if (matchingMethod) {
          // Switch to the matching method of the new type
          this.selectedPaymentMethodId = matchingMethod.id || '';
          this.useNewPaymentMethod = false;
          console.log('Switched to matching payment method:', matchingMethod.id);
        } else {
          // No saved method of the new type, switch to "Add New"
          this.selectedPaymentMethodId = 'new';
          this.useNewPaymentMethod = true;
          console.log('No matching saved method, switched to Add New');
        }
      }
    }
    
    // Update CVV input visibility - only show for cards
    if (type === 'card' && !this.useNewPaymentMethod && this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
      this.showCVVInput = true;
    } else {
      this.showCVVInput = false;
    }
    
    // Update payment object and clear irrelevant fields based on type
    this.payment.type = type;
    
    if (type === 'upi') {
      // Clear all card-related fields for UPI
      this.payment.cardNumber = '';
      this.payment.expiryMonth = '';
      this.payment.expiryYear = '';
      this.payment.cvv = '';
      this.payment.cardholderName = '';
      this.payment.upiId = '';
      
      // If using existing UPI method, load only UPI details
      if (!this.useNewPaymentMethod && this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
        const selectedMethod = this.userPaymentMethods.find(method => method.id === this.selectedPaymentMethodId);
        if (selectedMethod && selectedMethod.type === 'upi') {
          this.payment.upiId = selectedMethod.upiId || '';
        }
      }
    } else if (type === 'card') {
      // Clear UPI field for cards
      this.payment.upiId = '';
      
      // If using existing card method, load card details
      if (!this.useNewPaymentMethod && this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
        this.loadSelectedPaymentMethod();
      }
    }
    
    console.log('Payment object after type change:', this.payment);
    console.log('Current state - selectedPaymentMethodId:', this.selectedPaymentMethodId, 'useNewPaymentMethod:', this.useNewPaymentMethod);
    
    
  }

  // Angular-way navigation to order tracking
  private navigateToOrderTracking(orderNumber: string): void {
            console.log('Starting Angular navigation to order tracking');
    
    this.ngZone.run(() => {
      // Method 1: Try navigateByUrl with explicit options
      this.router.navigateByUrl(`/order-tracking/${orderNumber}`, {
        skipLocationChange: false,
        replaceUrl: false
      }).then((success) => {
        if (success) {
          console.log('Navigation successful via navigateByUrl');
          return Promise.resolve(true);
        } else {
          console.log('navigateByUrl returned false, trying navigate method');
          // Method 2: Fallback to navigate with array
          return this.router.navigate(['/order-tracking', orderNumber]);
        }
      }).catch((error) => {
        console.error('navigateByUrl failed:', error);
        // Method 3: Final fallback
        this.router.navigate(['/order-tracking', orderNumber]).then((success) => {
          console.log('🔄 Fallback navigation result:', success);
        });
      });
    });
  }

  // Simple card number formatting
  formatCardNumber(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    value = value.replace(/(\d{4})(?=\d)/g, '$1 ');
    this.paymentForm.patchValue({ cardNumber: value });
  }

  // Simple CVV formatting  
  formatCVV(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    value = value.substring(0, 4); // Max 4 digits
    this.paymentForm.patchValue({ cvv: value });
  }

  goBackToAddress(): void {
    this.router.navigate(['/checkout/address']);
  }

  formatPrice(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  // Helper to mark all form controls as touched
  private markFormGroupTouched(formGroup: any) {
    Object.values(formGroup.controls).forEach((control: any) => {
      control.markAsTouched();
      if (control.controls) {
        this.markFormGroupTouched(control);
      }
    });
  }

  // Validation methods
  private validatePaymentForm(): boolean {
    this.clearFormErrors();
    let isValid = true;

    if (this.selectedPaymentType === 'card') {
      // Card validation
      if (!this.payment.cardholderName || this.payment.cardholderName.trim().length < 2) {
        this.formErrors['cardholderName'] = 'Cardholder name must be at least 2 characters';
        isValid = false;
      }

      if (!this.payment.cardNumber || !this.validateCardNumber(this.payment.cardNumber)) {
        this.formErrors['cardNumber'] = 'Card number must be exactly 16 digits';
        isValid = false;
      }

      if (!this.payment.expiryMonth || !this.payment.expiryYear) {
        this.formErrors['expiry'] = 'Please select expiry month and year';
        isValid = false;
      }

      if (this.payment.expiryMonth && this.payment.expiryYear) {
        if (!this.validateExpiryDate(this.payment.expiryMonth, this.payment.expiryYear)) {
          this.formErrors['expiry'] = 'Card has expired';
          isValid = false;
        }
      }

      if (!this.payment.cvv || !this.validateCVV(this.payment.cvv)) {
        this.formErrors['cvv'] = 'Please enter a valid CVV';
        isValid = false;
      }
    } else if (this.selectedPaymentType === 'upi') {
      // UPI validation
      if (!this.payment.upiId || !this.validateUPI(this.payment.upiId)) {
        this.formErrors['upiId'] = 'Please enter a valid UPI ID';
        isValid = false;
      }
    }

    // Validate saved card CVV if using saved method
    if (!this.useNewPaymentMethod && this.selectedPaymentType === 'card') {
      if (!this.savedCardCVV || !this.validateCVV(this.savedCardCVV)) {
        this.formErrors['savedCardCVV'] = 'Please enter a valid CVV';
        isValid = false;
      }
    }

    return isValid;
  }

  private validateCardNumber(cardNumber: string): boolean {
    if (!cardNumber) return false;
    
    console.log('Validating card number:', cardNumber);
    
    // Remove spaces and dashes
    const cleanNumber = cardNumber.replace(/\s+/g, '').replace(/-/g, '');
    console.log('Clean card number:', cleanNumber);
    
    // Check if exactly 16 digits
    if (cleanNumber.length !== 16) {
      console.log('Card number must be exactly 16 digits, got:', cleanNumber.length);
      return false;
    }

    // Check if all characters are digits
    if (!/^\d+$/.test(cleanNumber)) {
      console.log('Card number contains non-digits');
      return false;
    }

    console.log('Card number validation passed: 16 digits, all numbers');
    return true;
  }

  private validateExpiryDate(month: string, year: string): boolean {
    const currentDate = new Date();
    const currentYear = currentDate.getFullYear();
    const currentMonth = currentDate.getMonth() + 1; // getMonth() returns 0-11
    
    const expiryYear = parseInt(year);
    const expiryMonth = parseInt(month);
    
    if (expiryYear < currentYear) {
      return false;
    }
    
    if (expiryYear === currentYear && expiryMonth < currentMonth) {
      return false;
    }
    
    return true;
  }

  private validateCVV(cvv: string): boolean {
    // CVV should be 3-4 digits
    return /^[0-9]{3,4}$/.test(cvv);
  }

  private validateUPI(upiId: string): boolean {
    // UPI format: username@bank or username@upi
    return /^[a-zA-Z0-9._-]+@[a-zA-Z]{3,}$/.test(upiId);
  }

  // Check if field has error
  hasError(fieldName: string): boolean {
    return !!this.formErrors[fieldName];
  }

  // Get error message for field
  getErrorMessage(fieldName: string): string {
    return this.formErrors[fieldName] || '';
  }

  // Clear form errors
  private clearFormErrors(): void {
    this.formErrors = {};
  }

  // Sync form values with payment object
  private syncPaymentObject(): void {
    if (this.useNewPaymentMethod) {
      this.payment = {
        type: this.selectedPaymentType,
        cardNumber: this.paymentForm.get('cardNumber')?.value || '',
        expiryMonth: this.paymentForm.get('expiryMonth')?.value || '',
        expiryYear: this.paymentForm.get('expiryYear')?.value || '',
        cvv: this.paymentForm.get('cvv')?.value || '',
        cardholderName: this.paymentForm.get('cardholderName')?.value || '',
        upiId: this.paymentForm.get('upiId')?.value || ''
      };
    }
  }

} 

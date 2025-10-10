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
    type: 'card' as PaymentType,
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

    const savedAddress = sessionStorage.getItem('checkoutAddress');
    if (!savedAddress) {
      this.router.navigate(['/checkout/address']);
      return;
    }
    
    this.shippingAddress = JSON.parse(savedAddress);
  }

  private loadCurrentUser(): void {
    this.currentUser = null;
    this.userPaymentMethods = [];
    this.selectedPaymentType = 'card';
    this.selectedPaymentMethodId = 'new';
    this.useNewPaymentMethod = true;
    this.showCVVInput = false;
  }

  private loadSelectedPaymentMethod(): void {
    if (this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
      const selectedMethod = this.userPaymentMethods.find(method => method.id === this.selectedPaymentMethodId);
      if (selectedMethod) {
        if (this.selectedPaymentType === 'card' && selectedMethod.type === 'card') {
          this.payment = {
            type: 'card',
            cardNumber: selectedMethod.cardNumber || '',
            expiryMonth: selectedMethod.expiryMonth || '',
            expiryYear: selectedMethod.expiryYear || '',
            cvv: '', 
            cardholderName: selectedMethod.cardholderName || '',
            upiId: ''
          };
          
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

  getSelectedPaymentMethod(): UserPaymentMethod | undefined {
    if (this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
      return this.userPaymentMethods.find(method => method.id === this.selectedPaymentMethodId);
    }
    return undefined;
  }

  getSelectedCardLastFour(): string {
    const method = this.getSelectedPaymentMethod();
    if (method && method.type === 'card' && method.cardNumber) {
      return method.cardNumber.slice(-4);
    }
    return '****';
  }

  onSavedCardCVVInput(event: any): void {
    this.savedCardCVV = event.target.value;
  }

  private validateExistingCardDetails(cardDetails: any): boolean {
    if (!this.currentUser?.paymentMethods) {
      return false;
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

  private saveNewPaymentMethod(paymentMethod: UserPaymentMethod): void {
    if (!this.currentUser) {
      return;
    }

    if (!this.currentUser.paymentMethods) {
      this.currentUser.paymentMethods = [];
    }

    const newPaymentMethod: UserPaymentMethod = {
      ...paymentMethod,
      id: Date.now().toString(),
      isDefault: this.currentUser.paymentMethods.length === 0 // First payment method is default
    };

    this.currentUser.paymentMethods.push(newPaymentMethod);

    console.log('Payment method saving disabled');
  }

  onSubmit(): void {
    if (this.selectedPaymentType === 'cod') {
      this.processPaymentMicroservice();
      return;
    }

    if (this.selectedPaymentType === 'card' || this.selectedPaymentType === 'upi') {
      if (this.useNewPaymentMethod || this.userPaymentMethods.length === 0) {
        if (this.paymentForm.invalid) {
          this.markFormGroupTouched(this.paymentForm);
          return;
        }

        this.syncPaymentObject();

        if (!this.validatePaymentForm()) {
          this.toastService.showError('Please fix the validation errors');
          return;
        }

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
        if (!this.selectedPaymentMethodId || this.selectedPaymentMethodId === 'new') {
          this.toastService.showError('Please select a payment method');
          return;
        }

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
          cvv: '', 
          cardholderName: selectedMethod.cardholderName || '',
          upiId: selectedMethod.upiId || ''
        };

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

    this.processPaymentMicroservice();
  }

  
  isValidUPI(upiId: string): boolean {
    if (!upiId) return false;
    
    const lowerUpiId = upiId.toLowerCase();
    
    return lowerUpiId.endsWith('@gpay') || 
           lowerUpiId.endsWith('@phonepe') || 
           lowerUpiId.endsWith('@paytm');
  }



  private processPaymentMicroservice(): void {
    this.isProcessing = true;
    this.isOverlayVisible = true;
    this.overlayStage = 'loading';
    
    const isPaid = this.selectedPaymentType !== 'cod';
    this.overlayTitle = isPaid ? 'Processing Payment…' : 'Processing Order…';
    this.overlaySubtitle = 'Please wait while we process your order';

    const currentUser = this.authService.getCurrentUser();
    console.log('Current user from AuthService:', currentUser);
    
    this.http.get<any[]>(`${EnvVariables.userServiceUrl}/users`).toPromise()
      .then(users => {
        const matchedUser = users?.find(u => u.email === currentUser?.email);
        const userId = matchedUser?.id?.toString() || '1';
        
        if (currentUser?.email && matchedUser?.id) {
          sessionStorage.setItem(`userAdminId_${currentUser.email}`, matchedUser.id.toString());
        }
        
        console.log('Auth service user:', currentUser);
        console.log('Matched user-admin service user:', matchedUser);
        console.log('Using user-admin service userId for order:', userId);
        
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



  private savePaymentMethodIfNew(): void {
    if (!this.currentUser) {
      return; 
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
  }









  onPaymentTypeChange(type: PaymentType): void {
    console.log('Payment type changed to:', type);
   
    
    this.selectedPaymentType = type;
    
    this.savedCardCVV = '';
    
    this.paymentForm.reset();
    
    if (this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
      const selectedMethod = this.userPaymentMethods.find(method => method.id === this.selectedPaymentMethodId);
      
      if (selectedMethod && selectedMethod.type !== type) {
        const matchingMethod = this.userPaymentMethods.find(method => method.type === type);
        
        if (matchingMethod) {
          this.selectedPaymentMethodId = matchingMethod.id || '';
          this.useNewPaymentMethod = false;
          console.log('Switched to matching payment method:', matchingMethod.id);
        } else {
          this.selectedPaymentMethodId = 'new';
          this.useNewPaymentMethod = true;
          console.log('No matching saved method, switched to Add New');
        }
      }
    }
    
    if (type === 'card' && !this.useNewPaymentMethod && this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
      this.showCVVInput = true;
    } else {
      this.showCVVInput = false;
    }
    
    this.payment.type = type;
    
    if (type === 'upi') {
      this.payment.cardNumber = '';
      this.payment.expiryMonth = '';
      this.payment.expiryYear = '';
      this.payment.cvv = '';
      this.payment.cardholderName = '';
      this.payment.upiId = '';
      
      if (!this.useNewPaymentMethod && this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
        const selectedMethod = this.userPaymentMethods.find(method => method.id === this.selectedPaymentMethodId);
        if (selectedMethod && selectedMethod.type === 'upi') {
          this.payment.upiId = selectedMethod.upiId || '';
        }
      }
    } else if (type === 'card') {
      // Clear UPI field for cards
      this.payment.upiId = '';
      
      if (!this.useNewPaymentMethod && this.selectedPaymentMethodId && this.selectedPaymentMethodId !== 'new') {
        this.loadSelectedPaymentMethod();
      }
    }
    
    console.log('Payment object after type change:', this.payment);
    console.log('Current state - selectedPaymentMethodId:', this.selectedPaymentMethodId, 'useNewPaymentMethod:', this.useNewPaymentMethod);
    
    
  }

  private navigateToOrderTracking(orderNumber: string): void {
            console.log('Starting Angular navigation to order tracking');
    
    this.ngZone.run(() => {
      this.router.navigateByUrl(`/order-tracking/${orderNumber}`, {
        skipLocationChange: false,
        replaceUrl: false
      }).then((success) => {
        if (success) {
          console.log('Navigation successful via navigateByUrl');
          return Promise.resolve(true);
        } else {
          console.log('navigateByUrl returned false, trying navigate method');
          return this.router.navigate(['/order-tracking', orderNumber]);
        }
      }).catch((error) => {
        console.error('navigateByUrl failed:', error);
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
    value = value.substring(0, 4); 
    this.paymentForm.patchValue({ cvv: value });
  }

  goBackToAddress(): void {
    this.router.navigate(['/checkout/address']);
  }

  formatPrice(price: number): string {
    return `$${price.toFixed(2)}`;
  }

  private markFormGroupTouched(formGroup: any) {
    Object.values(formGroup.controls).forEach((control: any) => {
      control.markAsTouched();
      if (control.controls) {
        this.markFormGroupTouched(control);
      }
    });
  }

  private validatePaymentForm(): boolean {
    this.clearFormErrors();
    let isValid = true;

    if (this.selectedPaymentType === 'card') {
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
    
    const cleanNumber = cardNumber.replace(/\s+/g, '').replace(/-/g, '');
    console.log('Clean card number:', cleanNumber);
    
    if (cleanNumber.length !== 16) {
      console.log('Card number must be exactly 16 digits, got:', cleanNumber.length);
      return false;
    }

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
    return /^[0-9]{3,4}$/.test(cvv);
  }

  private validateUPI(upiId: string): boolean {
    return /^[a-zA-Z0-9._-]+@[a-zA-Z]{3,}$/.test(upiId);
  }

  hasError(fieldName: string): boolean {
    return !!this.formErrors[fieldName];
  }

  getErrorMessage(fieldName: string): string {
    return this.formErrors[fieldName] || '';
  }

  private clearFormErrors(): void {
    this.formErrors = {};
  }

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

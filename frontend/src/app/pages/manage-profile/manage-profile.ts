import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { ManageProfileService } from '../../services/manage-profile.service';
import { ToastService } from '../../services/toast-service';
import { User, UserAddress, UserPaymentMethod } from '../../models/user';
import { AddressStateService } from '../../services/address-state.service';

@Component({
  selector: 'app-manage-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './manage-profile.html',
  styleUrls: ['./manage-profile.css']
})
export class ManageProfile implements OnInit {
  user: User = {} as User;
  loading = false;
  
  // Active section
  activeSection = 'account';
  
  // Edit modes
  editingAccount = false;
  editingAddress = false;
  editingPayment = false;
  
  // Temporary edit data
  tempAccountData: Partial<User> = {};
  tempAddressData: UserAddress = {} as UserAddress;
  tempPaymentData: UserPaymentMethod = {} as UserPaymentMethod;
  

  
  // Selected items for editing
  selectedAddress: UserAddress | null = null;
  selectedPayment: UserPaymentMethod | null = null;
  


  // Reactive form for address editing
  addressForm: FormGroup = new FormGroup({
    fullName: new FormControl('', [Validators.required, Validators.minLength(3), Validators.maxLength(100), Validators.pattern(/^[a-zA-Z\s.]+$/)]),
    phone: new FormControl('', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]),
    addressLine1: new FormControl('', [Validators.required, Validators.minLength(5), Validators.maxLength(200)]),
    addressLine2: new FormControl('', [Validators.maxLength(200)]),
    city: new FormControl('', [Validators.required, Validators.minLength(2), Validators.maxLength(100), Validators.pattern(/^[a-zA-Z\s]+$/)]),
    state: new FormControl('', [Validators.required, Validators.minLength(2), Validators.maxLength(100), Validators.pattern(/^[a-zA-Z\s]+$/)]),
    postalCode: new FormControl('', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]),
    country: new FormControl('', [Validators.required, Validators.minLength(2), Validators.maxLength(100), Validators.pattern(/^[a-zA-Z\s]+$/)])
  });

  // Reactive form for account editing
  accountForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(3)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    phone: new FormControl('', [Validators.pattern(/^[0-9]{10}$/)]),
    dateOfBirth: new FormControl(''),
    gender: new FormControl('')
  });

  // Reactive form for payment method editing
  paymentForm: FormGroup = new FormGroup({
    type: new FormControl('', [Validators.required]),
    cardNumber: new FormControl(''),
    cardholderName: new FormControl(''),
    expiryMonth: new FormControl(''),
    expiryYear: new FormControl(''),
    upiId: new FormControl('')
  });

  constructor(
    private manageProfileService: ManageProfileService,
    private toastService: ToastService,
    private router: Router,
    private route: ActivatedRoute,
    private addressState: AddressStateService
  ) {}

  ngOnInit(): void {
    this.loadUserProfile();
    
    // Handle URL fragment to open specific section
    this.route.fragment.subscribe(fragment => {
      if (fragment) {
        this.setActiveSection(fragment);
      }
    });
  }

  loadUserProfile(): void {
    this.loading = true;
    this.manageProfileService.getUserProfile().subscribe({
      next: (user) => {
        const loggedInUser = JSON.parse(localStorage.getItem('currentUser') || '{}');
        this.user = user;
        this.loading = false;
        
        // Always use logged-in user's email for display
        this.accountForm.patchValue({
          name: this.user.name,
          email: loggedInUser.email || this.user.email,
          phone: this.user.phone || '',
          dateOfBirth: this.user.dateOfBirth || '',
          gender: this.user.gender || ''
        });
        
        // Seed address form with default or first address if exists
        const addr = (this.user.addresses && this.user.addresses.length > 0) ? this.user.addresses[0] : null;
        if (addr) {
          this.addressForm.patchValue({
            fullName: addr.fullName || this.user.name,
            phone: addr.phone || this.user.phone || '',
            addressLine1: addr.addressLine1,
            addressLine2: addr.addressLine2 || '',
            city: addr.city,
            state: addr.state,
            postalCode: addr.postalCode,
            country: addr.country
          });
          // Broadcast address to header on load as well
          this.addressState.setAddressInfo({ city: addr.city, postalCode: addr.postalCode });
        } else {
          this.addressState.setAddressInfo(null);
        }
      },
      error: (error) => {
        this.toastService.showError('Error loading profile: ' + error.message);
        this.loading = false;
      }
    });
  }

  // Account Information Methods
  startEditingAccount(): void {
    this.editingAccount = true;
    // Always use logged-in user's email when editing
    const loggedInUser = JSON.parse(localStorage.getItem('currentUser') || '{}');
    this.accountForm.patchValue({
      name: this.user.name,
      email: loggedInUser.email || this.user.email,
      phone: this.user.phone || '',
      dateOfBirth: this.user.dateOfBirth || '',
      gender: this.user.gender || ''
    });
  }

  saveAccountInfo(): void {
    if (this.accountForm.invalid) {
      this.accountForm.markAllAsTouched();
      this.toastService.showError('Please correct the errors in the profile form');
      return;
    }

    const payload = this.accountForm.value as Partial<User>;

    this.loading = true;
    this.manageProfileService.updateAccountInfo(payload).subscribe({
      next: (updatedUser) => {
        this.user = { ...this.user, ...payload } as User;
        this.editingAccount = false;
        this.toastService.showSuccess('Account information updated successfully');
        this.loading = false;
      },
      error: (error) => {
        this.toastService.showError('Error updating account: ' + error.message);
        this.loading = false;
      }
    });
  }

  cancelEditingAccount(): void {
    this.editingAccount = false;
  }

  // Address Methods
  startEditingAddress(address?: UserAddress): void {
    this.editingAddress = true;
    if (address) {
      this.selectedAddress = address;
      this.addressForm.reset({
        fullName: address.fullName || this.user.name,
        phone: address.phone || this.user.phone || '',
        addressLine1: address.addressLine1,
        addressLine2: address.addressLine2 || '',
        city: address.city,
        state: address.state,
        postalCode: address.postalCode,
        country: address.country,
      });
    } else {
      this.selectedAddress = null;
      this.addressForm.reset({
        fullName: this.user.name,
        phone: this.user.phone || '',
        addressLine1: '',
        addressLine2: '',
        city: '',
        state: '',
        postalCode: '',
        country: ''
      });
    }
  }

  saveAddress(): void {
    if (this.addressForm.invalid) {
      this.addressForm.markAllAsTouched();
      this.toastService.showError('Please correct the errors in the address form');
      return;
    }

    const formValue = this.addressForm.value;
    const updated: UserAddress = {
      ...(this.selectedAddress ? { id: this.selectedAddress.id } : {}),
      fullName: formValue.fullName,
      addressLine1: formValue.addressLine1,
      addressLine2: formValue.addressLine2,
      city: formValue.city,
      state: formValue.state,
      postalCode: formValue.postalCode,
      country: formValue.country,
      phone: formValue.phone
    } as UserAddress;

    this.loading = true;
    this.manageProfileService.updateAddress(updated).subscribe({
      next: (updatedUser) => {
        this.loadUserProfile();
        this.editingAddress = false;
        this.selectedAddress = null;
        this.toastService.showSuccess('Address updated successfully');
        this.loading = false;
        // Broadcast to header immediately
        this.addressState.setAddressInfo({ city: updated.city, postalCode: updated.postalCode });
      },
      error: (error) => {
        this.toastService.showError('Error updating address: ' + error.message);
        this.loading = false;
      }
    });
  }

  cancelEditingAddress(): void {
    this.editingAddress = false;
    this.selectedAddress = null;
  }

  removeAddress(addressId: string): void {
    if (confirm('Are you sure you want to remove this address?')) {
      this.loading = true;
      this.manageProfileService.removeAddress(addressId).subscribe({
        next: () => {
          this.loadUserProfile();
          this.toastService.showSuccess('Address removed successfully');
          this.loading = false;
          // If we removed the only address, clear the header display
          const remaining = (this.user.addresses || []).filter(a => a.id !== addressId);
          if (remaining.length === 0) {
            this.addressState.setAddressInfo(null);
          }
        },
        error: (error) => {
          this.toastService.showError('Error removing address: ' + error.message);
          this.loading = false;
        }
      });
    }
  }

  // Payment Methods
  startEditingPayment(payment?: UserPaymentMethod): void {
    this.editingPayment = true;
    if (payment) {
      this.selectedPayment = payment;
      this.paymentForm.patchValue({
        type: payment.type,
        cardNumber: payment.cardNumber || '',
        cardholderName: payment.cardholderName || '',
        expiryMonth: payment.expiryMonth || '',
        expiryYear: payment.expiryYear || '',
        upiId: payment.upiId || ''
      });
    } else {
      this.selectedPayment = null;
      this.paymentForm.reset({
        type: '',
        cardNumber: '',
        cardholderName: '',
        expiryMonth: '',
        expiryYear: '',
        upiId: ''
      });
    }
    this.updatePaymentValidators();
  }

  savePaymentMethod(): void {
    if (this.paymentForm.invalid) {
      this.paymentForm.markAllAsTouched();
      this.toastService.showError('Please correct the errors in the payment form');
      return;
    }

    const formValue = this.paymentForm.value;
    const paymentData: UserPaymentMethod = {
      ...(this.selectedPayment ? { id: this.selectedPayment.id } : {}),
      type: formValue.type,
      cardNumber: formValue.type === 'card' ? formValue.cardNumber : null,
      cardholderName: formValue.type === 'card' ? formValue.cardholderName : null,
      expiryMonth: formValue.type === 'card' ? formValue.expiryMonth : null,
      expiryYear: formValue.type === 'card' ? formValue.expiryYear : null,
      upiId: formValue.type === 'upi' ? formValue.upiId : null
    } as UserPaymentMethod;

    this.loading = true;
    this.manageProfileService.updatePaymentMethod(paymentData).subscribe({
      next: () => {
        this.loadUserProfile();
        this.editingPayment = false;
        this.selectedPayment = null;
        this.toastService.showSuccess('Payment method saved successfully');
        this.loading = false;
      },
      error: (error) => {
        this.toastService.showError('Error saving payment method: ' + error.message);
        this.loading = false;
      }
    });
  }

  updatePaymentValidators(): void {
    const typeControl = this.paymentForm.get('type');
    const cardNumberControl = this.paymentForm.get('cardNumber');
    const cardholderNameControl = this.paymentForm.get('cardholderName');
    const expiryMonthControl = this.paymentForm.get('expiryMonth');
    const expiryYearControl = this.paymentForm.get('expiryYear');
    const upiIdControl = this.paymentForm.get('upiId');

    // Clear all validators first
    cardNumberControl?.clearValidators();
    cardholderNameControl?.clearValidators();
    expiryMonthControl?.clearValidators();
    expiryYearControl?.clearValidators();
    upiIdControl?.clearValidators();

    if (typeControl?.value === 'card') {
      cardNumberControl?.setValidators([Validators.required, Validators.pattern(/^[0-9\s]{13,19}$/)]);
      cardholderNameControl?.setValidators([Validators.required, Validators.minLength(2)]);
      expiryMonthControl?.setValidators([Validators.required]);
      expiryYearControl?.setValidators([Validators.required]);
    } else if (typeControl?.value === 'upi') {
      upiIdControl?.setValidators([Validators.required, Validators.pattern(/^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$/)]);
    }

    // Update validity
    cardNumberControl?.updateValueAndValidity();
    cardholderNameControl?.updateValueAndValidity();
    expiryMonthControl?.updateValueAndValidity();
    expiryYearControl?.updateValueAndValidity();
    upiIdControl?.updateValueAndValidity();
  }

  onPaymentTypeChange(): void {
    this.updatePaymentValidators();
  }



  onCardNumberInput(event: any): void {
    // Remove all non-digits and limit to 16 digits
    let value = event.target.value.replace(/\D/g, '');
    if (value.length > 16) {
      value = value.substring(0, 16);
    }
    // Format with spaces
    value = value.replace(/(\d{4})(?=\d)/g, '$1 ');
    this.paymentForm.patchValue({ cardNumber: value });
    event.target.value = value;
  }

  cancelEditingPayment(): void {
    this.editingPayment = false;
    this.selectedPayment = null;
    this.paymentForm.reset();
  }

  removePaymentMethod(paymentId: string): void {
    if (confirm('Are you sure you want to remove this payment method?')) {
      this.loading = true;
      this.manageProfileService.removePaymentMethod(paymentId).subscribe({
        next: () => {
          this.loadUserProfile();
          this.toastService.showSuccess('Payment method removed successfully');
          this.loading = false;
        },
        error: (error) => {
          this.toastService.showError('Error removing payment method: ' + error.message);
          this.loading = false;
        }
      });
    }
  }





  // Utility methods
  getMaskedCardNumber(cardNumber: string): string {
    if (!cardNumber || cardNumber.length < 4) return '****';
    return '**** **** **** ' + cardNumber.slice(-4);
  }

  getCurrentYear(): number {
    return new Date().getFullYear();
  }

  getYearRange(): number[] {
    const currentYear = this.getCurrentYear();
    const years = [];
    for (let i = currentYear; i <= currentYear + 20; i++) {
      years.push(i);
    }
    return years;
  }

  getLoggedInUserEmail(): string {
    const loggedInUser = JSON.parse(localStorage.getItem('currentUser') || '{}');
    return loggedInUser?.email || '';
  }

  // Navigation methods
  setActiveSection(section: string): void {
    // Validate section is one of the allowed sections
    const validSections = ['account', 'address', 'payment'];
    if (validSections.includes(section)) {
      this.activeSection = section;
    } else {
      // Default to account section if invalid section provided
      this.activeSection = 'account';
    }
    
    // Cancel any ongoing edits when switching sections
    this.cancelEditingAccount();
    this.cancelEditingAddress();
    this.cancelEditingPayment();


  }
} 
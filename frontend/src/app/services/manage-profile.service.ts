import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { EnvVariables } from '../env/env-variables';
import { AuthService } from './auth.service';
import { User, UserAddress, UserPaymentMethod, PasswordResetRequest } from '../models/user';

@Injectable({
  providedIn: 'root'
})
export class ManageProfileService {

  constructor(private http: HttpClient, private authService: AuthService) { }

  private getCurrentUserId(): string | null {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.email) return null;
    
    // Get user-admin service user ID from cache
    const cachedUserId = sessionStorage.getItem(`userAdminId_${currentUser.email}`);
    if (cachedUserId) {
      return cachedUserId;
    }
    
    // Fallback to auth service ID
    return currentUser.id ? currentUser.id.toString() : null;
  }

  // Get current user data
  getUserProfile(): Observable<User> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not logged in'));
    }

    return forkJoin({
      user: this.http.get<any>(`${EnvVariables.userServiceUrl}/users/profile/${userId}`),
      addresses: this.http.get<UserAddress[]>(`${EnvVariables.userServiceUrl}/users/${userId}/addresses`).pipe(catchError(() => of([]))),
      paymentMethods: this.http.get<UserPaymentMethod[]>(`${EnvVariables.userServiceUrl}/users/${userId}/payment-methods`).pipe(catchError(() => of([])))
    }).pipe(
      map(({ user, addresses, paymentMethods }) => ({
        ...user,
        addresses,
        paymentMethods
      } as User)),
      catchError(error => throwError(() => error))
    );
  }

  // Update account information
  updateAccountInfo(accountData: Partial<User>): Observable<User> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not logged in'));
    }

    // Directly update using the user-admin service user ID
    return this.http.put<User>(`${EnvVariables.userServiceUrl}/users/${userId}`, accountData).pipe(
      catchError(error => throwError(() => error))
    );
  }

  // Add or update address
  updateAddress(addressData: UserAddress): Observable<any> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not logged in'));
    }

    if (addressData.id) {
      // Update existing address
      return this.http.put(`${EnvVariables.userServiceUrl}/users/${userId}/addresses/${addressData.id}`, addressData)
        .pipe(catchError(error => throwError(() => error)));
    } else {
      // Add new address
      return this.http.post(`${EnvVariables.userServiceUrl}/users/${userId}/addresses`, addressData)
        .pipe(catchError(error => throwError(() => error)));
    }
  }

  // Remove address
  removeAddress(addressId: string): Observable<any> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not logged in'));
    }

    return this.http.delete(`${EnvVariables.userServiceUrl}/users/${userId}/addresses/${addressId}`)
      .pipe(catchError(error => throwError(() => error)));
  }

  // Add or update payment method
  updatePaymentMethod(paymentData: UserPaymentMethod): Observable<User> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not logged in'));
    }

    if (paymentData.id) {
      // Update existing payment method
      return this.http.put<UserPaymentMethod>(`${EnvVariables.userServiceUrl}/users/${userId}/payment-methods/${paymentData.id}`, paymentData)
        .pipe(
          switchMap(() => this.getUserProfile()),
          catchError(error => throwError(() => error))
        );
    } else {
      // Add new payment method
      return this.http.post<UserPaymentMethod>(`${EnvVariables.userServiceUrl}/users/${userId}/payment-methods`, paymentData)
        .pipe(
          switchMap(() => this.getUserProfile()),
          catchError(error => throwError(() => error))
        );
    }
  }

  // Remove payment method
  removePaymentMethod(paymentMethodId: string): Observable<User> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not logged in'));
    }

    return this.http.delete(`${EnvVariables.userServiceUrl}/users/${userId}/payment-methods/${paymentMethodId}`)
      .pipe(
        switchMap(() => this.getUserProfile()),
        catchError(error => throwError(() => error))
      );
  }

  // Reset password
  resetPassword(passwordData: PasswordResetRequest): Observable<any> {
    return this.http.post(`${EnvVariables.userServiceUrl}/auth/reset-password`, passwordData).pipe(
      map(() => ({ success: true, message: 'Password updated successfully' })),
      catchError(error => throwError(() => error))
    );
  }

  // Delete account (with cascade delete of related data)
  deleteAccount(): Observable<any> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not logged in'));
    }

    return this.http.delete(`${EnvVariables.userServiceUrl}/users/profile`).pipe(
      map(() => {
        this.authService.clearCurrentUser();
        return { success: true, message: 'Account deleted successfully' };
      }),
      catchError(error => throwError(() => error))
    );
  }
}
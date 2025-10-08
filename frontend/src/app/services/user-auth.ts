import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, forkJoin, map, Observable, ObservedValuesFromArray, switchMap, throwError } from 'rxjs';
import { EnvVariables } from '../env/env-variables';
import { Md5 } from 'ts-md5';
import { CartService } from './cart-service';

@Injectable({
  providedIn: 'root'
})
export class UserAuth {

  constructor(
    private http: HttpClient,
    private cartService: CartService
  ) {}

  register(registerData: any): Observable<any> {
    const { confirmPassword, ...cleanedRegisterData } = registerData;

    const registerPostData = {
      username: cleanedRegisterData.name,
      email: cleanedRegisterData.email,
      password: cleanedRegisterData.password,
      confirmPassword: cleanedRegisterData.password
    };

    return this.http.post(`${EnvVariables.userServiceUrl}/users/register`, registerPostData).pipe(
      map((response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('userId', response.id.toString());
          localStorage.setItem('userEmail', response.email);
          localStorage.setItem('isLoggedIn', 'true');
          
          // Refresh cart to load user-specific cart items
          this.cartService.refreshCart();
        }
        return response;
      }),
      catchError(err => throwError(() => new Error(err.error?.message || 'Registration failed')))
    );
  }

  login(loginData: any): Observable<any> {
    const loginPostData = {
      email: loginData.name, // Can be username or email
      password: loginData.password
    };

    return this.http.post(`${EnvVariables.userServiceUrl}/users/login`, loginPostData).pipe(
      map((response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('userId', response.id.toString());
          localStorage.setItem('userEmail', response.email);
          localStorage.setItem('isLoggedIn', 'true');
          
          // Refresh cart to load user-specific cart items
          this.cartService.refreshCart();
        }
        return response;
      }),
      catchError(err => throwError(() => new Error(err.error?.message || 'Login failed')))
    );
  }

  logout(): void {
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('userId');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('token');
    
    // Clear cart on logout
    this.cartService.refreshCart();
  }

  isLoggedIn(): boolean {
    return localStorage.getItem('isLoggedIn') === 'true';
  }

  getCurrentUserEmail(): string | null {
    return localStorage.getItem('userEmail');
  }

  getCurrentUserId(): number | null {
    const userId = localStorage.getItem('userId');
    return userId ? parseInt(userId) : null;
  }
}
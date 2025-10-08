import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { Router } from '@angular/router';
import { EnvVariables } from '../env/env-variables';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  name: string;
  email: string;
  role: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authUrl = `${EnvVariables.authServiceUrl}`;
  private userUrl = `${EnvVariables.userServiceUrl}`;
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    // Load user from localStorage on service initialization only if token exists
    const storedUser = localStorage.getItem('currentUser');
    const token = localStorage.getItem('token');
    if (storedUser && token) {
      this.currentUserSubject.next(JSON.parse(storedUser));
    } else {
      // Clear any partial data if token is missing
      this.clearCurrentUser();
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/auth/customer/login`, credentials);
  }

  register(userData: RegisterRequest): Observable<any> {
    return this.http.post(`${EnvVariables.userServiceUrl}/users/register`, userData);
  }

  logout(): Observable<any> {
    const token = localStorage.getItem('token');
    if (token) {
      return this.http.post(`${this.authUrl}/auth/logout`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
    }
    return new Observable(observer => {
      observer.next({});
      observer.complete();
    });
  }

  setCurrentUser(user: any): void {
    localStorage.setItem('currentUser', JSON.stringify(user));
    localStorage.setItem('token', user.token);
    this.currentUserSubject.next(user);
    
    // Cache user-admin service user ID for cart operations
    if (user.email) {
      this.cacheUserAdminId(user.email);
    }
  }
  
  private cacheUserAdminId(email: string): void {
    this.http.get<any[]>(`${this.userUrl}/users`).subscribe({
      next: (users) => {
        const matchedUser = users?.find(u => u.email === email);
        if (matchedUser?.id) {
          sessionStorage.setItem(`userAdminId_${email}`, matchedUser.id.toString());
          console.log('Cached user-admin service ID:', matchedUser.id, 'for email:', email);
          
          // Trigger cart migration if needed
          const currentUser = this.getCurrentUser();
          if (currentUser?.id && currentUser.id !== matchedUser.id) {
            console.log('User ID mismatch detected, cart migration may be needed');
          }
        }
      },
      error: (error) => {
        console.error('Failed to cache user-admin service ID:', error);
      }
    });
  }

  getCurrentUser(): any {
    return this.currentUserSubject.value || JSON.parse(localStorage.getItem('currentUser') || 'null');
  }

  clearCurrentUser(): void {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('token');
    localStorage.removeItem('userId'); // Remove any legacy userId
    this.currentUserSubject.next(null);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token') && !!this.getCurrentUser();
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getUserProfile(): Observable<any> {
    const user = this.getCurrentUser();
    if (user?.id) {
      return this.http.get(`${this.userUrl}/users/profile/${user.id}`);
    }
    throw new Error('No user logged in');
  }

  updateUserProfile(userData: any): Observable<any> {
    const user = this.getCurrentUser();
    if (user?.id) {
      return this.http.put(`${this.userUrl}/users/${user.id}`, userData);
    }
    throw new Error('No user logged in');
  }

  resetPassword(passwordData: any): Observable<any> {
    return this.http.post(`${this.userUrl}/users/reset-password`, passwordData);
  }
}
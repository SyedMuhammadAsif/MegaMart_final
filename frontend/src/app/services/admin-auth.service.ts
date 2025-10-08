import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { Router } from '@angular/router';
import { EnvVariables } from '../env/env-variables';

export interface AdminLoginRequest {
  email: string;
  password: string;
}

export interface AdminAuthResponse {
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
export class AdminAuthService {
  private baseUrl = `${EnvVariables.authServiceUrl}`;
  public currentAdminSubject = new BehaviorSubject<any>(null);
  public currentAdmin$ = this.currentAdminSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    // Load admin from localStorage on service initialization only if token exists
    const storedAdmin = localStorage.getItem('currentAdmin');
    const adminToken = localStorage.getItem('adminToken');
    if (storedAdmin && adminToken) {
      this.currentAdminSubject.next(JSON.parse(storedAdmin));
    } else {
      // Clear any partial data if token is missing
      this.clearCurrentAdmin();
    }
  }

  login(credentials: AdminLoginRequest): Observable<AdminAuthResponse> {
    return this.http.post<AdminAuthResponse>(`${this.baseUrl}/auth/admin/login`, credentials);
  }

  logout(): Observable<any> {
    const token = localStorage.getItem('adminToken');
    
    // Clear local data immediately
    this.clearCurrentAdmin();
    
    if (token) {
      return this.http.post(`${this.baseUrl}/users/admin/logout`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
    }
    return new Observable(observer => {
      observer.next({});
      observer.complete();
    });
  }

  setCurrentAdmin(admin: any): void {
    // Clear any existing data first
    this.clearCurrentAdmin();
    
    localStorage.setItem('currentAdmin', JSON.stringify(admin));
    localStorage.setItem('adminToken', admin.token);
    this.currentAdminSubject.next(admin);
    
    // Fetch full admin details including photo from database
    if (admin.id) {
      this.http.get(`${EnvVariables.userServiceUrl}/users/admin/${admin.id}`).subscribe({
        next: (fullAdmin: any) => {
          const updatedAdmin = { ...admin, ...fullAdmin };
          localStorage.setItem('currentAdmin', JSON.stringify(updatedAdmin));
          this.currentAdminSubject.next(updatedAdmin);
        },
        error: (error) => console.error('Error fetching admin details:', error)
      });
    }
  }

  getAdminDetails(adminId: number): Observable<any> {
    return this.http.get(`${EnvVariables.userServiceUrl}/users/admin/${adminId}`);
  }

  getCurrentAdmin(): any {
    return this.currentAdminSubject.value || JSON.parse(localStorage.getItem('currentAdmin') || 'null');
  }

  clearCurrentAdmin(): void {
    localStorage.removeItem('currentAdmin');
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminId'); // Remove any legacy adminId
    this.currentAdminSubject.next(null);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('adminToken') && !!this.getCurrentAdmin();
  }

  getToken(): string | null {
    return localStorage.getItem('adminToken');
  }

  hasPermission(permission: string): boolean {
    const admin = this.getCurrentAdmin();
    return admin?.permissions?.includes(permission) || false;
  }

  hasRole(role: string): boolean {
    const admin = this.getCurrentAdmin();
    return admin?.role === role;
  }

  isSuperAdmin(): boolean {
    return this.hasRole('super_admin');
  }

  canManageProducts(): boolean {
    const admin = this.getCurrentAdmin();
    if (!admin) return false;
    const email = admin.email || '';
    const result = email.includes('productmanager') || email.includes('superadmin');
    console.log('canManageProducts for', email, ':', result);
    return result;
  }

  canManageOrders(): boolean {
    const admin = this.getCurrentAdmin();
    if (!admin) return false;
    const email = admin.email || '';
    const result = email.includes('ordermanager') || email.includes('superadmin');
    console.log('canManageOrders for', email, ':', result);
    return result;
  }

  canManageCustomers(): boolean {
    const admin = this.getCurrentAdmin();
    if (!admin) return false;
    const email = admin.email || '';
    const result = email.includes('customermanager') || email.includes('superadmin');
    console.log('canManageCustomers for', email, ':', result);
    return result;
  }

  canViewAnalytics(): boolean {
    return this.hasPermission('view_analytics') || this.isSuperAdmin();
  }

  canManageAdmins(): boolean {
    return this.hasPermission('manage_admins') || this.isSuperAdmin();
  }

  refreshCurrentAdmin(): Observable<any> {
    const token = this.getToken();
    if (!token) {
      throw new Error('No token found');
    }
    
    // Decode token to get email and fetch admin by email
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const email = payload.sub || payload.email;
      return this.http.get(`${EnvVariables.userServiceUrl}/users/admin/email/${email}`);
    } catch (error) {
      throw new Error('Invalid token');
    }
  }
}
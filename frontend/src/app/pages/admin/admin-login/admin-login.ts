import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminAuthService, AdminAuthResponse } from '../../../services/admin-auth.service';

interface AdminLoginData {
  email: string;
  password: string;
}

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-login.html',
  styleUrl: './admin-login.css'
})
export class AdminLoginComponent implements OnInit {
  loginData: AdminLoginData = {
    email: '',
    password: ''
  };
  
  isLoading = false;
  errorMessage = '';
  showPassword = false;

  constructor(
    private adminAuth: AdminAuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Redirect if already logged in
    if (this.adminAuth.isLoggedIn()) {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  onSubmit(): void {
    if (!this.loginData.email || !this.loginData.password) {
      this.errorMessage = 'Please enter both email/username and password';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.adminAuth.login(this.loginData).subscribe({
      next: (response: AdminAuthResponse) => {
        console.log('Admin login successful:', response);
        
        // Set the admin data with the new format
        this.adminAuth.setCurrentAdmin({
          id: response.id,
          name: response.name,
          email: response.email,
          role: response.role,
          token: response.token,
          permissions: [] // Will be populated from backend if needed
        });
        
        this.isLoading = false;
        this.router.navigate(['/admin/dashboard']);
      },
      error: (error) => {
        console.error('Admin login failed:', error);
        this.errorMessage = error.error?.message || 'Login failed. Please check your credentials.';
        this.isLoading = false;
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  getDemoCredentials(): void {
    this.loginData = {
      email: 'admin@megamart.com',
      password: 'admin123'
    };
  }
} 
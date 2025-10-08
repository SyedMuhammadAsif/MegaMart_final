import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, AuthResponse } from '../../../services/auth.service';
import { CartService } from '../../../services/cart-service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  loginForm = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d])[A-Za-z\d\S]{8,}$/)]),
  });

  errorMessage: string = '';

  constructor(private router: Router, private auth: AuthService, private cartService: CartService) {}

  onSubmit() {
    if (this.loginForm.valid) {
      const loginData = {
        email: this.loginForm.value.email!,
        password: this.loginForm.value.password!
      };

      console.log('Customer login attempt:', loginData);
      this.auth.login(loginData).subscribe({
        next: (response: AuthResponse) => {
          console.log('Customer login successful:', response);
          this.errorMessage = '';
          
          // Set the user data with the new format
          this.auth.setCurrentUser({
            id: response.id,
            name: response.name,
            email: response.email,
            role: response.role,
            token: response.token
          });
          
          // Explicitly refresh cart after login
          this.cartService.refreshCart();
          
          this.router.navigate(['/home']); 
        },
        error: err => {
          console.error('Customer login failed:', err);
          this.loginForm.markAllAsTouched();
          this.errorMessage = err.error?.message || 'Login failed. Please check your credentials.'; 
        }
      });
    } else {
      console.log('Form invalid:', this.loginForm.errors);
      this.loginForm.markAllAsTouched();
    }
  }

  closeModal() {
    this.router.navigate(['']);
  }
}
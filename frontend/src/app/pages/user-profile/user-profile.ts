import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../services/cart-service';

@Component({
  selector: 'app-user-profile',
  imports: [CommonModule, RouterLink],
  templateUrl: './user-profile.html',
  styleUrl: './user-profile.css'
})
export class UserProfile {
  constructor(
    private auth: AuthService, 
    private router: Router,
    private cartService: CartService
  ) {}

  logout() {
    this.auth.logout().subscribe({
      next: () => {
        this.auth.clearCurrentUser();
        this.cartService.refreshCart();
        this.router.navigate(['']);
      },
      error: () => {
        // Clear local state even if backend call fails
        this.auth.clearCurrentUser();
        this.cartService.refreshCart();
        this.router.navigate(['']);
      }
    });
  }

  isLoggedIn() {
    return this.auth.isLoggedIn();
  }
}

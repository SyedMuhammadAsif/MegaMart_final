import { Component, OnInit } from '@angular/core';
import { AdminAuthService } from '../../services/admin-auth.service';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-nav',
  imports: [CommonModule,RouterLink],
  templateUrl: './admin-nav.html',
  styleUrl: './admin-nav.css'
})
export class AdminNav implements OnInit {
constructor(public adminAuth: AdminAuthService) {}

  ngOnInit(): void {
    // Refresh admin data to ensure correct permissions
    this.adminAuth.refreshCurrentAdmin().subscribe({
      next: (admin) => {
        this.adminAuth.currentAdminSubject.next(admin);
      },
      error: (error) => console.error('Error refreshing admin data:', error)
    });
  }

  onAdminLogout(): void {
    this.adminAuth.logout().subscribe({
      next: () => {
        this.adminAuth.clearCurrentAdmin();
        window.location.href = '/admin/login';
      },
      error: () => {
        // Clear local state even if backend call fails
        this.adminAuth.clearCurrentAdmin();
        window.location.href = '/admin/login';
      }
    });
  }
}

import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AdminAuthService } from '../services/admin-auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(
    private adminAuth: AdminAuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    // Check if admin is logged in
    if (!this.adminAuth.isLoggedIn()) {
              console.log('Role guard: Admin not logged in');
      this.router.navigate(['/admin/login']);
      return false;
    }

    // Get current admin
    const admin = this.adminAuth.getCurrentAdmin();
    
    // Get required permission from route data
    const requiredPermission = route.data['permission'];
    
    if (!requiredPermission) {
              console.log('Role guard: No permission required');
      return true;
    }

    // Check role-based access using email
    const email = admin?.email || '';
    console.log('RoleGuard checking permission:', requiredPermission, 'for email:', email);
    let hasAccess = false;
    
    switch (requiredPermission) {
      case 'manage_products':
        hasAccess = email.includes('productmanager') || email.includes('superadmin');
        break;
      case 'manage_orders':
        hasAccess = email.includes('ordermanager') || email.includes('superadmin');
        break;
      case 'manage_customers':
        hasAccess = email.includes('customermanager') || email.includes('superadmin');
        break;
      default:
        hasAccess = email.includes('superadmin') || admin.role === 'ADMIN';
    }
    
    console.log('Access result:', hasAccess);
    
    if (hasAccess) {
              console.log(`Role guard: Admin '${email}' has access to '${requiredPermission}'`);
      return true;
    } else {
              console.log(`Role guard: Admin '${email}' lacks access to '${requiredPermission}'`);
      // Redirect to dashboard with error message
      this.router.navigate(['/admin/dashboard'], { 
        queryParams: { error: 'insufficient_permissions' } 
      });
      return false;
    }
  }
} 
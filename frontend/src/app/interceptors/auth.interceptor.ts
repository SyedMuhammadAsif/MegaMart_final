import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Get token from localStorage
    const token = localStorage.getItem('token') || localStorage.getItem('adminToken');
    
    console.log('Interceptor - URL:', req.url);
    console.log('Interceptor - Token found:', !!token);
    console.log('Interceptor - Token preview:', token ? token.substring(0, 50) + '...' : 'none');
    
    // If token exists, clone the request and add Authorization header
    if (token) {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      console.log('Interceptor - Added Authorization header');
      return next.handle(authReq);
    }
    
    console.log('Interceptor - No token, proceeding without auth header');
    // If no token, proceed with original request
    return next.handle(req);
  }
}
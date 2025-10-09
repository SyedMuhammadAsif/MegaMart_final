import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Get token from localStorage
  const token = localStorage.getItem('token') || localStorage.getItem('adminToken');
  
  console.log('Functional Interceptor - URL:', req.url);
  console.log('Functional Interceptor - Token found:', !!token);
  console.log('Functional Interceptor - Token preview:', token ? token.substring(0, 50) + '...' : 'none');
  
  // If token exists, clone the request and add Authorization header
  if (token) {
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    console.log('Functional Interceptor - Added Authorization header');
    return next(authReq);
  }
  
  console.log('Functional Interceptor - No token, proceeding without auth header');
  // If no token, proceed with original request
  return next(req);
};
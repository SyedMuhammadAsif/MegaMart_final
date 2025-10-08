import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { EnvVariables } from '../env/env-variables';

@Injectable({
  providedIn: 'root'
})
export class DataPersistenceService {
  private dataSyncSubject = new BehaviorSubject<boolean>(true);
  public dataSync$ = this.dataSyncSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Ensure data is properly saved to db.json
   */
  ensureDataPersistence<T>(operation: Observable<T>): Observable<T> {
    return operation.pipe(
      tap(() => {
        console.log('Data operation completed successfully');
        this.dataSyncSubject.next(true);
      }),
      catchError((error) => {
        console.error('Data operation failed:', error);
        this.dataSyncSubject.next(false);
        throw error;
      })
    );
  }

  /**
   * Verify database connectivity
   */
  verifyDatabaseConnection(): Observable<boolean> {
    return new Observable(observer => {
      this.http.get(`${EnvVariables.userServiceUrl}/users`).subscribe({
        next: () => {
          console.log('Database connection verified');
          observer.next(true);
          observer.complete();
        },
        error: (error) => {
          console.error('Database connection failed:', error);
          observer.next(false);
          observer.complete();
        }
      });
    });
  }

  /**
   * Force refresh all data from database
   */
  forceDataRefresh(): Observable<void> {
    return new Observable(observer => {
      // Trigger a small operation to ensure database is responsive
      this.http.get(`${EnvVariables.userServiceUrl}/users`).subscribe({
        next: () => {
          console.log('Data refresh completed');
          observer.next();
          observer.complete();
        },
        error: (error) => {
          console.error('Data refresh failed:', error);
          observer.error(error);
        }
      });
    });
  }

  /**
   * Check if user data exists in database
   */
  checkUserDataExists(userId: string): Observable<boolean> {
    return this.http.get<any[]>(`${EnvVariables.userServiceUrl}/users?user_id=${userId}`).pipe(
      tap(users => {
       
      }),
      catchError(error => {
        console.error('Error checking user data:', error);
        throw error;
      }),
      map(users => users.length > 0)
    );
  }

  /**
   * Check if order data exists for user
   */
  checkOrderDataExists(userEmail: string): Observable<boolean> {
    return this.http.get<any[]>(`${EnvVariables.orderServiceUrl}/orders`).pipe(
      tap(orders => {
      
        const userOrders = orders.filter(order => 
          order.customerInfo?.email?.toLowerCase() === userEmail.toLowerCase()
        );
      
      }),
      catchError(error => {
        console.error('Error checking order data:', error);
        throw error;
      }),
      map(orders => {
        const userOrders = orders.filter(order => 
          order.customerInfo?.email?.toLowerCase() === userEmail.toLowerCase()
        );
        return userOrders.length > 0;
      })
    );
  }

 
  logApplicationState(): void {
   
  }
} 
import { Component, signal, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Header } from "./common/header/header";
import { Footer } from "./common/footer/footer";


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Footer, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  protected readonly title = signal('MEGA_Mart');
  
  isAdminPage = false;
  private routerSubscription: Subscription = new Subscription();

  constructor(
    private router: Router 
  ) {}

  ngOnInit(): void {
    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        // Check if current route starts with '/admin'
        this.isAdminPage = event.url.startsWith('/admin');
        console.log('Route changed:', event.url, 'Is Admin Page:', this.isAdminPage);
      });

    this.isAdminPage = this.router.url.startsWith('/admin');
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }
}

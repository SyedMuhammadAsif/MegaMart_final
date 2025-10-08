import { Component, ElementRef, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { ProductDetailService } from '../../services/product-detail-service';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Product } from '../../models/product';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { SimilarProducts } from '../similar-products/similar-products';

import { CartService } from '../../services/cart-service';
import { Subscription } from 'rxjs';
declare var bootstrap: any;

@Component({
  selector: 'app-product-detail',
  imports: [CommonModule,RouterModule,SimilarProducts],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css'
})
export class ProductDetail implements OnInit, OnDestroy {
   product: Product | null =null ;
 selectedImage: string | undefined;
 loading = true;
 quantityAdded:number=0;

 private cartSubscription: Subscription | undefined;
   constructor(
    private productService: ProductDetailService, 
    private route: ActivatedRoute, 

    private cartService: CartService
  ) {}


ngOnInit(): void {
  
    this.route.paramMap.subscribe(params => {
      const productId = Number(params.get('id')); // Getting the ID from route
      console.log('Attempting to load product with ID:', productId);
      window.scrollTo({top:0,behavior:'smooth'});
      if (productId) {
        this.loadProduct(productId);
      } else {
        this.loading = false;
        console.error('No product ID provided in route.');
        
      }
    });

    // Subscribe to cart changes to keep component state updated
    this.cartSubscription = this.cartService.cart$.subscribe(cart => {
      if (this.product) {
        const cartItem = cart.items.find(item => item.ProductID === this.product!.id);
        this.quantityAdded = cartItem ? cartItem.Quantity : 0;
        console.log(`Cart updated for product ${this.product.id}, quantity: ${this.quantityAdded}`);
      }
    });
    
  }

  loadProduct(id: number): void {
    this.loading = true; 
    this.productService.getProductById(id).subscribe({
      next: data => {
        this.product = data; 
        this.selectedImage = data.images?.[0]; 
        this.loading = false; 
        console.log('Product loaded:', this.product);
        console.log('Product loaded successfully (hardcoded ID):', this.product);
      
      console.log('Passing category to similar products:', this.product.category);
      console.log('Passing product ID to exclude:', this.product.id);
      

      
      // Check if this product is in the cart
      this.checkCartStatus(this.product.id);
      },
      error: err => {
        console.error('Error loading product:', err);
        this.product = null; 
        this.loading = false; 
        
      }
    });
  }



  // Check if the current product is in the cart and get its quantity
  checkCartStatus(productId: number): void {
    this.cartService.getProductQuantityInCart(productId).subscribe(quantity => {
      this.quantityAdded = quantity;
      console.log(`Product ${productId} quantity in cart: ${this.quantityAdded}`);
    });
  }

  selectImage(img: string): void {
    this.selectedImage = img;
  }

  @ViewChild('toastElem', { static: false }) toastElem!: ElementRef;

  showToast(): void {
    
    if (this.toastElem) {
      const toast = new bootstrap.Toast(this.toastElem.nativeElement);
      toast.show();
    }
  }

    addToCart(): void {
     console.log('addToCart called');
     console.log('Product:', this.product);
     console.log('quantityAdded:', this.quantityAdded);
     console.log('stock:', this.product?.stock);
     
     if (this.product && this.quantityAdded === 0 && this.product.stock > 0) {
       console.log('Calling cartService.addToCart with productId:', this.product.id);
       this.cartService.addToCart(this.product.id, 1).subscribe({
         next: (response) => {
           console.log('Cart service response:', response);
           if (response) {
             this.showToast();
             console.log('Product added to cart successfully');
             this.cartService.refreshCart();
           } else {
             console.log('No response from cart service');
           }
         },
         error: (error) => {
           console.error('Error adding to cart:', error);
           alert('Failed to add to cart. Please try again.');
         }
       });
     } else {
       console.log('Add to cart conditions not met');
     }
   }

    increaseQty(): void {
     if (this.product && this.quantityAdded > 0 && this.quantityAdded < this.product.stock) {
       this.cartService.addToCart(this.product.id, 1).subscribe({
         next: (response) => {
           if (response) {
             console.log('Quantity increased in cart');
             this.cartService.refreshCart();
           }
         },
         error: (error) => {
           console.error('Error updating cart:', error);
         }
       });
     }
   }

   decreaseQty(): void {
     if (this.product) { 
       if (this.quantityAdded > 1) {
         // Find the cart item and update quantity
         const currentCart = this.cartService.currentCart;
         const cartItem = currentCart.items.find(item => item.ProductID === this.product!.id);
         if (cartItem && cartItem.id) {
           this.cartService.updateQuantity(cartItem.id, this.quantityAdded - 1).subscribe({
             next: (response) => {
               if (response) {
                 console.log('Quantity decreased in cart');
                 this.cartService.refreshCart();
               }
             },
             error: (error) => {
               console.error('Error updating cart:', error);
             }
           });
         }
       } else {
         // Remove from cart
         const currentCart = this.cartService.currentCart;
         const cartItem = currentCart.items.find(item => item.ProductID === this.product!.id);
         if (cartItem && cartItem.id) {
           this.cartService.removeFromCart(cartItem.id).subscribe({
             next: (response) => {
               if (response) {
                 console.log('Item removed from cart');
                 this.cartService.refreshCart();
               }
             },
             error: (error) => {
               console.error('Error removing from cart:', error);
             }
           });
         }
       }
     }
   }




  ngOnDestroy(): void {
    this.cartSubscription?.unsubscribe();
  }

}
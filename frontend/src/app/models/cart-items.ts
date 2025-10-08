import { Product } from "./product";

export interface CartItem {
  id?: string; 
  CartItemID: number;
  ProductID: number;
  Quantity: number;
  TotalPrice: number;
  Product?: Product; 
  user_id?: string; 
}

export interface Cart {
  items: CartItem[];
  totalItems: number;
  totalPrice: number;
} 

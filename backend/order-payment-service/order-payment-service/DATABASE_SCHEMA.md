# Database Schema - Order Payment Service

## 🗄️ **Mermaid SQL Diagram**

```mermaid
erDiagram
    ORDERS {
        bigint id PK "AUTO_INCREMENT"
        integer user_id FK "NOT NULL"
        decimal total "DECIMAL(10,2) NOT NULL"
        enum payment_type "CARD, UPI, COD"
        enum order_status "PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED"
        enum payment_status "PENDING, COMPLETED, FAILED, REFUNDED"
        datetime order_date
        bigint shipping_address_id "FK to user service"
        datetime created_at "AUTO"
        datetime updated_at "AUTO"
    }

    ORDER_ITEMS {
        bigint id PK "AUTO_INCREMENT"
        bigint order_id FK "NOT NULL"
        bigint product_id "NOT NULL"
        integer quantity "NOT NULL"
        decimal line_total "DECIMAL(12,2) NOT NULL"
    }

    PAYMENTS {
        bigint id PK "AUTO_INCREMENT"
        integer user_id FK "NOT NULL"
        bigint order_id FK "NOT NULL, UNIQUE"
        bigint payment_method_id "FK to user service"
        decimal amount "DECIMAL(10,2) NOT NULL"
        enum payment_status "PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED"
        datetime payment_date
        varchar transaction_id "UNIQUE"
        datetime created_at "AUTO"
        datetime updated_at "AUTO"
    }

    %% External tables (in user service)
    USERS {
        integer id PK "AUTO_INCREMENT"
        varchar name
        varchar email
        enum role "USER, ADMIN"
    }

    ADDRESSES {
        bigint id PK "AUTO_INCREMENT"
        integer user_id FK
        varchar full_name
        varchar address_line1
        varchar address_line2
        varchar city
        varchar state
        varchar postal_code
        varchar country
        varchar phone "10 digits"
        boolean is_default
    }

    PAYMENT_METHODS {
        bigint id PK "AUTO_INCREMENT"
        integer user_id FK
        enum type "CARD, UPI, COD"
        varchar card_number "16 digits, masked"
        varchar cardholder_name
        varchar expiry_month "01-12"
        varchar expiry_year "2020-2099"
        varchar cvv "3 digits"
        varchar upi_id
        boolean is_default
    }

    PRODUCTS {
        bigint id PK "AUTO_INCREMENT"
        varchar title
        text description
        varchar category
        decimal price "DECIMAL(10,2)"
        integer stock
        varchar brand
        enum availability_status "IN_STOCK, OUT_OF_STOCK"
    }

    %% Relationships
    USERS ||--o{ ORDERS : "places"
    ORDERS ||--o{ ORDER_ITEMS : "contains"
    ORDERS ||--|| PAYMENTS : "has"
    ORDERS }o--|| ADDRESSES : "ships_to"
    PAYMENTS }o--|| PAYMENT_METHODS : "uses"
    ORDER_ITEMS }o--|| PRODUCTS : "references"
    USERS ||--o{ ADDRESSES : "has"
    USERS ||--o{ PAYMENT_METHODS : "owns"
```

## 📊 **Table Details**

### **ORDERS Table**
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    payment_type ENUM('CARD', 'UPI', 'COD'),
    order_status ENUM('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
    payment_status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') DEFAULT 'PENDING',
    order_date DATETIME,
    shipping_address_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (shipping_address_id) REFERENCES addresses(id)
);
```

### **ORDER_ITEMS Table**
```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

### **PAYMENTS Table**
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER NOT NULL,
    order_id BIGINT NOT NULL UNIQUE,
    payment_method_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    payment_status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED') DEFAULT 'PENDING',
    payment_date DATETIME,
    transaction_id VARCHAR(255) UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id)
);
```

## 🎯 **Key Relationships**

1. **Order ↔ OrderItems**: One-to-Many (One order has many items)
2. **Order ↔ Payment**: One-to-One (One order has one payment)
3. **Order → User**: Many-to-One (Many orders belong to one user)
4. **Order → Address**: Many-to-One (Many orders can ship to one address)
5. **Payment → PaymentMethod**: Many-to-One (Many payments can use one payment method)

## ✅ **Data Consistency**

### **Foreign Keys:**
- ✅ `orders.user_id` → `users.id` (Integer)
- ✅ `orders.shipping_address_id` → `addresses.id` (Long)
- ✅ `payments.user_id` → `users.id` (Integer)
- ✅ `payments.order_id` → `orders.id` (Long, UNIQUE)
- ✅ `payments.payment_method_id` → `payment_methods.id` (Long)
- ✅ `order_items.order_id` → `orders.id` (Long)
- ✅ `order_items.product_id` → `products.id` (Long)

### **Validation Rules:**
- ✅ **Phone**: Exactly 10 digits
- ✅ **Card Number**: Exactly 16 digits
- ✅ **CVV**: Exactly 3 digits
- ✅ **UPI ID**: Email format
- ✅ **User ID**: Integer (FK)

## 🧹 **No Redundancy Found**

Your code is **clean and well-organized**:
- ✅ **No duplicate DTOs**
- ✅ **No unused services**
- ✅ **No redundant interfaces**
- ✅ **Proper separation of concerns**
- ✅ **Consistent naming conventions**
- ✅ **All relationships properly mapped**

## 💰 **Refund Status Fix Applied**

The refund bug is now fixed. After cancellation, you'll see:
```json
{
  "orderStatus": "CANCELLED",
  "paymentStatus": "REFUNDED",  // ✅ Now consistent
  "payment": {
    "paymentStatus": "REFUNDED"  // ✅ Also consistent
  }
}
```

**Your database design is solid and your code is clean with no redundancy!** 🎉 
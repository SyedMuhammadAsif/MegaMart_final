# Order-Payment Service - Clean & Simple

## 📁 **Clean DTO Structure (Only 3 DTOs)**

```
📁 dto/
├── OrderRequest.java    ← For creating orders
├── OrderResponse.java   ← For order details  
└── PaymentRequest.java  ← For processing payments
```

## 🎯 **API Usage Examples**

### **1. Create Order**
**POST** `/api/orders`

```json
{
  "userId": 1001,
  "total": 999.99,
  "paymentType": "CARD",
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "lineTotal": 999.99
    }
  ],
  "addressId": 1,
  "paymentMethodId": 1
}
```

### **2. Create Order with New Address & Payment Method**
```json
{
  "userId": 1001,
  "total": 999.99,
  "paymentType": "CARD",
  "items": [
    {
      "productId": 1,
      "quantity": 1,
      "lineTotal": 999.99
    }
  ],
  "newAddress": {
    "fullName": "John Doe",
    "addressLine1": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA",
    "phone": "1234567890"
  },
  "newPaymentMethod": {
    "type": "CARD",
    "cardNumber": "1234567890123456",
    "cardholderName": "John Doe",
    "expiryMonth": "12",
    "expiryYear": "2025",
    "cvv": "123"
  }
}
```

### **3. Process Payment**
**POST** `/api/payments/process`

```json
{
  "orderId": 1,
  "paymentMethodId": 1
}
```

### **4. Process Payment with New Payment Method**
```json
{
  "orderId": 1,
  "newPaymentMethod": {
    "type": "UPI",
    "upiId": "user@paytm"
  }
}
```

## ✅ **Validation Rules**

- **Phone**: Exactly 10 digits (`1234567890`)
- **Card Number**: Exactly 16 digits (`1234567890123456`)
- **CVV**: Exactly 3 digits (`123`)
- **UPI ID**: Email format (`user@paytm`)

## 🚀 **Key Endpoints**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create order |
| GET | `/api/orders/{id}` | Get order details |
| GET | `/api/orders/user/{userId}` | Get user orders |
| PUT | `/api/orders/{id}/status?status=SHIPPED` | Update status |
| PUT | `/api/orders/{id}/cancel` | Cancel order |
| POST | `/api/payments/process` | Process payment |
| GET | `/api/payments/order/{orderId}` | Get payment details |

## 🔧 **Running the Service**

1. **Start**: `mvn spring-boot:run`
2. **Swagger**: `http://localhost:9096/swagger-ui.html`
3. **Health**: `http://localhost:9096/api/orders/health`

## 📱 **Frontend Integration**

### **Three Payment Buttons:**
```javascript
// Card Payment
{
  "orderId": 123,
  "newPaymentMethod": {
    "type": "CARD",
    "cardNumber": "1234567890123456",
    "cardholderName": "John Doe",
    "expiryMonth": "12",
    "expiryYear": "2025",
    "cvv": "123"
  }
}

// UPI Payment  
{
  "orderId": 123,
  "newPaymentMethod": {
    "type": "UPI",
    "upiId": "user@paytm"
  }
}

// COD Payment
{
  "orderId": 123,
  "newPaymentMethod": {
    "type": "COD"
  }
}
```

## 🎯 **Simple & Clean**

- ✅ **Only 3 DTOs** (not 6)
- ✅ **Builder pattern** for clean code
- ✅ **No "simplified" naming** 
- ✅ **All validation included**
- ✅ **Professional structure**
- ✅ **Easy to understand** 
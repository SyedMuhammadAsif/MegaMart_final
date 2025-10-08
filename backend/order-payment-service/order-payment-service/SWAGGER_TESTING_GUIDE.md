# Swagger Testing Guide - Correct Order

## 🚀 **Step-by-Step Testing in Swagger UI**

### **Prerequisites:**
1. Start the service: `mvn spring-boot:run`
2. Open Swagger UI: `http://localhost:9096/swagger-ui.html`
3. Wait for service to fully start (check logs)

---

## 📋 **Testing Order (Follow this sequence)**

### **Step 1: Health Check** ✅
**Test**: `GET /api/orders/health`
- **Purpose**: Verify service is running
- **Expected**: `{"status": "UP", "service": "Order-Payment Service"}`

---

### **Step 2: Create Order with Existing Data** 📦
**Test**: `POST /api/orders`

**Request Body:**
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

**Expected Result:**
- ✅ Status: `201 Created`
- ✅ Response contains: `id`, `orderStatus: "PENDING"`, `paymentStatus: "PENDING"`
- ✅ **Note the Order ID** (you'll need it for next steps)

---

### **Step 3: Get Order Details** 📋
**Test**: `GET /api/orders/{id}`
- **Use the Order ID from Step 2**
- **Expected**: Full order details with address and payment info

---

### **Step 4: Process Payment** 💳
**Test**: `POST /api/payments/process`

**Request Body:**
```json
{
  "orderId": 1,
  "paymentMethodId": 1
}
```
*Replace `1` with the actual Order ID from Step 2*

**Expected Result:**
- ✅ Status: `200 OK`
- ✅ Payment status: `"COMPLETED"` or `"FAILED"`
- ✅ Transaction ID generated
- ✅ **Note the Transaction ID**

---

### **Step 5: Verify Order Status Changed** 🔄
**Test**: `GET /api/orders/{id}` (same as Step 3)
- **Expected**: `orderStatus: "CONFIRMED"`, `paymentStatus: "COMPLETED"`

---

### **Step 6: Get Payment Details** 💰
**Test**: `GET /api/payments/order/{orderId}`
- **Use Order ID from Step 2**
- **Expected**: Payment details with transaction ID

---

### **Step 7: Get User's Orders** 👤
**Test**: `GET /api/orders/user/{userId}`
- **Use**: `userId: 1001`
- **Parameters**: `page: 0`, `size: 10`
- **Expected**: Paginated list with your created order

---

### **Step 8: Create Order with New Address & Payment Method** 🆕
**Test**: `POST /api/orders`

**Request Body:**
```json
{
  "userId": 1003,
  "total": 1499.99,
  "paymentType": "UPI",
  "items": [
    {
      "productId": 2,
      "quantity": 1,
      "lineTotal": 1499.99
    }
  ],
  "newAddress": {
    "fullName": "Alice Johnson",
    "addressLine1": "789 Pine Street",
    "city": "Chicago",
    "state": "IL",
    "postalCode": "60601",
    "country": "USA",
    "phone": "5551234567"
  },
  "newPaymentMethod": {
    "type": "UPI",
    "upiId": "alice.johnson@gpay"
  }
}
```

**Expected Result:**
- ✅ New address and payment method created
- ✅ Order created with new data
- ✅ **Note this Order ID for next tests**

---

### **Step 9: Process Payment with New Payment Method** 🆕💳
**Test**: `POST /api/payments/process`

**Request Body:**
```json
{
  "orderId": 2,
  "newPaymentMethod": {
    "type": "CARD",
    "cardNumber": "9876543210123456",
    "cardholderName": "Alice Johnson",
    "expiryMonth": "06",
    "expiryYear": "2027",
    "cvv": "456"
  }
}
```
*Replace `2` with actual Order ID from Step 8*

---

### **Step 10: Test Order Status Updates** 📈
**Test**: `PUT /api/orders/{id}/status`
- **Use Order ID from Step 2**
- **Parameter**: `status: PROCESSING`
- **Expected**: Order status updated

**Test**: `PUT /api/orders/{id}/status`
- **Parameter**: `status: SHIPPED`
- **Expected**: Order status updated to SHIPPED

---

### **Step 11: Test Order Tracking** 📍
**Test**: `GET /api/orders/{id}/tracking`
- **Use Order ID from Step 2**
- **Expected**: Tracking info with estimated delivery

---

### **Step 12: Test Order Cancellation** ❌
**Test**: `PUT /api/orders/{id}/cancel`
- **Use Order ID from Step 8**
- **Expected**: Order status changed to CANCELLED

---

### **Step 13: Test Admin Functions** 👨‍💼
**Test**: `GET /api/orders`
- **Parameters**: `page: 0`, `size: 10`
- **Expected**: All orders (admin view)

---

### **Step 14: Test Payment Lookup** 🔍
**Test**: `GET /api/payments/transaction/{transactionId}`
- **Use Transaction ID from Step 4**
- **Expected**: Payment details by transaction ID

---

## ⚠️ **Common Testing Mistakes to Avoid**

### **1. Wrong Order:**
```
❌ DON'T: Process payment before creating order
✅ DO: Create order first, then process payment
```

### **2. Using Non-existent IDs:**
```
❌ DON'T: Use orderId: 999 (doesn't exist)
✅ DO: Use the actual Order ID from create order response
```

### **3. Invalid Validation:**
```
❌ DON'T: Use phone: "123" (too short)
✅ DO: Use phone: "1234567890" (exactly 10 digits)
```

### **4. Wrong Payment Types:**
```
❌ DON'T: paymentType: "CREDIT_CARD"
✅ DO: paymentType: "CARD"
```

## 🎯 **Validation Test Cases**

### **Test Invalid Phone Number:**
```json
{
  "userId": 1001,
  "total": 999.99,
  "paymentType": "CARD",
  "items": [...],
  "newAddress": {
    "fullName": "Test User",
    "addressLine1": "123 Test St",
    "city": "Test City",
    "state": "TS",
    "postalCode": "12345",
    "country": "USA",
    "phone": "123"  // ❌ Too short - should fail
  }
}
```
**Expected**: `400 Bad Request` with validation error

### **Test Invalid Card Number:**
```json
{
  "orderId": 1,
  "newPaymentMethod": {
    "type": "CARD",
    "cardNumber": "1234",  // ❌ Too short - should fail
    "cardholderName": "Test User",
    "expiryMonth": "12",
    "expiryYear": "2025",
    "cvv": "123"
  }
}
```
**Expected**: `400 Bad Request` with validation error

### **Test Invalid CVV:**
```json
{
  "orderId": 1,
  "newPaymentMethod": {
    "type": "CARD",
    "cardNumber": "1234567890123456",
    "cardholderName": "Test User",
    "expiryMonth": "12",
    "expiryYear": "2025",
    "cvv": "12"  // ❌ Too short - should fail
  }
}
```
**Expected**: `400 Bad Request` with validation error

## 🎯 **Quick Test Sequence (5 minutes)**

1. **Health Check** → Should return UP
2. **Create Order** (existing data) → Note Order ID
3. **Process Payment** → Should succeed
4. **Get Order Details** → Should show CONFIRMED status
5. **Create Order** (new data) → Test new address/payment creation

## 📱 **Three Payment Button Tests**

### **Card Payment:**
```json
{
  "orderId": 1,
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

### **UPI Payment:**
```json
{
  "orderId": 1,
  "newPaymentMethod": {
    "type": "UPI",
    "upiId": "test@paytm"
  }
}
```

### **COD Payment:**
```json
{
  "orderId": 1,
  "newPaymentMethod": {
    "type": "COD"
  }
}
```

## 🎉 **Success Indicators**

- ✅ **Orders created** with status PENDING
- ✅ **Payments processed** with transaction IDs
- ✅ **Order status changes** to CONFIRMED after payment
- ✅ **Validation errors** for invalid data
- ✅ **Address/Payment methods** created automatically
- ✅ **Phone numbers** exactly 10 digits
- ✅ **Card numbers** exactly 16 digits  
- ✅ **CVV numbers** exactly 3 digits

Follow this order and your APIs will work perfectly! 🚀 
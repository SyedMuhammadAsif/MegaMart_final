# Quick Testing Guide (Without Swagger)

## 🚀 **Test Your APIs with curl/Postman**

Since Swagger is having issues, let's test directly with curl commands:

### **1. Health Check** ✅
```bash
curl -X GET http://localhost:9096/api/orders/health
```
**Expected**: `{"service": "Order-Payment Service", "status": "UP"}`

### **2. Create Order** 📦
```bash
curl -X POST http://localhost:9096/api/orders \
  -H "Content-Type: application/json" \
  -d '{
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
  }'
```

### **3. Get Order Details** 📋
```bash
curl -X GET http://localhost:9096/api/orders/1
```

### **4. Process Payment** 💳
```bash
curl -X POST http://localhost:9096/api/payments/process \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "paymentMethodId": 1
  }'
```

### **5. Get User Orders** 👤
```bash
curl -X GET "http://localhost:9096/api/orders/user/1001?page=0&size=10"
```

## 🔧 **Fix Swagger Later**

Once the basic APIs work, we can add Swagger back. For now, focus on:

1. ✅ **Health check works**
2. ✅ **Create order works**
3. ✅ **Process payment works**
4. ✅ **Get order details works**

## 📱 **Test in Postman**

Import these requests into Postman for easier testing:

**Base URL**: `http://localhost:9096`

| Method | Endpoint | Body |
|--------|----------|------|
| GET | `/api/orders/health` | None |
| POST | `/api/orders` | Order JSON |
| GET | `/api/orders/1` | None |
| POST | `/api/payments/process` | Payment JSON |

**This will verify your core functionality works!** 🚀 
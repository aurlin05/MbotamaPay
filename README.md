# MbotamaPay Backend

A complete backend for MbotamaPay, a P2P payment platform built with Spring Boot 3 and Java 17.

## Features

- **User Management**: Registration, login with JWT authentication, user profiles, KYC levels
- **Wallet System**: Virtual wallets with automatic creation, balance management, transaction history
- **P2P Transactions**: Send and receive money between users with atomic operations
- **CinetPay Integration**: Top-up wallets via CinetPay payment gateway
- **QR Code Payments**: Generate and scan QR codes for payments
- **Notifications**: Event-based notification system
- **Admin Dashboard**: User management, transaction monitoring, platform statistics

## Tech Stack

- Java 17
- Spring Boot 3.2.0
- Spring Security + JWT
- PostgreSQL
- Redis (Caching)
- JPA/Hibernate
- MapStruct (DTO Mapping)
- ZXing (QR Code)
- Bucket4j (Rate Limiting)
- WebSocket (STOMP)
- Spring Mail (OTP Email)
- Spring Actuator + Prometheus
- Swagger/OpenAPI
- Docker

## Project Structure

```
src/main/java/com/mbotamapay/backend/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/             # Data Transfer Objects
├── entity/          # JPA entities
├── exception/       # Exception handling
├── repository/      # JPA repositories
├── security/        # Security configuration
├── service/         # Business logic interfaces
└── service/impl/    # Business logic implementations
```

## Getting Started

### Prerequisites

- Java 17
- Maven
- Docker & Docker Compose

### Running the Application

1. **Start PostgreSQL & Redis**:
   ```bash
   docker-compose up -d
   ```

2. **Configure Environment Variables**: 
   Copy `.env.example` to `.env` and set your values, or configure directly in `application.yml`:
   ```yaml
   app:
     cinetpay:
       api-key: YOUR_API_KEY
       site-id: YOUR_SITE_ID
     mail:
       username: your_email@gmail.com
       password: your_app_password
   ```

3. **Build the Project** (downloads all dependencies including Bucket4j):
   ```bash
   mvn clean install
   ```

4. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

### API Documentation

Once the application is running, access Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login and get JWT token

### User Profile
- `GET /api/users/profile` - Get current user profile

### Wallet
- `GET /api/wallet/balance` - Get wallet balance

### Transactions
- `POST /api/transactions/send` - Send money to another user
- `GET /api/transactions/history` - Get transaction history

### Payments (CinetPay)
- `POST /api/payment/init` - Initialize a top-up payment
- `POST /api/payment/callback` - CinetPay callback endpoint

### QR Code
- `POST /api/qr/generate` - Generate QR code for payment
- `POST /api/qr/scan` - Decode QR code data
- `POST /api/qr/pay` - Pay using scanned QR code

### Admin (Requires ADMIN role)
- `GET /api/admin/users` - Get all users
- `GET /api/admin/transactions` - Get all transactions
- `GET /api/admin/stats` - Get platform statistics

## Security

- Passwords are hashed using BCrypt
- JWT tokens for authentication (24-hour expiration)
- Role-based access control (USER, ADMIN)
- Input validation on all endpoints

## Database Schema

### Users
- User information, credentials, KYC level

### Wallets
- One wallet per user
- Balance tracking in XAF (CFA Franc)

### Transactions
- Transaction history with status tracking
- Support for P2P transfers and top-ups

### Notifications
- Event-based notifications for users

## Docker Deployment

Build and run with Docker:

```bash
# Build the JAR
mvn clean package

# Build Docker image
docker build -t mbotamapay-backend .

# Run with docker-compose
docker-compose up
```

## Testing

Run tests with:
```bash
mvn test
```

## License

Copyright © 2024 MbotamaPay

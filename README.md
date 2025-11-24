# MbotamaPay Backend

A complete P2P payment platform backend built with Spring Boot 3 and Java 17, featuring multi-provider payment integrations, double-entry accounting, and liquidity management.

## ✨ Key Features

### Core Functionality
- **User Management**: Registration, JWT authentication with refresh tokens, KYC levels
- **Digital Wallets**: Multi-currency support (XAF, XOF, CDF, EUR, USD) with reserved balances
- **P2P Transactions**: Instant peer-to-peer transfers with atomic operations
- **Double-Entry Ledger**: Full accounting system with audit trail
- **QR Code Payments**: Generate and scan QR codes for payments

### Payment Integrations
- **FeexPay Integration**: Deposits and withdrawals for Benin, Togo, Congo
- **CinetPay Integration**: Deposits and withdrawals for Ivory Coast, Senegal, Mali
- **Bridge Transfers**: Cross-provider transfers with automatic routing
- **Webhook Support**: Real-time payment confirmations from providers

### Financial Systems
- **Liquidity Management**: Operator account balancing and rebalancing suggestions
- **Fee System**: Configurable transaction fees (percentage + fixed)
- **Exchange Rates**: Multi-currency conversion support
- **Reserved Balances**: Prevent overselling with pending transaction locks

### Administration
- **Admin Dashboard**: User management, transaction monitoring, platform statistics
- **Operator Monitoring**: Real-time liquidity tracking and topup capabilities
- **Audit Logging**: Complete security and admin action audit trail
- **Rebalancing Tools**: Automated liquidity rebalancing suggestions

## 🛠 Tech Stack

- **Java 17** + **Spring Boot 3.2.0**
- **Spring Security** + JWT (Access + Refresh Tokens)
- **PostgreSQL** (Flyway Migrations)
- **Redis** (Caching + Idempotency)
- **Spring Cloud OpenFeign** (Payment Provider APIs)
- **JPA/Hibernate** + **MapStruct**
- **ZXing** (QR Code Generation)
- **Bucket4j** (Rate Limiting)
- **WebSocket** (Real-time Notifications)
- **Actuator + Prometheus** (Monitoring)
- **Swagger/OpenAPI** (API Documentation)
- **Docker** + **Adminer**

## 📁 Project Structure

```
src/main/java/com/mbotamapay/backend/
├── config/              # Configuration (Security, Redis, WebSocket, etc.)
├── controller/          # REST controllers (Auth, Wallet, Transaction, Admin, Webhooks)
├── dto/                 # Data Transfer Objects
│   ├── auth/
│   ├── bridge/
│   ├── integrations/
│   ├── liquidity/
│   └── ...
├── entity/              # JPA entities (User, Wallet, Transaction, LedgerEntry, etc.)
├── exception/           # Custom exceptions and global handler
├── integrations/        # Payment provider integrations
│   ├── feexpay/         # FeexPay client, provider, DTOs
│   └── cinetpay/        # CinetPay client, provider, DTOs
├── repository/          # JPA repositories
├── security/            # JWT filters, UserDetails
├── service/             # Business logic interfaces
└── service/impl/        # Service implementations
```

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Gradle** (wrapper included)
- **Docker & Docker Compose**
- **PostgreSQL** (running locally or via Docker)

### Quick Start

1. **Start Services** (Redis + Adminer):
   ```bash
   docker-compose up -d
   ```
   - Redis: `localhost:6379`
   - Adminer: `http://localhost:8081` (PostgreSQL GUI)

2. **Configure Environment**:
   Copy `.env.example` to `.env` and set your values:
   ```bash
   cp .env.example .env
   ```
   
   Key variables:
   ```env
   # Database
   DB_URL=jdbc:postgresql://localhost:5432/mbotamapay_db
   DB_USERNAME=postgres
   DB_PASSWORD=your_password
   
   # JWT
   JWT_SECRET=your_secret_key_here
   JWT_ACCESS_TOKEN_EXPIRATION=900000     # 15 minutes
   JWT_REFRESH_TOKEN_EXPIRATION=2592000000 # 30 days
   
   # FeexPay
   FEEXPAY_API_KEY=your_feexpay_api_key
   FEEXPAY_WEBHOOK_SECRET=your_webhook_secret
   
   # CinetPay
   CINETPAY_API_KEY=your_cinetpay_api_key
   CINETPAY_SITE_ID=your_site_id
   CINETPAY_USERNAME=your_username
   CINETPAY_PASSWORD=your_password
   ```

3. **Build the Project**:
   ```bash
   ./gradlew build
   ```

4. **Run Database Migrations** (automatic with Flyway):
   ```bash
   ./gradlew bootRun
   ```

5. **Seed Operator Accounts** (required for bridge transfers):
   ```bash
   psql -h localhost -U postgres -d mbotamapay_db -f docs/seed_operator_accounts.sql
   ```

### Access Points

- **API**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Adminer**: `http://localhost:8081`
- **Actuator**: `http://localhost:8080/actuator`

## 📚 API Documentation

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login (returns access + refresh token)
- `POST /api/auth/refresh-token` - Get new access token
- `POST /api/auth/logout` - Revoke refresh token

### Wallet Operations
- `GET /api/wallet/balance` - Get current balance
- `GET /api/wallet/transactions` - Transaction history
- `POST /api/wallet/send` - P2P transfer
- `POST /api/wallet/qr-code` - Generate payment QR code

### Payment Providers
- `POST /api/payment/callback` - Generic callback endpoint
- `POST /api/payment/webhook/feexpay` - FeexPay webhooks
- `POST /api/payment/webhook/cinetpay` - CinetPay webhooks

### Admin Endpoints
- `GET /api/admin/stats` - Platform statistics
- `GET /api/admin/operator/balances` - Operator account balances
- `GET /api/admin/operator/rebalance-suggestions` - Liquidity rebalancing
- `POST /api/admin/operator/topup` - Topup operator account

## 🏗 Architecture Highlights

### Double-Entry Ledger
Every transaction creates paired DEBIT and CREDIT entries for full audit trail:
```java
// Example: P2P Transfer
DEBIT: User A wallet (-1000 XAF)
CREDIT: User B wallet (+1000 XAF)
```

### Bridge Transfers
Cross-provider transfers with liquidity reservation:
```
1. User deposits via FeexPay → User wallet credited
2. User initiates withdraw via CinetPay
3. Liquidity reserved on CinetPay operator account
4. Payout initiated → Reservation confirmed/released
```

### Payment Provider Abstraction
Common `PaymentProvider` interface with country-based routing:
```java
PaymentProvider provider = providerFactory.getProviderForCountry("BJ"); // Returns FeexPay
provider.initiatePayment(request);
```

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

## 📊 Database Schema

Key tables:
- `users` - User accounts with KYC levels
- `wallets` - Digital wallets with balance + reserved_balance
- `transactions` - User-facing transaction history
- `ledger_entries` - Double-entry accounting records
- `operator_accounts` - Provider liquidity tracking
- `refresh_tokens` - JWT refresh token management
- `fee_rules` - Configurable transaction fees
- `exchange_rates` - Currency conversion rates
- `webhook_logs` - Provider webhook audit trail

## 🔒 Security Features

- JWT with refresh token rotation
- HMAC-SHA256 webhook signature verification
- Rate limiting (Bucket4j)
- Pessimistic locking for balance updates
- Idempotency keys (Redis)
- Audit logging for admin actions
- Account lockout after failed login attempts

## 🌍 Deployment

### Docker Build
```bash
docker build -t mbotamapay-backend .
docker run -p 8080:8080 --env-file .env mbotamapay-backend
```

### Environment-Specific Configs
- `application.yml` - Base configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production settings

## 📖 Additional Documentation

- [Implementation Plan](file:///c:/Users/Kahpoo/.gemini/antigravity/brain/947b21bd-23e0-4a32-8840-58a92a4e2c60/implementation_plan.md)
- [Gap Analysis](file:///c:/Users/Kahpoo/.gemini/antigravity/brain/947b21bd-23e0-4a32-8840-58a92a4e2c60/gap_analysis.md)
- [Phase 1 Progress](file:///c:/Users/Kahpoo/.gemini/antigravity/brain/947b21bd-23e0-4a32-8840-58a92a4e2c60/phase1_progress.md)
- [Phase 2 Progress](file:///c:/Users/Kahpoo/.gemini/antigravity/brain/947b21bd-23e0-4a32-8840-58a92a4e2c60/phase2_progress.md)

## 👥 Contributing

This is a private project. For questions or contributions, please contact the development team.

## 📄 License

Proprietary - All rights reserved.

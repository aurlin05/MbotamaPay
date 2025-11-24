# Design Document - MbotamaPay Improvements

## Overview

Ce document décrit l'architecture et le design des améliorations à apporter à la plateforme MbotamaPay. Les améliorations visent à renforcer la sécurité, la fiabilité, les performances et la maintenabilité du système tout en maintenant la compatibilité avec l'architecture existante.

L'approche adoptée est incrémentale : chaque amélioration peut être implémentée indépendamment tout en contribuant à l'objectif global d'un système plus robuste et professionnel.

## Architecture

### Architecture Actuelle

MbotamaPay suit une architecture en couches classique Spring Boot :

```
┌─────────────────────────────────────┐
│         Controllers (REST)          │
├─────────────────────────────────────┤
│         Services (Business)         │
├─────────────────────────────────────┤
│      Repositories (Data Access)     │
├─────────────────────────────────────┤
│         Entities (Domain)           │
└─────────────────────────────────────┘
```

### Améliorations Architecturales

Les améliorations proposées ajoutent des composants transversaux :

```
┌──────────────────────────────────────────────────────────┐
│                    API Gateway Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Rate Limiter │  │ Correlation  │  │   Security   │  │
│  │    Filter    │  │  ID Filter   │  │    Headers   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│                    Controllers Layer                      │
│              (Validation & Error Handling)                │
└──────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│                    Services Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Idempotency  │  │    Audit     │  │    Cache     │  │
│  │   Manager    │  │   Service    │  │   Manager    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│                  Repositories Layer                       │
│         (Optimistic Locking & Transactions)               │
└──────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│                    Database Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  PostgreSQL  │  │    Redis     │  │   Flyway     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Optimistic Locking Components

#### Enhanced Entities

Les entités `Wallet` et `Transaction` seront enrichies avec le support du verrouillage optimiste :

```java
@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Long version;  // NEW: Optimistic locking
    
    @Column(nullable = false, precision = 19, scale = 2)
    @Positive
    private BigDecimal balance;
    
    // ... autres champs
}
```

#### ConcurrentModificationHandler

Nouveau composant pour gérer les conflits de version :

```java
@Component
public class ConcurrentModificationHandler {
    public <T> T retryOnOptimisticLock(Supplier<T> operation, int maxRetries);
    public void handleOptimisticLockException(OptimisticLockException ex);
}
```

### 2. Configuration Management

#### Profile-Based Configuration

Structure des fichiers de configuration :

```
src/main/resources/
├── application.yml              # Configuration commune
├── application-dev.yml          # Développement
├── application-test.yml         # Tests
├── application-staging.yml      # Staging
└── application-prod.yml         # Production
```

#### Environment Variables Manager

```java
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private JwtProperties jwt;
    private CinetPayProperties cinetpay;
    private MailProperties mail;
    
    @PostConstruct
    public void validate() {
        // Validation des propriétés requises
    }
}
```

### 3. Idempotency Management

#### IdempotencyService

```java
public interface IdempotencyService {
    Optional<TransactionResponse> checkIdempotency(String idempotencyKey);
    void storeIdempotencyResult(String idempotencyKey, TransactionResponse result);
    boolean isProcessing(String idempotencyKey);
    void markAsProcessing(String idempotencyKey);
}
```

#### Implementation avec Redis

```java
@Service
public class RedisIdempotencyService implements IdempotencyService {
    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Implémentation des méthodes
}
```

### 4. Rate Limiting

#### Enhanced RateLimitFilter

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        String key = getRateLimitKey(request);
        Bucket bucket = resolveBucket(key, request);
        
        if (bucket.tryConsume(1)) {
            addRateLimitHeaders(response, bucket);
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", 
                             String.valueOf(getRetryAfterSeconds(bucket)));
        }
    }
}
```

#### Rate Limit Configuration

```java
@Configuration
public class RateLimitConfig {
    @Bean
    public Map<String, BandwidthConfig> rateLimitConfigs() {
        return Map.of(
            "/api/auth/**", BandwidthConfig.of(5, Duration.ofMinutes(1)),
            "/api/transactions/**", BandwidthConfig.of(10, Duration.ofMinutes(1)),
            "/api/admin/**", BandwidthConfig.of(20, Duration.ofMinutes(1))
        );
    }
}
```

### 5. Cache Management

#### CacheService

```java
public interface CacheService {
    void evictWalletCache(Long walletId);
    void evictUserCache(Long userId);
    void evictTransactionHistoryCache(Long userId);
}
```

#### Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheConfiguration userCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues();
    }
    
    @Bean
    public RedisCacheConfiguration walletCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .disableCachingNullValues();
    }
}
```

### 6. Audit Service

#### AuditService Interface

```java
public interface AuditService {
    void logTransaction(Transaction transaction, User initiator);
    void logWalletModification(Wallet wallet, BigDecimal oldBalance, BigDecimal newBalance, User initiator);
    void logAdminAction(String action, User admin, Map<String, Object> details);
    void logSecurityEvent(String eventType, String severity, Map<String, Object> details);
    Page<AuditLog> queryAuditLogs(AuditLogQuery query, Pageable pageable);
}
```

#### Enhanced AuditLog Entity

```java
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_action_type", columnList = "action_type")
})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private String actionType;
    
    @Column(nullable = false)
    private String severity;
    
    @Column(columnDefinition = "jsonb")
    private String details;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### 7. Validation Components

#### Custom Validators

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TransactionAmountValidator.class)
public @interface ValidTransactionAmount {
    String message() default "Invalid transaction amount";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class TransactionAmountValidator implements ConstraintValidator<ValidTransactionAmount, BigDecimal> {
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return value.compareTo(BigDecimal.ZERO) > 0 && 
               value.compareTo(new BigDecimal("10000000")) <= 0;
    }
}
```

### 8. Health Check Components

#### Enhanced Health Indicators

```java
@Component
public class EmailHealthIndicator implements HealthIndicator {
    private final JavaMailSender mailSender;
    
    @Override
    public Health health() {
        try {
            mailSender.testConnection();
            return Health.up()
                .withDetail("provider", "Gmail SMTP")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## Data Models

### Enhanced Wallet Entity

```java
@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Long version;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(nullable = false, precision = 19, scale = 2)
    @Positive(message = "Balance must be positive")
    private BigDecimal balance;
    
    @Column(nullable = false, length = 3)
    @Pattern(regexp = "XAF|EUR|USD", message = "Unsupported currency")
    private String currency;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### Enhanced Transaction Entity

```java
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_reference", columnList = "reference", unique = true),
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Long version;
    
    @Column(unique = true, nullable = false)
    private String reference;
    
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    @ManyToOne
    @JoinColumn(name = "sender_wallet_id")
    private Wallet senderWallet;
    
    @ManyToOne
    @JoinColumn(name = "receiver_wallet_id")
    private Wallet receiverWallet;
    
    @Column(nullable = false, precision = 19, scale = 2)
    @Positive
    private BigDecimal amount;
    
    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal fee;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;
    
    private String description;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### Idempotency Record

```java
@Data
@Builder
public class IdempotencyRecord {
    private String key;
    private TransactionResponse result;
    private IdempotencyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}

public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
```

## C
orrectness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, several redundancies were identified:
- Property 1.1 and 1.4 both test optimistic locking on wallet updates - these will be combined
- Properties 2.1, 2.2, 2.3 all test configuration security - these will be combined into one comprehensive check
- Properties 5.1, 5.2, 5.3 all test cache TTL configuration - these will be combined
- Properties 6.2, 6.3, 6.4 all test specific rate limit configurations - these will be combined

### Core Properties

**Property 1: Optimistic locking prevents concurrent wallet modifications**
*For any* wallet and any two concurrent update operations, when both attempt to modify the wallet simultaneously, exactly one SHALL succeed and the other SHALL receive an OptimisticLockException
**Validates: Requirements 1.1, 1.4**

**Property 2: Negative and zero amounts are rejected**
*For any* transaction request with an amount less than or equal to zero, the system SHALL reject the request with a validation error before any processing occurs
**Validates: Requirements 1.2, 1.3**

**Property 3: Optimistic lock failures return clear error messages**
*For any* operation that fails due to an OptimisticLockException, the system SHALL return an error response containing a clear message indicating concurrent modification and appropriate HTTP status code
**Validates: Requirements 1.5**

**Property 4: Concurrent transactions maintain total balance invariant**
*For any* set of concurrent transactions, the sum of all wallet balances before the transactions SHALL equal the sum of all wallet balances after the transactions (minus fees collected by the system)
**Validates: Requirements 4.1**

**Property 5: Wallet operations are atomic**
*For any* wallet operation (credit or debit), either all changes SHALL be committed or all changes SHALL be rolled back - no partial updates SHALL occur
**Validates: Requirements 4.2**

**Property 6: JWT token round-trip preserves user identity**
*For any* valid user, generating a JWT token and then parsing it SHALL return the same user ID, email, and roles
**Validates: Requirements 4.3**

**Property 7: Unauthorized requests are rejected**
*For any* protected endpoint and any request without valid authentication, the system SHALL return HTTP 401 or 403
**Validates: Requirements 4.4**

**Property 8: CinetPay responses are handled correctly**
*For any* valid CinetPay response (success, failure, pending, timeout), the system SHALL update transaction status appropriately and not leave transactions in an inconsistent state
**Validates: Requirements 4.5**

**Property 9: Cache invalidation on wallet updates**
*For any* wallet balance modification, the system SHALL invalidate all cache entries related to that wallet
**Validates: Requirements 5.4**

**Property 10: Rate limit enforcement**
*For any* endpoint with rate limiting, when the request count exceeds the configured limit within the time window, the system SHALL return HTTP 429
**Validates: Requirements 6.1**

**Property 11: Rate limit headers are present**
*For any* rate-limited endpoint response, the response SHALL include headers indicating remaining quota and reset time
**Validates: Requirements 6.5**

**Property 12: Transaction amount validation**
*For any* transaction amount, the system SHALL validate that it is positive and does not exceed the maximum allowed limit based on user KYC level
**Validates: Requirements 7.1**

**Property 13: Email format validation**
*For any* email input, the system SHALL validate that it matches a valid email format pattern
**Validates: Requirements 7.2**

**Property 14: Phone number validation**
*For any* phone number input, the system SHALL validate that it matches the expected format for the target region
**Validates: Requirements 7.3**

**Property 15: Currency code validation**
*For any* currency code input, the system SHALL validate that it is one of the supported currencies (XAF, EUR, USD)
**Validates: Requirements 7.4**

**Property 16: Validation errors return field-level details**
*For any* request with validation errors, the system SHALL return a structured response containing error details for each invalid field
**Validates: Requirements 7.5, 10.1**

**Property 17: Business rule violations return specific error codes**
*For any* business rule violation (insufficient balance, transaction limit exceeded, etc.), the system SHALL return a specific error code and descriptive message
**Validates: Requirements 10.2**

**Property 18: Idempotency ensures single execution**
*For any* transaction submitted multiple times with the same idempotency key within 24 hours, the system SHALL execute the transaction only once
**Validates: Requirements 11.1**

**Property 19: Idempotent requests return consistent results**
*For any* transaction submitted multiple times with the same idempotency key, all requests SHALL return the same transaction result
**Validates: Requirements 11.2**

**Property 20: Transaction audit logging**
*For any* transaction created, the system SHALL create an audit log entry containing user ID, timestamp, transaction reference, amount, and type
**Validates: Requirements 12.1**

**Property 21: Wallet modification audit logging**
*For any* wallet balance modification, the system SHALL create an audit log entry containing the before and after balance values
**Validates: Requirements 12.2**

**Property 22: Admin action audit logging**
*For any* admin action performed, the system SHALL create an audit log entry containing admin ID, action type, and relevant details
**Validates: Requirements 12.3**

**Property 23: Security event audit logging**
*For any* security event (failed login, suspicious activity, etc.), the system SHALL create an audit log entry with appropriate severity level
**Validates: Requirements 12.4**

## Error Handling

### Error Response Structure

All errors will follow a consistent structure:

```json
{
  "timestamp": "2024-11-21T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Validation failed for request",
  "path": "/api/transactions/send",
  "correlationId": "abc-123-def",
  "errors": [
    {
      "field": "amount",
      "message": "Amount must be positive",
      "rejectedValue": -100
    }
  ]
}
```

### Error Categories

1. **Validation Errors (400)**
   - Invalid input format
   - Missing required fields
   - Constraint violations

2. **Authentication Errors (401)**
   - Missing or invalid JWT token
   - Expired token

3. **Authorization Errors (403)**
   - Insufficient permissions
   - KYC level too low

4. **Business Logic Errors (422)**
   - Insufficient balance
   - Transaction limit exceeded
   - Duplicate transaction

5. **Concurrency Errors (409)**
   - Optimistic lock failure
   - Resource conflict

6. **Rate Limiting Errors (429)**
   - Too many requests
   - Includes retry-after header

7. **Server Errors (500)**
   - Unexpected exceptions
   - Generic error message to client
   - Full stack trace in logs

### Retry Strategy

- **Optimistic Lock Failures**: Automatic retry up to 3 times with exponential backoff
- **Rate Limit Errors**: Client should respect Retry-After header
- **Idempotent Operations**: Safe to retry with same idempotency key
- **Network Errors**: Client should implement exponential backoff

## Testing Strategy

### Unit Testing

Unit tests will cover individual components and methods:

1. **Validation Tests**
   - Test each validator with valid and invalid inputs
   - Test boundary conditions (zero, negative, max values)

2. **Service Layer Tests**
   - Test business logic in isolation
   - Mock repository and external dependencies
   - Test error handling paths

3. **Repository Tests**
   - Test custom queries
   - Test optimistic locking behavior
   - Use @DataJpaTest for isolated database tests

4. **Security Tests**
   - Test JWT generation and validation
   - Test rate limiting logic
   - Test authentication and authorization rules

### Property-Based Testing

Property-based tests will verify universal properties across many generated inputs using **jqwik** (Java property-based testing library).

**Configuration**: Each property test will run a minimum of 100 iterations to ensure thorough coverage.

**Test Tagging**: Each property-based test will be tagged with a comment in this format:
```java
/**
 * Feature: mbotamapay-improvements, Property 1: Optimistic locking prevents concurrent wallet modifications
 */
```

**Key Property Tests**:

1. **Concurrency Properties**
   - Generate random wallet states and concurrent operations
   - Verify optimistic locking prevents conflicts
   - Verify total balance invariant holds

2. **Validation Properties**
   - Generate random valid and invalid inputs
   - Verify all invalid inputs are rejected
   - Verify error messages are consistent

3. **Idempotency Properties**
   - Generate random transactions with idempotency keys
   - Verify duplicate submissions return same result
   - Verify only one execution occurs

4. **Audit Properties**
   - Generate random operations
   - Verify all operations create audit logs
   - Verify audit logs contain required fields

5. **Cache Properties**
   - Generate random cache operations
   - Verify cache invalidation on updates
   - Verify TTL behavior

### Integration Testing

Integration tests will verify component interactions:

1. **API Integration Tests**
   - Test full request/response cycle
   - Test with real database (Testcontainers)
   - Test with real Redis (Testcontainers)
   - Verify security filters are applied

2. **Transaction Integration Tests**
   - Test complete transaction flow
   - Test concurrent transaction scenarios
   - Verify database consistency

3. **External Service Integration**
   - Test CinetPay integration with mock server
   - Test email service integration
   - Test error handling for service failures

### Test Environment

- **Framework**: JUnit 5 + Spring Boot Test
- **Property Testing**: jqwik 1.8.x
- **Mocking**: Mockito
- **Database**: Testcontainers (PostgreSQL)
- **Cache**: Testcontainers (Redis)
- **API Testing**: MockMvc + RestAssured

### Test Coverage Goals

- **Line Coverage**: Minimum 80%
- **Branch Coverage**: Minimum 75%
- **Critical Paths**: 100% (transaction processing, authentication, payment)

## Performance Considerations

### Database Optimization

1. **Indexing Strategy**
   - Add index on `transactions.idempotency_key`
   - Add composite index on `audit_logs(user_id, created_at)`
   - Add index on `transactions.created_at` for history queries

2. **Query Optimization**
   - Use pagination for large result sets
   - Implement query result caching for frequently accessed data
   - Use `@EntityGraph` to prevent N+1 queries

3. **Connection Pooling**
   - Configure HikariCP with appropriate pool size
   - Set connection timeout and max lifetime

### Caching Strategy

1. **Cache Layers**
   - L1: Spring Cache (Redis) for frequently accessed data
   - L2: Application-level cache for configuration

2. **Cache Keys**
   - User: `user:{userId}`
   - Wallet: `wallet:{walletId}`
   - Transaction History: `tx-history:{userId}:{page}`

3. **Cache Invalidation**
   - Invalidate on write operations
   - Use cache-aside pattern
   - Set appropriate TTLs

### Rate Limiting Performance

- Use in-memory Bucket4j for low latency
- Consider Redis-backed buckets for distributed deployments
- Implement sliding window algorithm for accurate rate limiting

## Security Considerations

### Secret Management

1. **Environment Variables**
   - All secrets must be provided via environment variables
   - No default secrets in production profile
   - Use Spring Boot's `@ConfigurationProperties` with validation

2. **Secret Rotation**
   - JWT secrets should be rotatable without downtime
   - Database credentials should support rotation
   - API keys should be externalized

### Authentication & Authorization

1. **JWT Security**
   - Use strong signing algorithm (HS512 or RS256)
   - Set appropriate expiration time
   - Include minimal claims (avoid PII)
   - Implement token refresh mechanism

2. **Password Security**
   - Use BCrypt with appropriate cost factor (12+)
   - Implement account lockout after failed attempts
   - Enforce password complexity requirements

### API Security

1. **Input Validation**
   - Validate all inputs at controller level
   - Use Bean Validation annotations
   - Sanitize inputs to prevent injection attacks

2. **Rate Limiting**
   - Implement per-IP and per-user rate limits
   - Use stricter limits for authentication endpoints
   - Return appropriate headers with rate limit info

3. **CORS Configuration**
   - Configure allowed origins explicitly
   - Avoid using wildcard in production
   - Set appropriate allowed methods and headers

### Audit & Compliance

1. **Audit Logging**
   - Log all sensitive operations
   - Include correlation IDs for tracing
   - Store logs securely with retention policy

2. **Data Privacy**
   - Mask sensitive data in logs
   - Implement data retention policies
   - Support GDPR compliance (data export, deletion)

## Deployment Strategy

### Environment Profiles

1. **Development (dev)**
   - H2 or local PostgreSQL
   - SQL logging enabled
   - Relaxed security for testing
   - Mock external services

2. **Testing (test)**
   - In-memory H2 database
   - Testcontainers for integration tests
   - Isolated test data

3. **Staging (staging)**
   - Production-like configuration
   - Separate database instance
   - Real external service integration
   - Performance testing environment

4. **Production (prod)**
   - Managed PostgreSQL (RDS, Cloud SQL)
   - Managed Redis (ElastiCache, Cloud Memorystore)
   - SQL logging disabled
   - Strict security configuration
   - Monitoring and alerting enabled

### Database Migration

1. **Flyway Configuration**
   - Migrations in `src/main/resources/db/migration`
   - Versioned migrations (V1__, V2__, etc.)
   - Repeatable migrations for views/procedures
   - Baseline existing database

2. **Migration Strategy**
   - Test migrations in staging first
   - Backup database before migration
   - Support rollback for failed migrations
   - Monitor migration execution time

### Monitoring & Observability

1. **Health Checks**
   - Liveness probe: `/actuator/health/liveness`
   - Readiness probe: `/actuator/health/readiness`
   - Custom health indicators for dependencies

2. **Metrics**
   - Expose Prometheus metrics at `/actuator/prometheus`
   - Track transaction count, success rate, latency
   - Monitor cache hit rate
   - Track rate limit violations

3. **Logging**
   - Structured logging (JSON format)
   - Include correlation IDs
   - Log levels: ERROR for production, DEBUG for dev
   - Centralized log aggregation (ELK, CloudWatch)

4. **Alerting**
   - Alert on high error rate
   - Alert on database connection failures
   - Alert on cache unavailability
   - Alert on unusual transaction patterns

## Migration Path

### Phase 1: Foundation (Week 1)
- Implement profile-based configuration
- Externalize secrets to environment variables
- Set up Flyway migrations
- Add optimistic locking to entities

### Phase 2: Security & Validation (Week 2)
- Implement enhanced validation
- Configure rate limiting
- Improve error handling
- Add security headers

### Phase 3: Reliability (Week 3)
- Implement idempotency service
- Add retry mechanisms
- Enhance audit logging
- Improve cache management

### Phase 4: Testing (Week 4)
- Write unit tests
- Implement property-based tests
- Add integration tests
- Achieve coverage goals

### Phase 5: Monitoring & Documentation (Week 5)
- Enhance health checks
- Configure metrics
- Update documentation
- Prepare deployment guides

## Dependencies

### New Dependencies

```kotlin
dependencies {
    // Property-Based Testing
    testImplementation("net.jqwik:jqwik:1.8.2")
    
    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    
    // Enhanced Validation
    implementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
    
    // Monitoring
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

### Version Updates

- Spring Boot: 3.2.0 → 3.3.5 (latest stable)
- JWT: 0.11.5 → 0.12.5 (latest)
- Bucket4j: 7.6.0 → 8.10.1 (latest)

## Backward Compatibility

All improvements are designed to be backward compatible:

- Existing API endpoints remain unchanged
- Database schema changes are additive (new columns with defaults)
- Idempotency is optional (backward compatible for clients not using it)
- Enhanced error responses maintain existing fields
- Rate limiting uses reasonable defaults

## Success Criteria

The improvements will be considered successful when:

1. ✅ All property-based tests pass with 100+ iterations
2. ✅ Test coverage exceeds 80% for line coverage
3. ✅ No secrets are exposed in configuration files
4. ✅ Optimistic locking prevents all concurrent modification issues
5. ✅ Rate limiting successfully prevents abuse
6. ✅ All transactions are idempotent
7. ✅ Audit logs capture all sensitive operations
8. ✅ Health checks accurately reflect system status
9. ✅ Documentation is accurate and complete
10. ✅ System passes load testing with 1000 concurrent users

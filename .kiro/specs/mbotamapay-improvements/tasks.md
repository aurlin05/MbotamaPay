# Implementation Plan - MbotamaPay Improvements

- [x] 1. Setup and Configuration Foundation



  - Update project dependencies to latest stable versions
  - Configure Flyway for database migrations
  - Create profile-specific configuration files (dev, test, staging, prod)
  - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3_

- [x] 1.1 Update dependencies in build.gradle.kts


  - Update Spring Boot from 3.2.0 to 3.3.5
  - Update JWT from 0.11.5 to 0.12.5
  - Update Bucket4j from 7.6.0 to 8.10.1
  - Add jqwik for property-based testing (1.8.2)
  - Add Testcontainers dependencies (1.19.3)
  - _Requirements: 3.1, 3.2, 3.3_

- [x] 1.2 Create profile-specific application configuration files


  - Create application-dev.yml with H2/local PostgreSQL config and SQL logging enabled
  - Create application-test.yml with H2 in-memory database
  - Create application-staging.yml with staging database config
  - Create application-prod.yml with production settings and SQL logging disabled
  - Update base application.yml to use environment variables for all secrets
  - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.5_



- [x] 1.3 Configure Flyway migrations




  - Enable Flyway in application.yml
  - Create baseline migration V1__baseline.sql from existing schema
  - Configure Flyway to use validate mode in production


  - _Requirements: 3.4, 3.5_

- [x] 1.4 Create AppProperties configuration class with validation








  - Create @ConfigurationProperties class for app.jwt, app.cinetpay, app.mail
  - Add @PostConstruct validation to ensure required properties are set
  - Configure application to fail fast if required environment variables are missing
  - _Requirements: 2.4, 2.5_
-

- [x] 2. Implement Optimistic Locking and Concurrency Control



  - Add @Version fields to Wallet and Transaction entities
  - Create ConcurrentModificationHandler for retry logic
  - Update WalletService to handle OptimisticLockException
  - _Requirements: 1.1, 1.4, 1.5_

- [x] 2.1 Add @Version field to Wallet entity


  - Add @Version private Long version field to Wallet.java
  - Create Flyway migration V2__add_version_to_wallet.sql
  - _Requirements: 1.1, 1.4_

- [x] 2.2 Add @Version field to Transaction entity


  - Add @Version private Long version field to Transaction.java
  - Create Flyway migration V3__add_version_to_transaction.sql
  - _Requirements: 1.1, 1.4_

- [x] 2.3 Create ConcurrentModificationHandler component


  - Implement retry logic with exponential backoff (max 3 retries)
  - Implement handleOptimisticLockException method to create clear error responses
  - _Requirements: 1.5_



- [x] 2.4 Update WalletService to use optimistic locking





  - Wrap wallet operations in retry logic using ConcurrentModificationHandler
  - Add proper exception handling for OptimisticLockException
  - _Requirements: 1.1, 1.4, 1.5_

- [ ]* 2.5 Write property test for optimistic locking
  - **Property 1: Optimistic locking prevents concurrent wallet modifications**
  - **Validates: Requirements 1.1, 1.4**

- [x] 3. Implement Enhanced Validation





  - Add validation annotations to entities
  - Create custom validators for transaction amounts, emails, phone numbers, currencies
  - Update GlobalExceptionHandler to return field-level validation errors

  - _Requirements: 1.2, 1.3, 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 3.1 Add validation annotations to Wallet entity

  - Add @Positive to balance field
  - Add @Pattern for currency field (XAF|EUR|USD)
  - _Requirements: 1.2, 7.4_


- [x] 3.2 Add validation annotations to Transaction entity

  - Add @Positive to amount field
  - Add @PositiveOrZero to fee field
  - _Requirements: 1.2, 1.3, 7.1_

- [x] 3.3 Create custom @ValidTransactionAmount validator


  - Implement ConstraintValidator to check amount is positive and within KYC limits
  - Apply validator to TransferRequest DTO
  - _Requirements: 1.2, 1.3, 7.1_


- [x] 3.4 Create custom @ValidEmail validator

  - Implement ConstraintValidator for email format validation
  - Apply to all DTOs with email fields
  - _Requirements: 7.2_



- [x] 3.5 Create custom @ValidPhoneNumber validator




  - Implement ConstraintValidator for phone number format validation
  - Support regional formats
  - Apply to all DTOs with phone fields


  - _Requirements: 7.3_

- [x] 3.6 Update GlobalExceptionHandler for validation errors





  - Enhance MethodArgumentNotValidException handler to return field-level details
  - Create ValidationErrorResponse with list of field errors
  - Include field name, rejected value, and error message for each error
  - _Requirements: 7.5, 10.1_

- [ ]* 3.7 Write property test for validation
  - **Property 2: Negative and zero amounts are rejected**
  - **Validates: Requirements 1.2, 1.3**

- [ ]* 3.8 Write property test for validation error responses
  - **Property 16: Validation errors return field-level details**
  - **Validates: Requirements 7.5, 10.1**

- [x] 4. Implement Idempotency Service







  - Create IdempotencyService interface and Redis implementation
  - Add idempotencyKey field to Transaction entity
  - Update TransactionService to use idempotency
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 4.1 Create IdempotencyRecord data class


  - Create IdempotencyRecord with key, result, status, timestamps
  - Create IdempotencyStatus enum (PROCESSING, COMPLETED, FAILED)
  - _Requirements: 11.5_



- [x] 4.2 Create IdempotencyService interface

  - Define methods: checkIdempotency, storeIdempotencyResult, isProcessing, markAsProcessing


  - _Requirements: 11.1, 11.2, 11.4_


- [x] 4.3 Implement RedisIdempotencyService





  - Implement IdempotencyService using RedisTemplate


  - Set TTL to 24 hours for idempotency records
  - Use Redis transactions for atomic operations
  - _Requirements: 11.1, 11.2, 11.3, 11.5_




- [x] 4.4 Add idempotencyKey field to Transaction entity




  - Add @Column(name = "idempotency_key", unique = true) String idempotencyKey


  - Create Flyway migration V4__add_idempotency_key_to_transaction.sql
  - Add unique index on idempotency_key

  - _Requirements: 11.1_

- [x] 4.5 Update TransferRequest DTO to include idempotencyKey


  - Add optional String idempotencyKey field
  - _Requirements: 11.1_

- [x] 4.6 Update TransactionService to use idempotency




  - Check idempotency before processing transaction
  - Mark as processing during transaction
  - Store result after successful transaction
  - Handle concurrent requests with same key
  - _Requirements: 11.1, 11.2, 11.4_

- [ ]* 4.7 Write property test for idempotency
  - **Property 18: Idempotency ensures single execution**
  - **Validates: Requirements 11.1**

- [ ]* 4.8 Write property test for idempotent result consistency
  - **Property 19: Idempotent requests return consistent results**
  - **Validates: Requirements 11.2**

- [x] 5. Enhance Rate Limiting




  - Configure rate limits for different endpoint categories
  - Update RateLimitFilter to include rate limit headers
  - Add configuration for per-IP and per-user limits
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 5.1 Create RateLimitConfig configuration class


  - Define rate limits for auth endpoints (5/min per IP)
  - Define rate limits for transaction endpoints (10/min per user)
  - Define rate limits for admin endpoints (20/min per admin)
  - Create BandwidthConfig helper class
  - _Requirements: 6.2, 6.3, 6.4_

- [x] 5.2 Update RateLimitFilter to use configuration


  - Inject RateLimitConfig
  - Implement getRateLimitKey to distinguish IP vs user-based limits
  - Implement resolveBucket to create appropriate buckets per endpoint
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 5.3 Add rate limit headers to responses

  - Add X-Rate-Limit-Remaining header with remaining quota
  - Add X-Rate-Limit-Reset header with reset timestamp
  - Add Retry-After header when limit exceeded
  - _Requirements: 6.5_


- [x] 5.4 Update error response for rate limiting




  - Return HTTP 429 when limit exceeded
  - Include retry-after information in error response body
  - _Requirements: 6.1, 10.5_

- [ ]* 5.5 Write property test for rate limiting
  - **Property 10: Rate limit enforcement**
  - **Validates: Requirements 6.1**

- [ ]* 5.6 Write property test for rate limit headers
  - **Property 11: Rate limit headers are present**
  - **Validates: Requirements 6.5**

- [x] 6. Implement Enhanced Cache Management





  - Create CacheService for cache invalidation
  - Configure cache TTLs per data type
  - Add cache invalidation on wallet updates
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 6.1 Create CacheService interface


  - Define methods: evictWalletCache, evictUserCache, evictTransactionHistoryCache
  - _Requirements: 5.4_


- [x] 6.2 Implement CacheServiceImpl

  - Inject CacheManager
  - Implement cache eviction methods using cache names
  - _Requirements: 5.4_



- [x] 6.3 Update CacheConfig with TTL configurations




  - Configure user cache with 30-minute TTL
  - Configure wallet cache with 5-minute TTL
  - Configure transaction history cache with 10-minute TTL
  - Configure LRU eviction policy


  - _Requirements: 5.1, 5.2, 5.3, 5.5_

- [x] 6.4 Add @Cacheable annotations to service methods





  - Add @Cacheable("users") to user lookup methods


  - Add @Cacheable("wallets") to wallet lookup methods
  - Add @Cacheable("transactionHistory") to transaction history methods
  - _Requirements: 5.1, 5.2, 5.3_

- [x] 6.5 Update WalletService to invalidate cache on updates




  - Call CacheService.evictWalletCache after credit/debit operations
  - Call CacheService.evictTransactionHistoryCache after transactions
  - _Requirements: 5.4_

- [ ]* 6.6 Write property test for cache invalidation
  - **Property 9: Cache invalidation on wallet updates**
  - **Validates: Requirements 5.4**

- [x] 7. Implement Enhanced Audit Logging



  - Create enhanced AuditLog entity with indexes
  - Implement AuditService with logging methods
  - Add audit logging to transaction, wallet, and admin operations
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [x] 7.1 Enhance AuditLog entity


  - Add severity field (INFO, WARNING, ERROR, CRITICAL)
  - Add ipAddress field
  - Add details field (JSONB for PostgreSQL)
  - Add indexes on user_id, created_at, action_type
  - Create Flyway migration V5__enhance_audit_log.sql
  - _Requirements: 12.4_



- [x] 7.2 Create AuditService interface






  - Define logTransaction method
  - Define logWalletModification method
  - Define logAdminAction method
  - Define logSecurityEvent method
  - Define queryAuditLogs method with filtering


  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [x] 7.3 Implement AuditServiceImpl




  - Implement all logging methods to create AuditLog entries
  - Capture current user from SecurityContext
  - Capture IP address from request
  - Store details as JSON




  - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [x] 7.4 Create AuditLogQuery DTO for filtering


  - Add fields: userId, actionType, startDate, endDate, severity
  - _Requirements: 12.5_



- [x] 7.5 Add audit logging to TransactionService



  - Call AuditService.logTransaction after each transaction
  - Include user ID, timestamp, reference, amount, type


  - _Requirements: 12.1_

- [x] 7.6 Add audit logging to WalletService





  - Call AuditService.logWalletModification on credit/debit
  - Include before and after balance values
  - _Requirements: 12.2_

- [x] 7.7 Add audit logging to AdminController



  - Call AuditService.logAdminAction for all admin operations
  - Include admin ID, action type, and relevant details
  - _Requirements: 12.3_

- [x] 7.8 Add audit logging to security events



  - Log failed login attempts in AuthService
  - Log suspicious activities (multiple failed attempts, unusual patterns)
  - Include severity levels
  - _Requirements: 12.4_

- [ ]* 7.9 Write property test for transaction audit logging
  - **Property 20: Transaction audit logging**
  - **Validates: Requirements 12.1**

- [ ]* 7.10 Write property test for wallet audit logging
  - **Property 21: Wallet modification audit logging**
  - **Validates: Requirements 12.2**

- [ ]* 7.11 Write property test for admin audit logging
  - **Property 22: Admin action audit logging**
  - **Validates: Requirements 12.3**

- [ ]* 7.12 Write property test for security audit logging
  - **Property 23: Security event audit logging**
  - **Validates: Requirements 12.4**

- [x] 8. Implement Enhanced Error Handling





  - Update GlobalExceptionHandler with all error categories
  - Create structured error response DTOs
  - Add correlation ID to all error responses
  - _Requirements: 1.5, 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 8.1 Create ErrorResponse DTO

  - Add fields: timestamp, status, error, code, message, path, correlationId
  - Add optional errors list for validation errors
  - _Requirements: 10.1, 10.2_

- [x] 8.2 Create FieldError DTO

  - Add fields: field, message, rejectedValue
  - _Requirements: 10.1_

- [x] 8.3 Update GlobalExceptionHandler for OptimisticLockException


  - Return HTTP 409 with retry-able error message
  - Include correlation ID
  - _Requirements: 1.5, 10.4_

- [x] 8.4 Update GlobalExceptionHandler for business exceptions

  - Map each business exception to specific error code
  - Return HTTP 422 for business rule violations
  - Include specific error codes (INSUFFICIENT_BALANCE, LIMIT_EXCEEDED, etc.)
  - _Requirements: 10.2_

- [x] 8.5 Update GlobalExceptionHandler for unexpected exceptions

  - Log full stack trace with correlation ID
  - Return generic error message to client
  - Return HTTP 500
  - _Requirements: 10.3_

- [x] 8.6 Add CorrelationIdFilter to generate correlation IDs

  - Generate UUID for each request
  - Store in MDC for logging
  - Include in all error responses
  - _Requirements: 10.1, 10.2, 10.3_

- [ ]* 8.7 Write property test for error response structure
  - **Property 3: Optimistic lock failures return clear error messages**
  - **Validates: Requirements 1.5**

- [ ]* 8.8 Write property test for business error codes
  - **Property 17: Business rule violations return specific error codes**
  - **Validates: Requirements 10.2**

- [ ] 9. Implement Enhanced Health Checks
  - Create EmailHealthIndicator
  - Update existing health indicators
  - Configure health check endpoints
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [ ] 9.1 Create EmailHealthIndicator
  - Implement HealthIndicator interface
  - Test email service connection
  - Return UP/DOWN status with details
  - _Requirements: 8.3_

- [ ] 9.2 Update DatabaseHealthIndicator
  - Add connection pool metrics
  - Add query execution test
  - _Requirements: 8.1_

- [ ] 9.3 Update RedisHealthIndicator
  - Add connection test
  - Add ping test
  - _Requirements: 8.2_

- [ ] 9.4 Configure health check aggregation
  - Configure overall health as DOWN if any dependency is DOWN
  - Configure liveness and readiness probes
  - _Requirements: 8.4_

- [ ] 10. Implement Enhanced Metrics
  - Configure custom metrics for transactions
  - Add metrics for cache performance
  - Add metrics for rate limiting
  - _Requirements: 8.5_

- [ ] 10.1 Update MetricsConfig with transaction metrics
  - Add counter for total transactions
  - Add counter for successful transactions
  - Add counter for failed transactions
  - Add timer for transaction processing time
  - _Requirements: 8.5_

- [ ] 10.2 Add cache metrics
  - Add counter for cache hits
  - Add counter for cache misses
  - Calculate and expose cache hit rate
  - _Requirements: 8.5_

- [ ] 10.3 Add rate limiting metrics
  - Add counter for rate limit violations
  - Add gauge for current rate limit usage
  - _Requirements: 8.5_

- [ ] 10.4 Update TransactionService to record metrics
  - Increment counters on transaction success/failure
  - Record processing time
  - _Requirements: 8.5_

- [ ] 11. Update Documentation
  - Update README with correct Gradle commands
  - Document all environment variables
  - Update docker-compose with health checks
  - Create API documentation
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [ ] 11.1 Update README.md
  - Replace all Maven commands with Gradle equivalents
  - Update build command to `./gradlew build`
  - Update run command to `./gradlew bootRun`
  - Update test command to `./gradlew test`
  - _Requirements: 9.1_

- [ ] 11.2 Create ENVIRONMENT_VARIABLES.md
  - Document all required environment variables
  - Document optional environment variables with defaults
  - Provide example values for each environment (dev, staging, prod)
  - _Requirements: 9.3_

- [ ] 11.3 Update docker-compose.yml
  - Add health check for PostgreSQL service
  - Add health check for Redis service
  - Configure health check intervals and timeouts
  - _Requirements: 9.4_

- [ ] 11.4 Update OpenAPI/Swagger documentation
  - Ensure all endpoints are documented
  - Document error responses for each endpoint
  - Document rate limiting headers
  - Document idempotency key usage
  - _Requirements: 9.2_

- [ ] 12. Write Comprehensive Unit Tests
  - Write unit tests for all new services
  - Write unit tests for validators
  - Write unit tests for filters
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 12.1 Write unit tests for ConcurrentModificationHandler
  - Test retry logic with successful retry
  - Test retry logic with max retries exceeded
  - Test error message generation
  - _Requirements: 1.5_

- [ ]* 12.2 Write unit tests for IdempotencyService
  - Test storing and retrieving idempotency records
  - Test TTL expiration
  - Test concurrent access handling
  - _Requirements: 11.1, 11.2, 11.3, 11.4_

- [ ]* 12.3 Write unit tests for custom validators
  - Test ValidTransactionAmount with valid and invalid amounts
  - Test ValidEmail with valid and invalid emails
  - Test ValidPhoneNumber with valid and invalid phone numbers
  - _Requirements: 7.1, 7.2, 7.3_

- [ ]* 12.4 Write unit tests for RateLimitFilter
  - Test rate limit enforcement
  - Test header generation
  - Test different endpoint configurations
  - _Requirements: 6.1, 6.5_

- [ ]* 12.5 Write unit tests for CacheService
  - Test cache eviction methods
  - Test cache key generation
  - _Requirements: 5.4_

- [ ]* 12.6 Write unit tests for AuditService
  - Test audit log creation for each operation type
  - Test query filtering
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [ ] 13. Write Property-Based Tests
  - Write property tests for concurrency
  - Write property tests for validation
  - Write property tests for idempotency
  - Write property tests for audit logging
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 13.1 Write property test for wallet balance invariant
  - **Property 4: Concurrent transactions maintain total balance invariant**
  - **Validates: Requirements 4.1**

- [ ]* 13.2 Write property test for atomic wallet operations
  - **Property 5: Wallet operations are atomic**
  - **Validates: Requirements 4.2**

- [ ]* 13.3 Write property test for JWT round-trip
  - **Property 6: JWT token round-trip preserves user identity**
  - **Validates: Requirements 4.3**

- [ ]* 13.4 Write property test for authentication enforcement
  - **Property 7: Unauthorized requests are rejected**
  - **Validates: Requirements 4.4**

- [ ]* 13.5 Write property test for CinetPay response handling
  - **Property 8: CinetPay responses are handled correctly**
  - **Validates: Requirements 4.5**

- [ ]* 13.6 Write property test for transaction amount validation
  - **Property 12: Transaction amount validation**
  - **Validates: Requirements 7.1**

- [ ]* 13.7 Write property test for email validation
  - **Property 13: Email format validation**
  - **Validates: Requirements 7.2**

- [ ]* 13.8 Write property test for phone validation
  - **Property 14: Phone number validation**
  - **Validates: Requirements 7.3**

- [ ]* 13.9 Write property test for currency validation
  - **Property 15: Currency code validation**
  - **Validates: Requirements 7.4**

- [ ] 14. Write Integration Tests
  - Write integration tests for transaction flow
  - Write integration tests for authentication
  - Write integration tests with Testcontainers
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 14.1 Setup Testcontainers configuration
  - Configure PostgreSQL Testcontainer
  - Configure Redis Testcontainer
  - Create base integration test class
  - _Requirements: 4.1, 4.2_

- [ ]* 14.2 Write integration test for complete transaction flow
  - Test P2P transfer from start to finish
  - Verify wallet balances are updated correctly
  - Verify transaction records are created
  - Verify audit logs are created
  - _Requirements: 4.1, 12.1, 12.2_

- [ ]* 14.3 Write integration test for concurrent transactions
  - Simulate multiple concurrent transactions
  - Verify optimistic locking works correctly
  - Verify total balance invariant holds
  - _Requirements: 1.1, 4.1_

- [ ]* 14.4 Write integration test for idempotency
  - Submit same transaction multiple times
  - Verify only one execution occurs
  - Verify same result is returned
  - _Requirements: 11.1, 11.2_

- [ ]* 14.5 Write integration test for rate limiting
  - Send requests exceeding rate limit
  - Verify 429 responses
  - Verify rate limit headers
  - _Requirements: 6.1, 6.5_

- [ ]* 14.6 Write integration test for authentication flow
  - Test registration, login, JWT generation
  - Test protected endpoint access
  - Test unauthorized access rejection
  - _Requirements: 4.3, 4.4_

- [ ] 15. Final Checkpoint - Ensure all tests pass
  - Run all unit tests and verify they pass
  - Run all property-based tests and verify they pass
  - Run all integration tests and verify they pass
  - Verify test coverage meets 80% threshold
  - Ensure all tests pass, ask the user if questions arise.

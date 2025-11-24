# Flyway Configuration Guide

## Overview

MbotamaPay uses Flyway for database migration management. This document explains the configuration and usage of Flyway across different environments.

## Requirements Addressed

This configuration addresses the following requirements:
- **Requirement 3.4**: Flyway migrations run in a controlled manner
- **Requirement 3.5**: JPA ddl-auto uses validate mode in production

## Configuration Summary

### Base Configuration (application.yml)

```yaml
spring:
  flyway:
    enabled: ${FLYWAY_ENABLED:true}
    baseline-on-migrate: true
    baseline-version: 0
    locations: classpath:db/migration
    validate-on-migrate: ${FLYWAY_VALIDATE_ON_MIGRATE:false}
    out-of-order: false
    placeholder-replacement: false
```

### Environment-Specific Configurations

#### Development (application-dev.yml)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # Allows Hibernate to update schema
  flyway:
    enabled: true
    validate-on-migrate: false  # Skip validation for faster development
    clean-disabled: false  # Allow clean operations
```

**Purpose**: Provides flexibility during development while maintaining migration history.

#### Test (application-test.yml)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Fresh schema for each test run
  flyway:
    enabled: false  # Disabled in favor of Hibernate schema generation
```

**Purpose**: Uses H2 in-memory database with Hibernate schema generation for fast test execution.

#### Staging (application-staging.yml)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Only validate, never modify
  flyway:
    enabled: true
    validate-on-migrate: true  # Validate migrations before applying
    clean-disabled: true  # Prevent accidental data loss
```

**Purpose**: Production-like environment for testing migrations before production deployment.

#### Production (application-prod.yml)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Only validate, never modify
  flyway:
    enabled: true
    validate-on-migrate: true  # Validate migrations before applying
    clean-disabled: true  # Prevent accidental data loss
    baseline-on-migrate: false  # Require explicit baseline
```

**Purpose**: Maximum safety - only applies validated migrations, never modifies schema automatically.

## Migration Files

All migration files are located in `src/main/resources/db/migration/`:

1. **V1__initial_schema.sql**: Creates all core tables
2. **V2__add_indexes.sql**: Adds performance indexes
3. **V3__add_missing_tables.sql**: Adds audit logs and supporting tables

## Key Configuration Properties Explained

### baseline-on-migrate: true
- Allows Flyway to baseline existing databases automatically
- Useful when introducing Flyway to an existing project
- Set to `false` in production for explicit control

### validate-on-migrate: true (Production/Staging)
- Validates all migrations before applying them
- Ensures migration checksums match
- Catches configuration errors before they affect the database
- **Addresses Requirement 3.4**: Ensures controlled migration application

### clean-disabled: true (Production/Staging)
- Prevents the `flyway clean` command from running
- Protects against accidental data loss
- Critical safety feature for production environments

### ddl-auto: validate (Production/Staging)
- Hibernate only validates the schema against entities
- Never creates, updates, or drops tables
- **Addresses Requirement 3.5**: Ensures JPA uses validate mode in production
- Provides an additional safety layer beyond Flyway

## Workflow

### Development Workflow
1. Make entity changes in code
2. Run application - Hibernate updates schema (ddl-auto: update)
3. Generate migration from schema changes
4. Test migration in clean database
5. Commit migration file

### Staging/Production Workflow
1. Deploy application with new migration
2. Flyway validates existing migrations (validate-on-migrate: true)
3. Flyway applies new migrations
4. Hibernate validates schema matches entities (ddl-auto: validate)
5. Application starts successfully

## Safety Features

1. **Checksum Validation**: Ensures migrations haven't been modified
2. **Clean Disabled**: Prevents accidental data deletion in production
3. **Validate Mode**: JPA never modifies production schema
4. **Baseline Support**: Safely introduces Flyway to existing databases
5. **Out-of-Order Disabled**: Enforces sequential migration application

## Troubleshooting

### Issue: "Validate failed: Migration checksum mismatch"
**Cause**: A migration file was modified after being applied
**Solution**: 
- Revert the change to the migration file, OR
- Create a new migration with the desired change

### Issue: "Schema validation failed"
**Cause**: Entity definitions don't match database schema
**Solution**:
- Ensure all migrations have been applied
- Check that entity annotations match database structure
- Create a new migration if schema changes are needed

### Issue: "Baseline required"
**Cause**: Existing database without Flyway history
**Solution**:
- Set `baseline-on-migrate: true` (already configured)
- Or manually run baseline command

## Best Practices

1. **Never modify applied migrations** - Always create new migrations
2. **Test in staging first** - Validate migrations before production
3. **Keep migrations small** - One logical change per migration
4. **Use transactions** - PostgreSQL supports DDL transactions
5. **Document complex migrations** - Add comments explaining the change
6. **Version control** - All migrations must be in version control

## Dependencies

The following dependencies are configured in `build.gradle.kts`:

```kotlin
implementation("org.flywaydb:flyway-core")
runtimeOnly("org.flywaydb:flyway-database-postgresql")
```

## Verification

To verify Flyway configuration:

1. **Check application startup logs** for Flyway migration messages
2. **Query flyway_schema_history table** to see applied migrations
3. **Run tests** to ensure configuration doesn't break test environment

```sql
-- Check migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

## Environment Variables

Flyway behavior can be controlled via environment variables:

- `FLYWAY_ENABLED`: Enable/disable Flyway (default: true)
- `FLYWAY_VALIDATE_ON_MIGRATE`: Enable validation (default: false, true in prod/staging)

## Conclusion

This Flyway configuration provides:
- ✅ Controlled database migrations (Requirement 3.4)
- ✅ Validate mode in production (Requirement 3.5)
- ✅ Safety features to prevent data loss
- ✅ Flexibility for development
- ✅ Production-ready migration management

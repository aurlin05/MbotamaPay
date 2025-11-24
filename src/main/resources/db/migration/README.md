# Flyway Database Migrations

This directory contains all database migration scripts for MbotamaPay.

## Migration Naming Convention

Flyway migrations follow the pattern: `V{version}__{description}.sql`

- **V**: Prefix for versioned migrations
- **{version}**: Sequential version number (e.g., 1, 2, 3)
- **__**: Double underscore separator
- **{description}**: Brief description using underscores (e.g., initial_schema, add_indexes)

## Existing Migrations

- **V1__initial_schema.sql**: Creates all core tables (users, wallets, transactions, etc.)
- **V2__add_indexes.sql**: Adds performance indexes on frequently queried columns
- **V3__add_missing_tables.sql**: Adds audit logs, password reset tokens, recurring payments, and support tickets

## Configuration by Environment

### Development (dev)
- Flyway enabled: `true`
- Validate on migrate: `false`
- Clean disabled: `false` (allows clean for development)
- JPA ddl-auto: `update` (for convenience during development)

### Test (test)
- Flyway enabled: `false`
- Uses H2 in-memory database with `create-drop`

### Staging (staging)
- Flyway enabled: `true`
- Validate on migrate: `true`
- Clean disabled: `true` (prevents accidental data loss)
- JPA ddl-auto: `validate`

### Production (prod)
- Flyway enabled: `true`
- Validate on migrate: `true` (validates migrations before applying)
- Clean disabled: `true` (prevents accidental data loss)
- Baseline on migrate: `false` (requires explicit baseline)
- JPA ddl-auto: `validate` (only validates, never modifies schema)

## Creating New Migrations

1. Create a new file with the next version number: `V4__your_description.sql`
2. Write your SQL DDL statements
3. Test in development environment first
4. Deploy to staging for validation
5. Apply to production

## Best Practices

- **Never modify existing migrations** once they've been applied to production
- **Always test migrations** in development and staging before production
- **Use transactions** where possible (PostgreSQL supports DDL transactions)
- **Include rollback scripts** as comments or separate files for complex migrations
- **Keep migrations small** and focused on a single change
- **Use IF NOT EXISTS** clauses for idempotent operations where appropriate

## Baseline

The baseline version is set to `0`, meaning V1 is the first migration. If you need to baseline an existing database:

```bash
./gradlew flywayBaseline
```

## Useful Flyway Commands

```bash
# Check migration status
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate

# Apply migrations
./gradlew flywayMigrate

# Repair migration history (use with caution)
./gradlew flywayRepair
```

## Troubleshooting

### Migration Checksum Mismatch
If you see a checksum mismatch error, it means a migration file was modified after being applied. Options:
1. Revert the change to the migration file
2. Use `flywayRepair` to update the checksum (only in development)
3. Create a new migration to apply the desired change

### Out of Order Migrations
By default, out-of-order migrations are disabled. If you need to insert a migration between existing ones, set `out-of-order: true` in configuration (not recommended for production).

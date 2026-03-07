# Using Liquibase Changesets for Tenant 2FA Configuration

## Overview

The file `0012_configure_tenant_2fa_optional.xml` contains optional Liquibase changesets that allow you to configure which tenants should have Two-Factor Authentication enabled.

**IMPORTANT**: These changesets are **OPTIONAL** and use Liquibase **contexts** to control when they run. They will NOT run automatically during normal application startup.

## Why Use Contexts?

Liquibase contexts allow you to:
- Control which changesets run in different environments
- Enable 2FA for specific tenants without modifying code
- Maintain configuration as code (Infrastructure as Code)
- Have a rollback strategy built-in

## Available Changesets

| Changeset ID | Context | Description |
|--------------|---------|-------------|
| `enable-2fa-production` | `enable-2fa-production` | Enable 2FA for 'production' tenant |
| `enable-2fa-multiple-tenants` | `enable-2fa-multiple` | Enable 2FA for tenant1, tenant2, tenant3 |
| `enable-2fa-all-tenants` | `enable-2fa-all` | Enable 2FA for ALL tenants |
| `disable-2fa-development` | `disable-2fa-development` | Disable 2FA for 'development' tenant |
| `enable-2fa-production-pattern` | `enable-2fa-production-pattern` | Enable 2FA for tenants matching 'prod' pattern |
| `enable-2fa-recent-tenants` | `enable-2fa-recent-tenants` | Enable 2FA for tenants created after 2024-01-01 |
| `enable-2fa-main-tenant` | `enable-2fa-by-name` | Enable 2FA for 'default' or 'main' tenant |
| `disable-2fa-test-staging` | `disable-2fa-test-staging` | Disable 2FA for dev, test, staging tenants |
| `enable-2fa-uat` | `enable-2fa-uat` | Enable 2FA for UAT tenant |
| `enable-2fa-pilot-tenant` | `enable-2fa-pilot` | Enable 2FA for pilot tenant |

## Usage Methods

### Method 1: Using Liquibase Command Line

#### Enable 2FA for Production Tenant
```bash
liquibase \
  --changeLogFile=db/changelog/tenant-store/parts/0012_configure_tenant_2fa_optional.xml \
  --url=jdbc:mysql://localhost:3306/fineract_tenants \
  --username=root \
  --password=mysql \
  --contexts=enable-2fa-production \
  update
```

#### Enable 2FA for Multiple Tenants
```bash
liquibase \
  --changeLogFile=db/changelog/tenant-store/parts/0012_configure_tenant_2fa_optional.xml \
  --url=jdbc:mysql://localhost:3306/fineract_tenants \
  --username=root \
  --password=mysql \
  --contexts=enable-2fa-multiple \
  update
```

#### Enable 2FA for ALL Tenants
```bash
liquibase \
  --changeLogFile=db/changelog/tenant-store/parts/0012_configure_tenant_2fa_optional.xml \
  --url=jdbc:mysql://localhost:3306/fineract_tenants \
  --username=root \
  --password=mysql \
  --contexts=enable-2fa-all \
  update
```

#### Multiple Contexts at Once
```bash
# Enable for production AND disable for development
liquibase \
  --changeLogFile=db/changelog/tenant-store/parts/0012_configure_tenant_2fa_optional.xml \
  --url=jdbc:mysql://localhost:3306/fineract_tenants \
  --username=root \
  --password=mysql \
  --contexts=enable-2fa-production,disable-2fa-development \
  update
```

### Method 2: Using Application Properties

Add to your `application.properties`:

```properties
# Enable specific contexts
spring.liquibase.contexts=enable-2fa-production,disable-2fa-development
```

Then start the application normally. The changesets matching those contexts will run.

### Method 3: Using Gradle

Add a task to your `build.gradle`:

```gradle
task enableTenantTwoFactor(type: JavaExec) {
    main = 'liquibase.integration.commandline.Main'
    classpath = sourceSets.main.runtimeClasspath
    args = [
        '--changeLogFile=src/main/resources/db/changelog/tenant-store/parts/0012_configure_tenant_2fa_optional.xml',
        '--url=jdbc:mysql://localhost:3306/fineract_tenants',
        '--username=root',
        '--password=mysql',
        '--contexts=enable-2fa-production',
        'update'
    ]
}
```

Run with:
```bash
./gradlew enableTenantTwoFactor
```

### Method 4: Manual SQL (Traditional Way)

If you prefer not to use Liquibase contexts, you can still use the SQL script:

```bash
mysql -u root -p fineract_tenants < enable_tenant_2fa.sql
```

Or execute specific statements manually.

## Common Scenarios

### Scenario 1: Production Deployment
Enable 2FA for production tenant only:
```bash
liquibase --contexts=enable-2fa-production update
```

### Scenario 2: Disable for Non-Production
Disable 2FA for test environments:
```bash
liquibase --contexts=disable-2fa-test-staging update
```

### Scenario 3: Gradual Rollout
Enable 2FA one tenant at a time:
```bash
# Phase 1: Pilot
liquibase --contexts=enable-2fa-pilot update

# Phase 2: UAT (after successful pilot)
liquibase --contexts=enable-2fa-uat update

# Phase 3: Production (after successful UAT)
liquibase --contexts=enable-2fa-production update
```

### Scenario 4: Enable for All Production-Like Tenants
Use pattern matching:
```bash
liquibase --contexts=enable-2fa-production-pattern update
```

## Rollback

Each changeset has a rollback strategy. To rollback:

```bash
liquibase \
  --changeLogFile=db/changelog/tenant-store/parts/0012_configure_tenant_2fa_optional.xml \
  --url=jdbc:mysql://localhost:3306/fineract_tenants \
  --username=root \
  --password=mysql \
  --contexts=enable-2fa-production \
  rollbackCount 1
```

## Verification

After running a changeset, verify the changes:

```sql
-- Check all tenants
SELECT identifier, name, two_factor_enabled 
FROM tenants 
ORDER BY identifier;

-- Check specific tenant
SELECT identifier, two_factor_enabled 
FROM tenants 
WHERE identifier = 'production';

-- Count by status
SELECT two_factor_enabled, COUNT(*) as count
FROM tenants
GROUP BY two_factor_enabled;
```

## Customizing Changesets

To customize for your tenant names:

1. Edit `0012_configure_tenant_2fa_optional.xml`
2. Modify the `<where>` clause in the desired changeset
3. Update the context name if desired
4. Run with the appropriate context

Example:
```xml
<changeSet author="fineract" id="enable-2fa-my-tenant" context="enable-2fa-my-tenant">
    <update tableName="tenants">
        <column name="two_factor_enabled" valueBoolean="true"/>
        <where>identifier = 'my_custom_tenant_name'</where>
    </update>
    <rollback>
        <update tableName="tenants">
            <column name="two_factor_enabled" valueBoolean="false"/>
            <where>identifier = 'my_custom_tenant_name'</where>
        </update>
    </rollback>
</changeSet>
```

## Best Practices

1. **Test First**: Always test in a non-production environment first
2. **Use Contexts**: Leverage contexts to control execution
3. **Verify**: Always verify changes after execution
4. **Document**: Keep track of which contexts you've run
5. **Rollback Plan**: Know how to rollback if needed
6. **Backup**: Backup database before running changesets

## Including in Main Changelog

If you want these changesets to be available (but not run automatically), you can add them to the main changelog:

Edit: `fineract-provider/src/main/resources/db/changelog/tenant-store/changelog-tenant-store.xml`

```xml
<!-- Optional 2FA configuration changesets (controlled by contexts) -->
<include file="parts/0012_configure_tenant_2fa_optional.xml" relativeToChangelogFile="true"/>
```

**Note**: Even when included, these changesets will only run if their context is specified.

## Troubleshooting

### Changeset Not Running
- Verify the context is specified correctly
- Check if the changeset has already been executed (check `DATABASECHANGELOG` table)
- Ensure you're connected to the correct database

### Changeset Already Executed
```sql
-- Check execution history
SELECT * FROM DATABASECHANGELOG 
WHERE ID LIKE '%2fa%';

-- To force re-run (use with caution)
-- DELETE FROM DATABASECHANGELOG WHERE ID = 'enable-2fa-production';
```

### Wrong Tenants Updated
Use rollback:
```bash
liquibase --contexts=<your-context> rollbackCount 1
```

## Summary

- ✅ Use contexts to control which changesets run
- ✅ Each changeset has a rollback strategy
- ✅ Changesets are optional and won't run automatically
- ✅ Can be customized for your specific tenant names
- ✅ Supports gradual rollout strategies
- ✅ Infrastructure as Code approach

For direct SQL execution without Liquibase, use the `enable_tenant_2fa.sql` file instead.

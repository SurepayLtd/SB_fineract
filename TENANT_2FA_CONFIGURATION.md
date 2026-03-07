# Tenant-Level Two-Factor Authentication Configuration

## Overview

This feature allows you to enable or disable Two-Factor Authentication (2FA) on a per-tenant basis. Previously, the `fineract.security.2fa.enabled` configuration applied globally to all tenants on the platform. Now, 2FA can be controlled at the tenant level while still respecting the global configuration.

## How It Works

For 2FA to be enforced for users in a tenant, **BOTH** conditions must be met:

1. **Global 2FA Flag**: `fineract.security.2fa.enabled` must be set to `true` in the application configuration
2. **Tenant 2FA Flag**: The tenant must have `two_factor_enabled` set to `true` in the `tenants` table

If either condition is false, 2FA will not be enforced for that tenant.

Users with the `BYPASS_TWO_FACTOR_PERMISSION` permission will never be required to use 2FA, regardless of tenant or global settings.

## Database Changes

A new column `two_factor_enabled` (BOOLEAN) has been added to the `tenants` table in the tenant store database.

### Migration

The database migration is automatically applied via Liquibase:
- **File**: `fineract-provider/src/main/resources/db/changelog/tenant-store/parts/0011_add_two_factor_enabled_column.xml`
- **Default Value**: `false` (2FA disabled by default for all tenants)

## Configuration

### Step 1: Enable Global 2FA

In your `application.properties` or environment variables:

```properties
fineract.security.2fa.enabled=true
```

### Step 2: Enable 2FA for Specific Tenants

Update the tenant record in the tenant store database:

```sql
-- Enable 2FA for a specific tenant
UPDATE tenants 
SET two_factor_enabled = true 
WHERE identifier = 'your_tenant_identifier';

-- Disable 2FA for a specific tenant
UPDATE tenants 
SET two_factor_enabled = false 
WHERE identifier = 'your_tenant_identifier';

-- Check current 2FA status for all tenants
SELECT identifier, name, two_factor_enabled 
FROM tenants;
```

## Implementation Details

### Code Changes

1. **Domain Model** (`FineractPlatformTenant.java`):
   - Added `twoFactorEnabled` field
   - Added getter method `isTwoFactorEnabled()`
   - Added constructor parameter for the new field

2. **Tenant Mapper** (`TenantMapper.java`):
   - Updated SQL query to include `two_factor_enabled` column
   - Updated `mapRow()` method to read and set the value

3. **Authentication Resources**:
   - `AuthenticationApiResource.java`: Updated to check tenant-specific 2FA configuration
   - `UserDetailsApiResource.java`: Updated to check tenant-specific 2FA configuration

### Logic Flow

```
User Login
    ↓
Check Global 2FA Flag (fineract.security.2fa.enabled)
    ↓
    If FALSE → 2FA Not Required
    ↓
    If TRUE → Check Tenant 2FA Flag
        ↓
        Get Current Tenant from ThreadLocalContextUtil
        ↓
        Check tenant.isTwoFactorEnabled()
            ↓
            If FALSE → 2FA Not Required
            ↓
            If TRUE → Check User Bypass Permission
                ↓
                If Has BYPASS_TWO_FACTOR_PERMISSION → 2FA Not Required
                ↓
                If No Bypass Permission → 2FA Required
```

## Usage Examples

### Example 1: Enable 2FA for Production Tenant Only

```sql
-- Enable 2FA for production tenant
UPDATE tenants SET two_factor_enabled = true WHERE identifier = 'production';

-- Keep 2FA disabled for development tenant
UPDATE tenants SET two_factor_enabled = false WHERE identifier = 'development';
```

### Example 2: Bulk Enable 2FA for All Tenants

```sql
-- Enable 2FA for all tenants
UPDATE tenants SET two_factor_enabled = true;
```

### Example 3: Selective 2FA Based on Tenant Type

```sql
-- Enable 2FA for specific tenants
UPDATE tenants 
SET two_factor_enabled = true 
WHERE identifier IN ('tenant1', 'tenant2', 'tenant3');
```

## Testing

1. **Test with 2FA Disabled Globally**:
   - Set `fineract.security.2fa.enabled=false`
   - Result: No tenant should require 2FA, regardless of tenant setting

2. **Test with 2FA Enabled Globally**:
   - Set `fineract.security.2fa.enabled=true`
   - Set tenant `two_factor_enabled=true`
   - Result: Users should be prompted for 2FA

3. **Test with 2FA Enabled Globally but Disabled for Tenant**:
   - Set `fineract.security.2fa.enabled=true`
   - Set tenant `two_factor_enabled=false`
   - Result: Users should NOT be prompted for 2FA

4. **Test with Bypass Permission**:
   - Enable 2FA globally and for tenant
   - Grant user `BYPASS_TWO_FACTOR_PERMISSION`
   - Result: User should NOT be prompted for 2FA

## Backward Compatibility

- **Default Behavior**: All existing tenants will have `two_factor_enabled=false` by default after migration
- **Existing Behavior**: If you had 2FA enabled globally before, you'll need to explicitly enable it for each tenant after this update
- **No Breaking Changes**: The global configuration still works as before; tenants just have an additional layer of control

## Troubleshooting

### Issue: 2FA not enforced even though both flags are enabled

**Possible causes**:
1. User has the `BYPASS_TWO_FACTOR_PERMISSION` permission
2. Tenant context not properly set in ThreadLocal
3. Database migration not applied

**Solution**:
```sql
-- Verify tenant 2FA status
SELECT identifier, two_factor_enabled FROM tenants WHERE identifier = 'your_tenant';

-- Verify user permissions
SELECT * FROM m_permission WHERE code = 'BYPASS_TWO_FACTOR_PERMISSION';
```

### Issue: All tenants stopped requiring 2FA after upgrade

**Cause**: Default value for new column is `false`

**Solution**: Enable 2FA for desired tenants:
```sql
UPDATE tenants SET two_factor_enabled = true WHERE identifier IN ('tenant1', 'tenant2');
```

## API Response Changes

The authentication response will include `twoFactorAuthenticationRequired` field:

```json
{
  "username": "user@example.com",
  "userId": 1,
  "authenticated": true,
  "twoFactorAuthenticationRequired": true,
  ...
}
```

This field will be `true` only if:
- Global 2FA is enabled (`fineract.security.2fa.enabled=true`)
- Tenant 2FA is enabled (`tenants.two_factor_enabled=true`)
- User doesn't have bypass permission

## Future Enhancements

Potential improvements for future releases:
1. Add API endpoints to manage tenant 2FA settings
2. Add UI in administration panel to toggle 2FA per tenant
3. Add audit logging for 2FA configuration changes
4. Add tenant-level 2FA statistics and reporting

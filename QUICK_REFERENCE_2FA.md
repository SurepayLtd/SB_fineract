# Quick Reference: Tenant-Level 2FA

## 🎯 What Changed?

2FA can now be enabled/disabled per tenant instead of globally for all tenants.

## 🔑 Key Points

1. **Two Flags Required**: BOTH must be true for 2FA to be enforced
   - Global: `fineract.security.2fa.enabled=true`
   - Tenant: `two_factor_enabled=true` in database

2. **Default Behavior**: All tenants default to `two_factor_enabled=false`

3. **Bypass Still Works**: Users with `BYPASS_TWO_FACTOR_PERMISSION` skip 2FA

## 📝 Quick Commands

### Check Tenant 2FA Status
```sql
SELECT identifier, two_factor_enabled FROM tenants;
```

### Enable 2FA for a Tenant
```sql
UPDATE tenants SET two_factor_enabled = true WHERE identifier = 'your_tenant';
```

### Disable 2FA for a Tenant
```sql
UPDATE tenants SET two_factor_enabled = false WHERE identifier = 'your_tenant';
```

## 🧪 Quick Test

1. Set `fineract.security.2fa.enabled=true` in config
2. Run: `UPDATE tenants SET two_factor_enabled = true WHERE identifier = 'test';`
3. Login to tenant 'test' → Should prompt for 2FA
4. Login to other tenants → Should NOT prompt for 2FA

## 📂 Modified Files

- ✅ Database: Added `two_factor_enabled` column to `tenants` table
- ✅ Domain: `FineractPlatformTenant.java` - Added field + getter
- ✅ Mapper: `TenantMapper.java` - Reads from database
- ✅ Auth: `AuthenticationApiResource.java` - Checks tenant config
- ✅ Auth: `UserDetailsApiResource.java` - Checks tenant config

## 🔍 How to Verify

```sql
-- Database level
DESCRIBE tenants;  -- Should show two_factor_enabled column

-- Data level
SELECT identifier, name, two_factor_enabled 
FROM tenants 
ORDER BY identifier;
```

## 🚨 Important Notes

- Migration runs automatically via Liquibase on startup
- All existing tenants will have 2FA disabled by default
- No breaking changes - backward compatible
- If you want old behavior (global 2FA), run: `UPDATE tenants SET two_factor_enabled = true;`

## 📚 Full Documentation

- **Complete Guide**: `TENANT_2FA_CONFIGURATION.md`
- **Checklist**: `IMPLEMENTATION_CHECKLIST.md`
- **SQL Scripts**: `enable_tenant_2fa.sql`

# Implementation Checklist: Tenant-Level 2FA Configuration

## ✅ Implementation Status: COMPLETE

All code changes have been successfully implemented. Follow the steps below to deploy and test.

---

## 📋 Pre-Deployment Checklist

### 1. Code Review
- [x] Database migration created (`0011_add_two_factor_enabled_column.xml`)
- [x] Migration added to changelog master
- [x] Domain model updated (`FineractPlatformTenant`)
- [x] Tenant mapper updated (`TenantMapper`)
- [x] Authentication logic updated (`AuthenticationApiResource`)
- [x] User details logic updated (`UserDetailsApiResource`)
- [x] All imports added correctly
- [x] No compilation errors (only style warnings)

### 2. Files Modified/Created

#### Database Migration
- ✅ `fineract-provider/src/main/resources/db/changelog/tenant-store/parts/0011_add_two_factor_enabled_column.xml`
- ✅ `fineract-provider/src/main/resources/db/changelog/tenant-store/changelog-tenant-store.xml`

#### Domain Layer
- ✅ `fineract-core/src/main/java/org/apache/fineract/infrastructure/core/domain/FineractPlatformTenant.java`
- ✅ `fineract-core/src/main/java/org/apache/fineract/infrastructure/core/service/tenant/TenantMapper.java`

#### API Layer
- ✅ `fineract-provider/src/main/java/org/apache/fineract/infrastructure/security/api/AuthenticationApiResource.java`
- ✅ `fineract-provider/src/main/java/org/apache/fineract/infrastructure/security/api/UserDetailsApiResource.java`

#### Documentation
- ✅ `TENANT_2FA_CONFIGURATION.md`
- ✅ `enable_tenant_2fa.sql`

---

## 🚀 Deployment Steps

### Step 1: Backup Database
```bash
# Backup tenant store database
mysqldump -u root -p fineract_tenants > fineract_tenants_backup_$(date +%Y%m%d).sql
```

### Step 2: Build Application
```bash
cd /home/mwesigye/WorkProject/SurePay/LatestRepo/BE/SB_fineract
./gradlew clean build -x test
```

### Step 3: Deploy Application
- Stop the application
- Deploy new build
- Start the application
- Liquibase will automatically run the migration on startup

### Step 4: Verify Migration
```sql
-- Connect to fineract_tenants database
USE fineract_tenants;

-- Check if column exists
DESCRIBE tenants;

-- Verify default values
SELECT identifier, two_factor_enabled FROM tenants;
```

### Step 5: Configure Tenants
```sql
-- Enable 2FA for specific tenants (example)
UPDATE tenants 
SET two_factor_enabled = true 
WHERE identifier IN ('production', 'main');

-- Verify
SELECT identifier, name, two_factor_enabled 
FROM tenants 
ORDER BY identifier;
```

---

## 🧪 Testing Checklist

### Test Scenario 1: Global 2FA Disabled
**Setup:**
```properties
fineract.security.2fa.enabled=false
```
**Expected:** No tenant should require 2FA regardless of tenant setting
- [ ] Login with tenant where `two_factor_enabled=true` → No 2FA prompt
- [ ] Login with tenant where `two_factor_enabled=false` → No 2FA prompt

### Test Scenario 2: Global 2FA Enabled, Tenant 2FA Disabled
**Setup:**
```properties
fineract.security.2fa.enabled=true
```
```sql
UPDATE tenants SET two_factor_enabled = false WHERE identifier = 'test_tenant';
```
**Expected:** 2FA should NOT be required
- [ ] Login with user → No 2FA prompt
- [ ] Check API response: `twoFactorAuthenticationRequired: false`

### Test Scenario 3: Global 2FA Enabled, Tenant 2FA Enabled
**Setup:**
```properties
fineract.security.2fa.enabled=true
```
```sql
UPDATE tenants SET two_factor_enabled = true WHERE identifier = 'test_tenant';
```
**Expected:** 2FA should be required
- [ ] Login with user → 2FA prompt appears
- [ ] Check API response: `twoFactorAuthenticationRequired: true`
- [ ] Complete 2FA flow successfully

### Test Scenario 4: User with Bypass Permission
**Setup:**
```properties
fineract.security.2fa.enabled=true
```
```sql
UPDATE tenants SET two_factor_enabled = true WHERE identifier = 'test_tenant';
-- Grant user BYPASS_TWO_FACTOR_PERMISSION
```
**Expected:** 2FA should NOT be required
- [ ] Login with bypass user → No 2FA prompt
- [ ] Check API response: `twoFactorAuthenticationRequired: false`

### Test Scenario 5: Multiple Tenants
**Setup:**
```sql
UPDATE tenants SET two_factor_enabled = true WHERE identifier = 'tenant_a';
UPDATE tenants SET two_factor_enabled = false WHERE identifier = 'tenant_b';
```
**Expected:** Each tenant behaves independently
- [ ] Login to tenant_a → 2FA required
- [ ] Login to tenant_b → 2FA not required
- [ ] Switch between tenants → Correct 2FA behavior for each

---

## 🔍 Verification Queries

### Check Current Configuration
```sql
-- View all tenants and their 2FA status
SELECT 
    id,
    identifier,
    name,
    two_factor_enabled,
    created_date,
    lastmodified_date
FROM tenants
ORDER BY identifier;

-- Count tenants by 2FA status
SELECT 
    two_factor_enabled,
    COUNT(*) as count,
    GROUP_CONCAT(identifier) as tenant_identifiers
FROM tenants
GROUP BY two_factor_enabled;

-- Find tenants with 2FA enabled
SELECT identifier, name 
FROM tenants 
WHERE two_factor_enabled = true;
```

### Check User Permissions
```sql
-- Check if bypass permission exists in tenant schema
USE fineract_default;  -- Replace with your tenant schema

SELECT * FROM m_permission 
WHERE code = 'BYPASS_TWO_FACTOR_PERMISSION';

-- Check users with bypass permission
SELECT 
    u.username,
    u.email,
    r.name as role_name
FROM m_appuser u
JOIN m_appuser_role ur ON ur.appuser_id = u.id
JOIN m_role r ON r.id = ur.role_id
JOIN m_role_permission rp ON rp.role_id = r.id
JOIN m_permission p ON p.id = rp.permission_id
WHERE p.code = 'BYPASS_TWO_FACTOR_PERMISSION';
```

---

## 🐛 Troubleshooting

### Issue 1: Migration Not Applied
**Symptoms:** Column `two_factor_enabled` doesn't exist in `tenants` table

**Solution:**
```sql
-- Check Liquibase changelog
SELECT * FROM schema_version 
WHERE script LIKE '%0011_add_two_factor_enabled%';

-- If not found, manually apply:
ALTER TABLE tenants 
ADD COLUMN two_factor_enabled BOOLEAN DEFAULT false NOT NULL;
```

### Issue 2: 2FA Not Required Despite Flags Being True
**Check:**
1. Verify global flag: `fineract.security.2fa.enabled=true`
2. Verify tenant flag: `SELECT two_factor_enabled FROM tenants WHERE identifier = 'your_tenant'`
3. Verify user doesn't have bypass permission
4. Check application logs for errors

### Issue 3: All Tenants Suddenly Don't Require 2FA
**Cause:** Migration set default to `false` for all tenants

**Solution:**
```sql
-- Re-enable for desired tenants
UPDATE tenants SET two_factor_enabled = true;
-- OR for specific tenants
UPDATE tenants SET two_factor_enabled = true WHERE identifier IN ('prod', 'main');
```

### Issue 4: NullPointerException When Getting Tenant
**Check:** Ensure tenant context is properly set in ThreadLocal
```java
// In code, verify:
FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant();
if (tenant == null) {
    // Handle null case
}
```

---

## 📊 Monitoring

### Metrics to Track
- Number of 2FA challenges issued per tenant
- 2FA success/failure rates per tenant
- Tenants with 2FA enabled vs disabled
- Users with bypass permission

### Log Messages to Monitor
```
Search for:
- "two_factor_enabled"
- "twoFactorAuthenticationRequired"
- "BYPASS_TWO_FACTOR_PERMISSION"
- "ThreadLocalContextUtil.getTenant()"
```

---

## 📝 Rollback Plan

If issues occur, you can rollback:

### Option 1: Disable 2FA Globally
```properties
fineract.security.2fa.enabled=false
```

### Option 2: Disable 2FA for All Tenants
```sql
UPDATE tenants SET two_factor_enabled = false;
```

### Option 3: Full Rollback (if needed)
```sql
-- Remove the column (not recommended)
ALTER TABLE tenants DROP COLUMN two_factor_enabled;

-- Restore code from previous commit
git revert <commit_hash>
```

---

## ✅ Sign-off Checklist

- [ ] Code changes reviewed and approved
- [ ] Database backup completed
- [ ] Application built successfully
- [ ] Migration executed successfully
- [ ] All test scenarios passed
- [ ] Documentation updated
- [ ] Team trained on new feature
- [ ] Monitoring configured
- [ ] Rollback plan documented and tested

---

## 📚 Additional Resources

- **Feature Documentation**: `TENANT_2FA_CONFIGURATION.md`
- **SQL Helper Script**: `enable_tenant_2fa.sql`
- **Migration File**: `0011_add_two_factor_enabled_column.xml`

---

## 🎯 Success Criteria

✅ The implementation is successful when:
1. Each tenant can independently enable/disable 2FA
2. Global 2FA flag still controls overall 2FA availability
3. Users with bypass permission are never required to use 2FA
4. Multiple tenants coexist with different 2FA settings
5. No impact on existing functionality
6. All tests pass
7. Documentation is complete

---

## 👥 Support

For issues or questions:
1. Review `TENANT_2FA_CONFIGURATION.md`
2. Check troubleshooting section above
3. Review application logs
4. Contact development team

---

**Implementation Date**: March 7, 2026
**Version**: 1.0
**Status**: ✅ COMPLETE - Ready for Testing

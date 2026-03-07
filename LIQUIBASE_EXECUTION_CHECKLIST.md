# Liquibase Changesets - Implementation Checklist

## ✅ Pre-Execution Checklist

- [ ] Read `LIQUIBASE_TENANT_2FA_GUIDE.md`
- [ ] Review `LIQUIBASE_QUICK_REF.txt`
- [ ] Backup `fineract_tenants` database
- [ ] Identify target tenants for 2FA
- [ ] Choose appropriate context(s)
- [ ] Test in non-production first

## 📋 Execution Steps

### Step 1: Verify Prerequisites
```bash
# Check database connection
mysql -u root -p fineract_tenants -e "SELECT COUNT(*) FROM tenants;"

# Verify column exists
mysql -u root -p fineract_tenants -e "DESCRIBE tenants;" | grep two_factor_enabled

# Check Liquibase is available
liquibase --version
```
- [ ] Database accessible
- [ ] Column exists (migration ran)
- [ ] Liquibase available

### Step 2: Choose Your Context

Select one or more contexts:

- [ ] `enable-2fa-production` - Production tenant only
- [ ] `enable-2fa-multiple` - Multiple specific tenants
- [ ] `enable-2fa-all` - All tenants (⚠️ CAUTION)
- [ ] `disable-2fa-development` - Disable for dev
- [ ] `enable-2fa-production-pattern` - All prod-like tenants
- [ ] `enable-2fa-recent-tenants` - Recently created tenants
- [ ] `enable-2fa-by-name` - Default/main tenant
- [ ] `disable-2fa-test-staging` - Disable test environments
- [ ] `enable-2fa-uat` - UAT tenant
- [ ] `enable-2fa-pilot` - Pilot tenant

### Step 3: Customize (If Needed)

If your tenant names differ from defaults:

1. [ ] Open `0012_configure_tenant_2fa_optional.xml`
2. [ ] Find the relevant changeset
3. [ ] Update the `<where>` clause with your tenant identifier
4. [ ] Save the file

Example:
```xml
<where>identifier = 'your_tenant_name'</where>
```

### Step 4: Execute Changeset

Run the command:
```bash
liquibase \
  --changeLogFile=fineract-provider/src/main/resources/db/changelog/tenant-store/parts/0012_configure_tenant_2fa_optional.xml \
  --url=jdbc:mysql://localhost:3306/fineract_tenants \
  --username=root \
  --password=mysql \
  --contexts=YOUR_CONTEXT_HERE \
  update
```

- [ ] Command executed successfully
- [ ] No errors in output

### Step 5: Verify Changes

```sql
-- Check all tenants
SELECT identifier, name, two_factor_enabled 
FROM tenants 
ORDER BY identifier;

-- Check specific tenant
SELECT two_factor_enabled 
FROM tenants 
WHERE identifier = 'YOUR_TENANT';

-- Count by status
SELECT two_factor_enabled, COUNT(*) as count
FROM tenants
GROUP BY two_factor_enabled;

-- Check execution history
SELECT * FROM DATABASECHANGELOG 
WHERE ID LIKE '%2fa%'
ORDER BY DATEEXECUTED DESC;
```

- [ ] Tenant 2FA status is correct
- [ ] Changeset recorded in DATABASECHANGELOG
- [ ] Count matches expectations

### Step 6: Test Authentication

1. [ ] Ensure `fineract.security.2fa.enabled=true` in config
2. [ ] Login to tenant with 2FA enabled → Should prompt for 2FA
3. [ ] Login to tenant with 2FA disabled → Should NOT prompt
4. [ ] Test user with bypass permission → Should NOT prompt

## 🔄 Rollback Procedure (If Needed)

If you need to undo the changes:

```bash
liquibase \
  --changeLogFile=fineract-provider/src/main/resources/db/changelog/tenant-store/parts/0012_configure_tenant_2fa_optional.xml \
  --url=jdbc:mysql://localhost:3306/fineract_tenants \
  --username=root \
  --password=mysql \
  --contexts=YOUR_CONTEXT_HERE \
  rollbackCount 1
```

- [ ] Rollback executed (if needed)
- [ ] Changes reverted
- [ ] Verification query confirms rollback

## 📊 Common Deployment Scenarios

### Scenario A: Production Only
- [ ] Context: `enable-2fa-production`
- [ ] Expected: Only 'production' tenant has 2FA
- [ ] Verified: ✓

### Scenario B: All Production Environments
- [ ] Context: `enable-2fa-production,enable-2fa-uat`
- [ ] Expected: Production and UAT have 2FA
- [ ] Verified: ✓

### Scenario C: Enable All, Disable Dev/Test
- [ ] Context: `enable-2fa-all,disable-2fa-test-staging`
- [ ] Expected: All tenants enabled except dev/test/staging
- [ ] Verified: ✓

### Scenario D: Gradual Rollout
- [ ] Phase 1: `enable-2fa-pilot` - Tested: ___
- [ ] Phase 2: `enable-2fa-uat` - Tested: ___
- [ ] Phase 3: `enable-2fa-production` - Tested: ___

## 🐛 Troubleshooting

### Issue: Changeset not executing
- [ ] Verify context name is correct
- [ ] Check database connection
- [ ] Verify changeset not already executed (check DATABASECHANGELOG)
- [ ] Check for typos in command

### Issue: Wrong tenants affected
- [ ] Review the changeset WHERE clause
- [ ] Check tenant identifiers in database
- [ ] Execute rollback if needed
- [ ] Customize changeset for your tenant names

### Issue: Cannot connect to database
- [ ] Verify database URL
- [ ] Check credentials
- [ ] Ensure database is running
- [ ] Test with mysql command line

## 📝 Documentation Reference

Quick answers:
- [ ] Read `LIQUIBASE_QUICK_REF.txt`

Detailed guide:
- [ ] Read `LIQUIBASE_TENANT_2FA_GUIDE.md`

Feature overview:
- [ ] Read `TENANT_2FA_CONFIGURATION.md`

## ✅ Post-Implementation

- [ ] Document which contexts were executed
- [ ] Update deployment runbook
- [ ] Inform operations team
- [ ] Monitor application logs
- [ ] Track 2FA usage metrics
- [ ] Schedule review after 1 week

## 📋 Execution Log

Record your executions here:

| Date | Context | Tenants Affected | Executed By | Result | Notes |
|------|---------|------------------|-------------|--------|-------|
| ____ | _______ | ________________ | ___________ | ______ | _____ |
| ____ | _______ | ________________ | ___________ | ______ | _____ |
| ____ | _______ | ________________ | ___________ | ______ | _____ |

## 🎯 Success Criteria

- [ ] All target tenants have correct 2FA status
- [ ] No unintended tenants affected
- [ ] Authentication works as expected
- [ ] Changes recorded in DATABASECHANGELOG
- [ ] Rollback tested (optional but recommended)
- [ ] Documentation updated
- [ ] Team informed

## 📞 Support

If issues arise:
1. Check `LIQUIBASE_TENANT_2FA_GUIDE.md` troubleshooting section
2. Review execution logs
3. Verify database state
4. Check application logs
5. Contact development team

---

**Checklist Version**: 1.0  
**Last Updated**: March 7, 2026  
**Status**: Ready for Use

---

✅ When all items are checked, the Liquibase implementation is complete!

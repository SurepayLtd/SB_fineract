-- =========================================
-- Enable Two-Factor Authentication Per Tenant
-- =========================================
--
-- This script helps you configure 2FA for specific tenants.
-- Run this against the fineract_tenants database (tenant store).
--
-- IMPORTANT: The global flag fineract.security.2fa.enabled must also be true
--            for 2FA to be enforced.
--
-- =========================================
-- LIQUIBASE ALTERNATIVE
-- =========================================
-- For a more structured approach using Liquibase changesets with contexts,
-- see the file:
--   fineract-provider/src/main/resources/db/changelog/tenant-store/parts/
--   0012_configure_tenant_2fa_optional.xml
--
-- Documentation: LIQUIBASE_TENANT_2FA_GUIDE.md
--
-- Example Liquibase usage:
--   liquibase --contexts=enable-2fa-production update
-- =========================================
--

-- =========================================
-- 1. CHECK CURRENT 2FA STATUS
-- =========================================
-- View current 2FA configuration for all tenants
SELECT
    id,
    identifier,
    name,
    two_factor_enabled,
    timezone_id,
    created_date
FROM tenants
ORDER BY identifier;


-- =========================================
-- 2. ENABLE 2FA FOR SPECIFIC TENANTS
-- =========================================

-- Enable 2FA for a single tenant (replace 'default' with your tenant identifier)
-- UPDATE tenants
-- SET two_factor_enabled = true
-- WHERE identifier = 'default';

-- Enable 2FA for multiple specific tenants
-- UPDATE tenants
-- SET two_factor_enabled = true
-- WHERE identifier IN ('tenant1', 'tenant2', 'tenant3');

-- Enable 2FA for all tenants
-- UPDATE tenants
-- SET two_factor_enabled = true;


-- =========================================
-- 3. DISABLE 2FA FOR SPECIFIC TENANTS
-- =========================================

-- Disable 2FA for a single tenant
-- UPDATE tenants
-- SET two_factor_enabled = false
-- WHERE identifier = 'development';

-- Disable 2FA for all tenants
-- UPDATE tenants
-- SET two_factor_enabled = false;


-- =========================================
-- 4. CONDITIONAL UPDATES
-- =========================================

-- Enable 2FA only for production-like tenants (example pattern)
-- UPDATE tenants
-- SET two_factor_enabled = true
-- WHERE identifier LIKE '%prod%' OR identifier LIKE '%production%';

-- Enable 2FA for tenants created after a certain date
-- UPDATE tenants
-- SET two_factor_enabled = true
-- WHERE created_date >= '2024-01-01';


-- =========================================
-- 5. VERIFICATION QUERIES
-- =========================================

-- Count tenants with 2FA enabled
SELECT
    two_factor_enabled,
    COUNT(*) as tenant_count
FROM tenants
GROUP BY two_factor_enabled;

-- List tenants with 2FA enabled
SELECT
    identifier,
    name,
    two_factor_enabled
FROM tenants
WHERE two_factor_enabled = true
ORDER BY identifier;

-- List tenants with 2FA disabled
SELECT
    identifier,
    name,
    two_factor_enabled
FROM tenants
WHERE two_factor_enabled = false
ORDER BY identifier;


-- =========================================
-- USAGE EXAMPLES
-- =========================================

-- Example 1: Enable 2FA for production tenant only
-- UPDATE tenants SET two_factor_enabled = true WHERE identifier = 'production';
-- UPDATE tenants SET two_factor_enabled = false WHERE identifier = 'development';

-- Example 2: Enable 2FA for all except test/dev tenants
-- UPDATE tenants SET two_factor_enabled = true;
-- UPDATE tenants SET two_factor_enabled = false WHERE identifier IN ('dev', 'test', 'staging');

-- Example 3: Gradual rollout - enable for one tenant at a time
-- UPDATE tenants SET two_factor_enabled = true WHERE identifier = 'pilot_tenant';
-- -- Test and monitor
-- UPDATE tenants SET two_factor_enabled = true WHERE identifier = 'next_tenant';

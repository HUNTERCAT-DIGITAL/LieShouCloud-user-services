-- 2026-08-31 · 手机号租户内唯一(方案 B):uk_users_phone(全局)→ uk_users_tenant_phone(tenant_id, phone)
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_phone;
ALTER TABLE users ADD CONSTRAINT uk_users_tenant_phone UNIQUE (tenant_id, phone);

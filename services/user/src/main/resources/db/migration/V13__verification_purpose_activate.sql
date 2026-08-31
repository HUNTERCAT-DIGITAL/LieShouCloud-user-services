-- 2026-08-31 · 首次登录激活:verification_codes.purpose 增加 ACTIVATE
ALTER TABLE verification_codes DROP CONSTRAINT ck_vc_purpose;
ALTER TABLE verification_codes ADD CONSTRAINT ck_vc_purpose CHECK (purpose IN ('LOGIN', 'REGISTER', 'RESET_PASSWORD', 'ACTIVATE'));
COMMENT ON COLUMN verification_codes.purpose IS '用途：LOGIN / REGISTER / RESET_PASSWORD / ACTIVATE(首次激活)';

-- 2026-08-31 · 首次登录激活：管理员建用户可不设密码(password_hash 可空,验证码激活时设置)
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

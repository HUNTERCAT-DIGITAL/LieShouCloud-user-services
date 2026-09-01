-- 用户头像（2026-09 头像功能）：avatar_url 存上传文件路径或默认头像标识
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(2048);

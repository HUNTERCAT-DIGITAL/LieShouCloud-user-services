-- ============================================================
-- V10 · 站内通知（notifications）
--
-- 开源版消息通知模块：平台/业务事件推送给租户内用户（站内信）。
--  - tenant_id + user_id 定位接收者（租户隔离，与 user 服务一致）
--  - type：SYSTEM / APPROVAL / AUDIT 等（预留业务扩展）
--  - read_at 为空 = 未读；已读时间戳记录
-- 幂等：IF NOT EXISTS。
-- ============================================================

CREATE TABLE IF NOT EXISTS notifications (
  id          BIGSERIAL PRIMARY KEY,
  tenant_id   BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  type        VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
  title       VARCHAR(200) NOT NULL,
  content     TEXT NOT NULL DEFAULT '',
  biz_type    VARCHAR(64),
  biz_id      BIGINT,
  read_at     TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read
  ON notifications (tenant_id, user_id, read_at, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_created
  ON notifications (tenant_id, created_at DESC);

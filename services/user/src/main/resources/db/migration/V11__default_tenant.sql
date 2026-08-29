-- ============================================================
-- V11 · 默认租户统一为 default（2026-08-29 全局决策）
--
-- 背景：历史库中默认租户 code='huntercat'（南昌猎手猫数字科技有限公司），
--       与前端契约 DEFAULT_TENANT_CODE、framework auth.default-tenant-code 不一致。
-- 决策：无特别说明的默认租户均为 'default'（前端 / 接口 / 数据统一）。
-- 处理：已有库 huntercat → default；新库走 V2（仍插入 huntercat）再经本迁移收敛。
-- 注意：V2 为已执行历史迁移，内容不可改（checksum 校验）；新增迁移为唯一收敛点。
-- ============================================================

UPDATE tenants
SET code = 'default', name = '默认租户'
WHERE code = 'huntercat';

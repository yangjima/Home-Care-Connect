-- 已有库手工执行：为 sys_user 增加首次/最近登录时间（与 user-service 实体字段对应）
ALTER TABLE sys_user
    ADD COLUMN first_login_at DATETIME NULL COMMENT '首次登录时间' AFTER updated_at,
    ADD COLUMN last_login_at DATETIME NULL COMMENT '最近登录时间' AFTER first_login_at,
    ADD INDEX idx_last_login_at (last_login_at);

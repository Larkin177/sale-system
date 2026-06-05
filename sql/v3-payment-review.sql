-- ==================== v3: 双模式支付支持 ====================

-- 订单表新增审核相关字段
ALTER TABLE orders ADD COLUMN reviewed_by BIGINT NULL COMMENT '审核人ID(管理员)';
ALTER TABLE orders ADD COLUMN review_note VARCHAR(200) COMMENT '审核备注';
ALTER TABLE orders ADD COLUMN reviewed_at TIMESTAMP NULL COMMENT '审核时间';
ALTER TABLE orders ADD INDEX idx_reviewed_by (reviewed_by);

-- 系统配置新增支付模式开关
INSERT INTO system_config (config_key, config_value, description) VALUES
('wechat_pay_mode', 'static', '微信支付模式: static(静态二维码)/api(官方API)'),
('alipay_pay_mode', 'static', '支付宝模式: static(静态二维码)/api(官方API)')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);

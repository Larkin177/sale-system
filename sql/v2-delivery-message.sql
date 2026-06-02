-- 发货消息模板配置
-- 支持变量: {site_name}, {order_no}, {amount}, {phone}, {download_url}

-- 扩展 config_value 字段长度以容纳模板文本
ALTER TABLE system_config MODIFY COLUMN config_value TEXT NOT NULL COMMENT '配置值';

-- 插入发货消息模板
INSERT INTO system_config (config_key, config_value, description) VALUES
('delivery_template', '【{site_name}】感谢您的购买！\n\n下载链接: {download_url}\n订单号: {order_no}\n金额: ¥{amount}\n\n如有问题请联系客服。', '发货消息模板')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);

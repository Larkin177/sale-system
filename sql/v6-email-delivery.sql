-- v6: 邮箱替换手机号 + 套餐发货模板 + QQ邮箱SMTP

-- ==================== orders 表：邮箱字段 ====================
ALTER TABLE orders ADD COLUMN customer_email VARCHAR(100) COMMENT '客户邮箱';
ALTER TABLE orders ADD INDEX idx_customer_email (customer_email);

-- ==================== sales 表：邮箱字段 ====================
ALTER TABLE sales ADD COLUMN email VARCHAR(100) COMMENT '销售邮箱';

-- ==================== product_packages 表：发货模板 ====================
ALTER TABLE product_packages ADD COLUMN delivery_template TEXT COMMENT '发货邮件模板';

-- 设置默认模板（参考CC-Installer发货助手）
UPDATE product_packages SET delivery_template =
'【{product_name} - {package_name}】

感谢您的购买！

下载链接：{download_url}

授权码：{auth_code}
🔐 授权码有效期 {hours} 小时（至 {expires_at}），请尽快激活使用

订单号：{order_no}
金额：¥{amount}

如有问题请联系客服。'
WHERE delivery_template IS NULL;

-- ==================== 系统配置：QQ邮箱SMTP ====================
INSERT INTO system_config (config_key, config_value, description) VALUES
('mail_host', 'smtp.qq.com', 'SMTP服务器地址'),
('mail_port', '587', 'SMTP端口'),
('mail_username', '', '发件邮箱地址'),
('mail_password', '', 'QQ邮箱授权码(非QQ密码)'),
('mail_from_name', 'CC-Installer', '发件人显示名称')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);

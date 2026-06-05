-- 添加索引以优化查询性能

-- 订单表索引
ALTER TABLE orders ADD INDEX idx_customer_phone (customer_phone);
ALTER TABLE orders ADD INDEX idx_created_at (created_at);
ALTER TABLE orders ADD INDEX idx_paid_at (paid_at);
ALTER TABLE orders ADD INDEX idx_payment_method (payment_method);
ALTER TABLE orders ADD INDEX idx_product_id (product_id);
ALTER TABLE orders ADD INDEX idx_auth_status (auth_status);

-- 分润记录表索引
ALTER TABLE commissions ADD INDEX idx_created_at (created_at);
ALTER TABLE commissions ADD INDEX idx_order_id (order_id);
ALTER TABLE commissions ADD INDEX idx_sales_id_status (sales_id, status);

-- 下载记录表索引
ALTER TABLE downloads ADD INDEX idx_order_id (order_id);
ALTER TABLE downloads ADD INDEX idx_expire_at (expire_at);

-- 销售表索引
ALTER TABLE sales ADD INDEX idx_status (status);
ALTER TABLE sales ADD INDEX idx_created_at (created_at);

-- 管理员表索引
ALTER TABLE admin ADD INDEX idx_username (username);

-- 系统配置表索引
ALTER TABLE system_config ADD INDEX idx_config_key (config_key);

-- 产品表索引（如果存在）
-- ALTER TABLE products ADD INDEX idx_status (status);
-- ALTER TABLE products ADD INDEX idx_slug (slug);

-- 认证日志表索引（如果存在）
-- ALTER TABLE auth_logs ADD INDEX idx_order_id (order_id);
-- ALTER TABLE auth_logs ADD INDEX idx_auth_code (auth_code);
-- ALTER TABLE auth_logs ADD INDEX idx_created_at (created_at);

-- 客户价格表索引（如果存在）
-- ALTER TABLE customer_prices ADD INDEX idx_phone (phone);
-- ALTER TABLE customer_prices ADD INDEX idx_phone_sales_id (phone, sales_id);

-- 站点设置表索引（如果存在）
-- ALTER TABLE site_settings ADD INDEX idx_setting_key (setting_key);

-- 验证码表索引（如果存在）
-- ALTER TABLE verification_codes ADD INDEX idx_phone (phone);
-- ALTER TABLE verification_codes ADD INDEX idx_code (code);
-- ALTER TABLE verification_codes ADD INDEX idx_expire_at (expire_at);

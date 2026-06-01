-- 销售分润系统数据库

CREATE DATABASE IF NOT EXISTS sales_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sales_system;

-- 销售表
CREATE TABLE sales (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL COMMENT '销售名称',
  phone VARCHAR(20) UNIQUE NOT NULL COMMENT '手机号',
  password VARCHAR(200) NOT NULL COMMENT '密码(BCrypt)',
  code VARCHAR(20) UNIQUE NOT NULL COMMENT '推广码',
  commission_rate DECIMAL(5,2) DEFAULT 10.00 COMMENT '分润比例(%)',
  status VARCHAR(20) DEFAULT 'active' COMMENT 'active/disabled',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '销售表';

-- 订单表
CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(64) UNIQUE NOT NULL COMMENT '订单号',
  amount DECIMAL(10,2) NOT NULL COMMENT '实际支付金额(销售定价)',
  base_amount DECIMAL(10,2) NOT NULL COMMENT '基础价格快照',
  sales_id BIGINT NULL COMMENT '绑定的销售ID',
  status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/paid/bound/settled',
  payment_method VARCHAR(20) COMMENT 'wechat/alipay',
  payment_no VARCHAR(128) COMMENT '支付流水号',
  customer_phone VARCHAR(20) COMMENT '客户手机号',
  claimed_by BIGINT NULL COMMENT '手动认领的销售ID',
  claimed_at TIMESTAMP NULL COMMENT '认领时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  paid_at TIMESTAMP NULL COMMENT '支付时间',
  INDEX idx_sales_id (sales_id),
  INDEX idx_status (status),
  INDEX idx_order_no (order_no)
) COMMENT '订单表';

-- 分润记录表
CREATE TABLE commissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL COMMENT '订单ID',
  sales_id BIGINT NOT NULL COMMENT '销售ID',
  amount DECIMAL(10,2) NOT NULL COMMENT '销售分润金额',
  admin_amount DECIMAL(10,2) NOT NULL COMMENT '管理员抽成金额',
  rate DECIMAL(5,2) NOT NULL COMMENT '分润比例(%)',
  status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/settled',
  settled_at TIMESTAMP NULL COMMENT '结算时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_sales_id (sales_id),
  INDEX idx_status (status)
) COMMENT '分润记录表';

-- 系统配置表
CREATE TABLE system_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(50) UNIQUE NOT NULL COMMENT '配置键',
  config_value VARCHAR(200) NOT NULL COMMENT '配置值',
  description VARCHAR(200) COMMENT '描述',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '系统配置表';

-- 初始配置
INSERT INTO system_config (config_key, config_value, description) VALUES
('base_price', '99', '基础价格'),
('min_price', '80', '销售最低定价'),
('max_price', '150', '销售最高定价'),
('default_commission_rate', '10', '默认分润比例(%)'),
('admin_name', '管理员', '管理员名称');

-- 管理员表
CREATE TABLE admin (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
  password VARCHAR(200) NOT NULL COMMENT '密码(BCrypt)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) COMMENT '管理员表';

-- 初始管理员 (密码: admin123)
INSERT INTO admin (username, password) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH');

-- 下载记录表
CREATE TABLE downloads (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL COMMENT '订单ID',
  download_token VARCHAR(128) UNIQUE NOT NULL COMMENT '下载token',
  download_count INT DEFAULT 0 COMMENT '下载次数',
  expire_at TIMESTAMP NOT NULL COMMENT '过期时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_token (download_token)
) COMMENT '下载记录表';

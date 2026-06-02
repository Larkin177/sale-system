-- 销售分润系统 v2 数据库
-- 新增：products 表 + orders 加授权码字段

-- ==================== 产品表 ====================
CREATE TABLE products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL COMMENT '产品名称',
  slug VARCHAR(50) UNIQUE NOT NULL COMMENT '产品标识(英文)',
  description TEXT COMMENT '产品描述',
  version VARCHAR(20) COMMENT '当前版本号',
  download_url VARCHAR(500) COMMENT '下载链接',
  base_price DECIMAL(10,2) NOT NULL COMMENT '基础价格',
  min_price DECIMAL(10,2) COMMENT '销售最低定价',
  max_price DECIMAL(10,2) COMMENT '销售最高定价',
  auth_enabled BOOLEAN DEFAULT false COMMENT '是否启用云端授权验证',
  auth_validity_hours INT DEFAULT 24 COMMENT '授权有效小时数',
  default_commission_rate DECIMAL(5,2) DEFAULT 10.00 COMMENT '该产品默认分润比例',
  status VARCHAR(20) DEFAULT 'active' COMMENT 'active/disabled',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '产品表';

-- 初始产品：CC-Installer
INSERT INTO products (name, slug, description, version, base_price, min_price, max_price, auth_enabled, auth_validity_hours) VALUES
('CC-Installer 一键安装工具', 'cc-installer', 'AI编程工具一键安装器，支持 Claude Code + Codex CLI', '1.0.0', 99.00, 80.00, 150.00, true, 72);

-- ==================== 订单表（v2 修改）====================
-- 添加字段
ALTER TABLE orders ADD COLUMN product_id BIGINT COMMENT '关联产品ID';
ALTER TABLE orders ADD COLUMN product_name VARCHAR(100) COMMENT '产品名称快照';
ALTER TABLE orders ADD COLUMN auth_code VARCHAR(512) COMMENT '授权码';
ALTER TABLE orders ADD COLUMN auth_status VARCHAR(20) DEFAULT 'active' COMMENT 'active/revoked/expired/used';
ALTER TABLE orders ADD COLUMN auth_used_at TIMESTAMP NULL COMMENT '首次验证时间';
ALTER TABLE orders ADD COLUMN auth_machine VARCHAR(255) COMMENT '首次绑定的机器指纹';
ALTER TABLE orders ADD COLUMN auth_uses INT DEFAULT 0 COMMENT '验证次数';
ALTER TABLE orders ADD COLUMN auth_last_validated_at TIMESTAMP NULL COMMENT '最后验证时间';

-- 添加索引
ALTER TABLE orders ADD INDEX idx_product_id (product_id);
ALTER TABLE orders ADD INDEX idx_auth_code (auth_code);
ALTER TABLE orders ADD INDEX idx_auth_status (auth_status);

-- ==================== 授权码操作日志 ====================
CREATE TABLE auth_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL COMMENT '订单ID',
  auth_code VARCHAR(512) NOT NULL COMMENT '授权码',
  action VARCHAR(50) NOT NULL COMMENT 'validate/revoke/use',
  machine VARCHAR(255) COMMENT '机器指纹',
  ip_address VARCHAR(50) COMMENT 'IP地址',
  result VARCHAR(20) NOT NULL COMMENT 'success/failed',
  reason VARCHAR(200) COMMENT '失败原因',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_order_id (order_id),
  INDEX idx_auth_code (auth_code)
) COMMENT '授权码操作日志';

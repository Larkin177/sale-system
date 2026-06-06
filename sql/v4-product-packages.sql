-- v4: 产品套餐管理系统

-- ==================== 产品套餐表 ====================
CREATE TABLE IF NOT EXISTS product_packages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL COMMENT '关联产品ID',
  name VARCHAR(100) NOT NULL COMMENT '套餐名称(如: Claude Code)',
  platform VARCHAR(20) NOT NULL COMMENT '平台: mac/windows/all',
  version VARCHAR(50) COMMENT '版本号',
  description TEXT COMMENT '套餐描述',
  price DECIMAL(10,2) NOT NULL COMMENT '售价',
  download_url VARCHAR(500) COMMENT '下载链接',
  auth_enabled BOOLEAN DEFAULT true COMMENT '是否启用授权码',
  auth_validity_hours INT DEFAULT 72 COMMENT '授权有效小时数',
  sort_order INT DEFAULT 0 COMMENT '排序(越小越前)',
  status VARCHAR(20) DEFAULT 'active' COMMENT 'active/disabled',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_product_id (product_id),
  INDEX idx_platform (platform)
) COMMENT '产品套餐表';

-- ==================== 订单表新增字段 ====================
-- 先检查字段是否存在，不存在才添加
SET @dbname = 'sales_system';

SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'orders' AND column_name = 'package_id';
SET @sql = IF(@col_exists = 0, 'ALTER TABLE orders ADD COLUMN package_id BIGINT COMMENT \'关联套餐ID\'', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'orders' AND column_name = 'package_name';
SET @sql = IF(@col_exists = 0, 'ALTER TABLE orders ADD COLUMN package_name VARCHAR(100) COMMENT \'套餐名称快照\'', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'orders' AND column_name = 'platform';
SET @sql = IF(@col_exists = 0, 'ALTER TABLE orders ADD COLUMN platform VARCHAR(20) COMMENT \'平台快照\'', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 系统配置：产品页 ====================
INSERT INTO system_config (config_key, config_value, description) VALUES
('product_page_title', '选择您的产品', '产品页标题'),
('product_page_subtitle', '请选择平台和套餐', '产品页副标题'),
('product_page_tips', '购买后将获得下载链接和授权码，请妥善保管', '产品页提示语')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);

-- ==================== 初始套餐数据 ====================
-- CC-Installer Mac 版
INSERT INTO product_packages (product_id, name, platform, version, description, price, download_url, auth_enabled, auth_validity_hours, sort_order) VALUES
(1, 'Claude Code', 'mac', '1.0.0', 'Claude Code AI编程助手，支持Mac系统', 99.00, '', true, 72, 1),
(1, 'Codex CLI', 'mac', '1.0.0', 'Codex CLI命令行工具，支持Mac系统', 99.00, '', true, 72, 2),
(1, 'Claude Code + Codex', 'mac', '1.0.0', 'Claude Code + Codex 组合包，Mac系统完整体验', 149.00, '', true, 72, 3);

-- CC-Installer Windows 版
INSERT INTO product_packages (product_id, name, platform, version, description, price, download_url, auth_enabled, auth_validity_hours, sort_order) VALUES
(1, 'Claude Code', 'windows', '1.0.0', 'Claude Code AI编程助手，支持Windows系统', 99.00, '', true, 72, 1),
(1, 'Codex CLI', 'windows', '1.0.0', 'Codex CLI命令行工具，支持Windows系统', 99.00, '', true, 72, 2),
(1, 'Claude Code + Codex', 'windows', '1.0.0', 'Claude Code + Codex 组合包，Windows系统完整体验', 149.00, '', true, 72, 3);

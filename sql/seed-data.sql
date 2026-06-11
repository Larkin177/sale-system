-- ================================================================
-- 销售分润系统 - 种子数据
-- 说明：清空旧订单/套餐数据，插入干净的演示数据
-- ================================================================

USE sales_system;

-- ==================== 1. 清空订单相关数据 ====================
TRUNCATE TABLE commissions;
TRUNCATE TABLE orders;

-- ==================== 2. 重置套餐（清空重复数据，重新插入） ====================
TRUNCATE TABLE product_packages;

-- Windows 套餐
INSERT INTO product_packages (product_id, name, platform, version, description, price, auth_enabled, auth_validity_hours, sort_order, status, min_price, max_price, created_at, updated_at)
VALUES
(1, 'ClaudeCode版', 'windows', '1.0.0', 'Claude Code AI编程助手，支持Windows系统', 15.00, 1, 24, 1, 'active', 8.00, 15.00, NOW(), NOW()),
(1, 'Codex版', 'windows', '1.0.0', 'Codex CLI命令行工具，支持Windows系统', 15.00, 1, 24, 2, 'active', 8.00, 15.00, NOW(), NOW()),
(1, 'Pro(ClaudeCode+Codex)', 'windows', '1.0.0', 'Claude Code + Codex 组合包，Windows系统完整体验', 25.00, 1, 24, 3, 'active', 20.00, 25.00, NOW(), NOW());

-- Mac 套餐
INSERT INTO product_packages (product_id, name, platform, version, description, price, auth_enabled, auth_validity_hours, sort_order, status, min_price, max_price, created_at, updated_at)
VALUES
(1, 'ClaudeCode版', 'mac', '1.0.0', 'Claude Code AI编程助手，支持Mac系统', 15.00, 1, 24, 4, 'active', 8.00, 15.00, NOW(), NOW()),
(1, 'Codex版本', 'mac', '1.0.0', 'Codex CLI命令行工具，支持Mac系统', 15.00, 1, 24, 5, 'active', 8.00, 15.00, NOW(), NOW()),
(1, 'Pro(ClaudeCode+Codex)', 'mac', '1.0.0', 'Claude Code + Codex 组合包，Mac系统完整体验', 25.00, 1, 24, 6, 'active', 20.00, 25.00, NOW(), NOW());

-- ==================== 3. 删除旧销售重新插入 ====================
DELETE FROM sales WHERE id = 1;

INSERT INTO sales (name, phone, password, code, commission_rate, status, email, created_at, updated_at)
VALUES ('周媛', '15375938067', '$2a$10$9GLz.fT4nCFlnoANh.bmSedVBnpfAbWmtjdiwl1DerNWnJ88etGWy', 'ZZYY', 40.00, 'active', '1060210178@qq.com', NOW(), NOW());

-- ==================== 4. 更新系统配置 ====================
-- 默认分润比例改为 40%
UPDATE system_config SET config_value = '40' WHERE config_key = 'default_commission_rate';

-- ==================== 5. 更新功能特性图标 ====================
-- 图标列表：https://element-plus.org/zh-CN/component/icon.html
UPDATE site_settings SET setting_value = 'MagicStick' WHERE setting_key = 'feature_1_icon';   -- 零门槛
UPDATE site_settings SET setting_value = 'Mouse'     WHERE setting_key = 'feature_2_icon';   -- 傻瓜式操作
UPDATE site_settings SET setting_value = 'Connection' WHERE setting_key = 'feature_3_icon';  -- DeepSeek友好

-- ==================== 6. 清理已删除的旧套餐遗留 ====================
DELETE FROM site_settings WHERE setting_key = 'product_page_tips';

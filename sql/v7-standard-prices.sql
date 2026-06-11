-- v7: 标准定价调整
-- 6个商品：mac/windows × (Claude Code / Codex CLI / 组合包)
-- 授权码有效期改为24小时

USE sales_system;

-- ==================== 1. 更新产品表 ====================
UPDATE products SET
  base_price = 15.00,
  min_price  = 10.00,
  max_price  = 20.00,
  auth_validity_hours = 24
WHERE slug = 'cc-installer';

-- ==================== 2. 更新套餐表（6个商品）====================
-- Mac 版
UPDATE product_packages SET
  price = 15.00,
  auth_validity_hours = 24
WHERE product_id = 1 AND platform = 'mac' AND name = 'Claude Code';

UPDATE product_packages SET
  price = 15.00,
  auth_validity_hours = 24
WHERE product_id = 1 AND platform = 'mac' AND name = 'Codex CLI';

UPDATE product_packages SET
  price = 25.00,
  auth_validity_hours = 24
WHERE product_id = 1 AND platform = 'mac' AND name = 'Claude Code + Codex';

-- Windows 版
UPDATE product_packages SET
  price = 15.00,
  auth_validity_hours = 24
WHERE product_id = 1 AND platform = 'windows' AND name = 'Claude Code';

UPDATE product_packages SET
  price = 15.00,
  auth_validity_hours = 24
WHERE product_id = 1 AND platform = 'windows' AND name = 'Codex CLI';

UPDATE product_packages SET
  price = 25.00,
  auth_validity_hours = 24
WHERE product_id = 1 AND platform = 'windows' AND name = 'Claude Code + Codex';

-- ==================== 3. 更新系统配置 ====================
UPDATE system_config SET config_value = '15'  WHERE config_key = 'base_price';
UPDATE system_config SET config_value = '10'  WHERE config_key = 'min_price';
UPDATE system_config SET config_value = '20'  WHERE config_key = 'max_price';

-- ==================== 4. 验证结果 ====================
SELECT '=== 产品表 ===' AS '';
SELECT id, name, base_price, min_price, max_price, auth_validity_hours FROM products;

SELECT '=== 套餐列表 ===' AS '';
SELECT id, name, platform, price, auth_validity_hours
FROM product_packages ORDER BY platform, sort_order;

SELECT '=== 系统配置 ===' AS '';
SELECT config_key, config_value, description FROM system_config WHERE config_key LIKE '%price%';

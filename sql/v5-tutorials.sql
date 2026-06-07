-- v5: 教程内容管理 + 支付二维码迁移

-- ==================== 教程表 ====================
CREATE TABLE IF NOT EXISTS tutorials (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL COMMENT '教程标题',
  type VARCHAR(20) NOT NULL COMMENT '类型: text/video/image',
  content TEXT COMMENT '富文本内容(type=text)',
  media_url VARCHAR(500) COMMENT '视频/图片URL(type=video/image)',
  thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
  category VARCHAR(50) NOT NULL COMMENT '分类: claude-code/codex/installer',
  sort_order INT DEFAULT 0 COMMENT '排序',
  status VARCHAR(20) DEFAULT 'active' COMMENT 'active/disabled',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_category (category),
  INDEX idx_status (status)
) COMMENT '教程表';

-- ==================== 支付二维码迁移到 system_config ====================
-- 将 site_settings 中的二维码迁移过来
INSERT INTO system_config (config_key, config_value, description)
SELECT 'wechat_qrcode', setting_value, '微信收款二维码'
FROM site_settings WHERE setting_key = 'wechat_qrcode'
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO system_config (config_key, config_value, description)
SELECT 'alipay_qrcode', setting_value, '支付宝收款二维码'
FROM site_settings WHERE setting_key = 'alipay_qrcode'
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- 删除 site_settings 中的旧二维码记录（可选，保留也不影响）
DELETE FROM site_settings WHERE setting_key IN ('wechat_qrcode', 'alipay_qrcode');

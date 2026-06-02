CREATE TABLE IF NOT EXISTS `site_settings` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `setting_key` VARCHAR(100) NOT NULL COMMENT '设置键',
    `setting_value` TEXT COMMENT '设置值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '设置描述',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_setting_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点设置';

-- 默认站点设置
INSERT INTO `site_settings` (`setting_key`, `setting_value`, `description`) VALUES
('site_name', 'CC-Installer', '站点名称'),
('site_subtitle', 'Professional AI Programming Tools', '站点副标题'),
('hero_title', 'Professional Software Tools', '首页Banner标题'),
('hero_subtitle', 'One-click installation, ready to use out of the box', '首页Banner副标题'),
('hero_image', '', '首页Banner背景图片URL'),
('hero_bg_color', '#667eea', '首页Banner渐变起始色'),
('hero_bg_color_end', '#764ba2', '首页Banner渐变结束色'),
('feature_1_icon', 'trophy', '特性1图标'),
('feature_1_title', 'Quality Assurance', '特性1标题'),
('feature_1_desc', 'Carefully tested to ensure stable operation', '特性1描述'),
('feature_2_icon', 'star', '特性2图标'),
('feature_2_title', 'Continuous Updates', '特性2标题'),
('feature_2_desc', 'Regular feature updates and optimizations', '特性2描述'),
('feature_3_icon', 'service', '特性3图标'),
('feature_3_title', 'Technical Support', '特性3标题'),
('feature_3_desc', 'Professional team provides technical support', '特性3描述'),
('footer_text', '© 2026 CC-Installer. All rights reserved.', '页脚文本'),
('pay_title', 'Purchase Software', '购买页标题'),
('pay_subtitle', 'Complete payment to receive download link', '购买页副标题'),
('download_title', 'Download Software', '下载页标题'),
('download_subtitle', 'Thank you for your purchase', '下载页副标题')
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`);

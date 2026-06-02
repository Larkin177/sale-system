-- Feature card: custom icon, content type, and content value
INSERT INTO `site_settings` (`setting_key`, `setting_value`, `description`) VALUES
('feature_1_icon_url', '', '特性1自定义图标URL'),
('feature_1_content_type', 'none', '特性1点击内容类型(none/image/video/text/download)'),
('feature_1_content_value', '', '特性1点击内容值'),
('feature_2_icon_url', '', '特性2自定义图标URL'),
('feature_2_content_type', 'none', '特性2点击内容类型(none/image/video/text/download)'),
('feature_2_content_value', '', '特性2点击内容值'),
('feature_3_icon_url', '', '特性3自定义图标URL'),
('feature_3_content_type', 'none', '特性3点击内容类型(none/image/video/text/download)'),
('feature_3_content_value', '', '特性3点击内容值')
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`);

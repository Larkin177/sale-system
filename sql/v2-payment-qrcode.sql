INSERT INTO site_settings (setting_key, setting_value, description) VALUES
('wechat_qrcode', '', '微信收款二维码'),
('alipay_qrcode', '', '支付宝收款二维码')
ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value);

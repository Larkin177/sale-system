-- v6: email + package files + localStorage auto-verify
ALTER TABLE orders ADD COLUMN customer_email VARCHAR(100) COMMENT 'customer email';
ALTER TABLE orders ADD INDEX idx_customer_email (customer_email);
ALTER TABLE sales ADD COLUMN email VARCHAR(100) COMMENT 'sales email';
ALTER TABLE product_packages ADD COLUMN file_url VARCHAR(500) COMMENT 'package file URL';
ALTER TABLE product_packages ADD COLUMN email_template TEXT COMMENT 'email template';
UPDATE product_packages SET email_template='<p>Your auth code is ready! Visit <a href="{site_url}">{site_url}</a> to get it.</p><p>Order: {order_no}</p>' WHERE email_template IS NULL;
INSERT INTO system_config (config_key, config_value, description) VALUES ('mail_host','smtp.qq.com','SMTP server'),('mail_port','587','SMTP port'),('mail_username','','sender email'),('mail_password','','QQ mail auth code'),('mail_from_name','CC-Installer','sender name'),('site_url','http://localhost:3000','site URL') ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);

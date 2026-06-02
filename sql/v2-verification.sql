-- 验证码表（手机号验证）
CREATE TABLE IF NOT EXISTS verification_codes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  phone VARCHAR(20) NOT NULL COMMENT '手机号',
  code VARCHAR(10) NOT NULL COMMENT '验证码',
  purpose VARCHAR(50) DEFAULT 'phone_verify' COMMENT '用途',
  used TINYINT(1) DEFAULT 0 COMMENT '是否已使用',
  expires_at DATETIME NOT NULL COMMENT '过期时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_phone (phone),
  INDEX idx_phone_purpose (phone, purpose)
) COMMENT '验证码表';

-- v9: Settlement system
-- Payment codes for sales
CREATE TABLE IF NOT EXISTS sales_payment_codes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sales_id BIGINT NOT NULL COMMENT '销售ID',
  code_type VARCHAR(20) NOT NULL COMMENT 'wechat/alipay',
  code_url VARCHAR(500) NOT NULL COMMENT '收款码图片URL',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_sales_id (sales_id)
) DEFAULT CHARSET=utf8mb4 COMMENT='销售收款码';

-- Settlement records
CREATE TABLE IF NOT EXISTS settlement_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sales_id BIGINT NOT NULL COMMENT '销售ID',
  amount DECIMAL(10,2) NOT NULL COMMENT '结算金额',
  status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/completed',
  proof_url VARCHAR(500) COMMENT '转账凭证截图URL',
  admin_note VARCHAR(500) COMMENT '管理员备注',
  settled_by BIGINT COMMENT '管理员ID',
  settled_at TIMESTAMP NULL COMMENT '结算时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_sales_id (sales_id),
  INDEX idx_status (status)
) DEFAULT CHARSET=utf8mb4 COMMENT='结算记录';

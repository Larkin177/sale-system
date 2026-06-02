CREATE TABLE IF NOT EXISTS customer_prices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  phone VARCHAR(20) NOT NULL COMMENT '客户手机号',
  price DECIMAL(10,2) NOT NULL COMMENT '成交价格',
  sales_id BIGINT NULL COMMENT '关联销售ID',
  order_no VARCHAR(64) NULL COMMENT '关联订单号',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_phone (phone),
  UNIQUE KEY uk_phone_sales (phone, sales_id)
) COMMENT '客户价格记忆表';

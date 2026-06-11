-- v8: tutorial categories
CREATE TABLE IF NOT EXISTS tutorial_categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL COMMENT 'category name',
  slug VARCHAR(50) UNIQUE NOT NULL COMMENT 'category slug',
  sort_order INT DEFAULT 0 COMMENT 'sort order',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COMMENT='tutorial categories';

-- initial categories
INSERT INTO tutorial_categories (name, slug, sort_order) VALUES
('安装教程', 'installer', 1),
('Claude Code', 'claude-code', 2),
('Codex', 'codex', 3)
ON DUPLICATE KEY UPDATE name=VALUES(name);

# 测试指南

## 📋 测试前准备

### 1. 环境检查

```bash
# 检查Java版本
java -version

# 检查Node.js版本
node -v

# 检查MySQL是否运行
mysql -u root -p

# 检查端口占用
netstat -ano | findstr :8080
netstat -ano | findstr :3000
```

### 2. 数据库准备

```bash
# 连接MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE IF NOT EXISTS sales_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 执行基础表结构
SOURCE E:/DevEnv/project/sale-system/sql/schema.sql;

# 执行v2表结构
SOURCE E:/DevEnv/project/sale-system/sql/v2-schema.sql;

# 执行其他v2脚本
SOURCE E:/DevEnv/project/sale-system/sql/v2-site-settings.sql;
SOURCE E:/DevEnv/project/sale-system/sql/v2-feature-cards.sql;
SOURCE E:/DevEnv/project/sale-system/sql/v2-verification.sql;
SOURCE E:/DevEnv/project/sale-system/sql/v2-customer-prices.sql;
SOURCE E:/DevEnv/project/sale-system/sql/v2-payment-qrcode.sql;
SOURCE E:/DevEnv/project/sale-system/sql/v2-delivery-message.sql;
SOURCE E:/DevEnv/project/sale-system/sql/v2-delivery-auth-code.sql;

# 执行索引优化
SOURCE E:/DevEnv/project/sale-system/sql/add-indexes.sql;
```

### 3. 启动后端服务

```bash
# 进入后端目录
cd E:/DevEnv/project/sale-system/backend

# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run

# 或者打包后运行
mvn clean package -DskipTests
java -jar target/sales-system-1.0.0.jar
```

### 4. 启动前端服务

```bash
# 进入前端目录
cd E:/DevEnv/project/sale-system/frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

---

## 🧪 功能测试

### 1. 支付回调测试

#### 测试场景：模拟微信支付回调

```bash
# 使用curl模拟微信支付回调
curl -X POST http://localhost:8080/api/webhook/wechat \
  -H "Content-Type: application/json" \
  -d '{
    "out_trade_no": "ORD20260604120000001",
    "transaction_id": "WX20260604120000001",
    "total_fee": 9900,
    "result_code": "SUCCESS"
  }'
```

**预期结果：**
- 返回 `<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>`
- 订单状态从 `pending` 变为 `paid`
- 自动生成授权码
- 分润记录创建成功

#### 测试场景：模拟支付宝回调

```bash
# 使用curl模拟支付宝回调
curl -X POST "http://localhost:8080/api/alipay/notify?out_trade_no=ORD20260604120000002&trade_no=ALI20260604120000002&total_amount=99.00&trade_status=TRADE_SUCCESS" \
  -G
```

**预期结果：**
- 返回 `success`
- 订单状态从 `pending` 变为 `paid`
- 自动生成授权码

#### 测试场景：重复支付防护

```bash
# 再次发送相同的支付回调
curl -X POST http://localhost:8080/api/webhook/wechat \
  -H "Content-Type: application/json" \
  -d '{
    "out_trade_no": "ORD20260604120000001",
    "transaction_id": "WX20260604120000001",
    "total_fee": 9900,
    "result_code": "SUCCESS"
  }'
```

**预期结果：**
- 返回 SUCCESS（不重复处理）
- 日志显示 "订单状态不是pending，跳过处理"

### 2. 分润计算测试

#### 测试场景：验证分润统计

```bash
# 访问管理端统计接口
curl -X GET http://localhost:8080/api/admin/stats \
  -H "Authorization: Bearer <admin_token>"
```

**预期结果：**
- `totalCommission` 从 commissions 表查询实际金额
- `monthCommission` 显示本月分润金额
- 不再使用固定的 10% 计算

#### 测试场景：验证销售业绩统计

```bash
# 访问销售端业绩接口
curl -X GET http://localhost:8080/api/orders/stats \
  -H "Authorization: Bearer <sales_token>"
```

**预期结果：**
- 使用 SQL 聚合查询，性能更好
- 返回总成交单数、总成交额、本月数据

### 3. 下载链接安全测试

#### 测试场景：下载次数限制

```bash
# 第1次下载
curl -X GET "http://localhost:8080/api/download/info?token=dl-abc123def456"

# 继续下载直到达到限制（最多10次）
for i in {1..10}; do
  curl -X GET "http://localhost:8080/api/download/file?token=dl-abc123def456"
done

# 第11次下载
curl -X GET "http://localhost:8080/api/download/info?token=dl-abc123def456"
```

**预期结果：**
- 前10次返回成功
- 第11次返回错误："下载次数已达上限，请联系客服"

#### 测试场景：Token格式验证

```bash
# 无效token格式
curl -X GET "http://localhost:8080/api/download/info?token=invalid_token"

# 不存在的token
curl -X GET "http://localhost:8080/api/download/info?token=dl-nonexistent"
```

**预期结果：**
- 无效格式返回："下载链接无效"
- 不存在的token返回："下载链接无效"

### 4. 性能优化测试

#### 测试场景：N+1查询优化

```bash
# 测试客户最近订单查询
curl -X GET "http://localhost:8080/api/customer/last-order?phone=13800138000"
```

**预期结果：**
- 使用 JOIN 查询，只执行1条SQL
- 日志显示查询时间明显减少

#### 测试场景：销售业绩统计优化

```bash
# 测试销售业绩统计
curl -X GET "http://localhost:8080/api/orders/stats" \
  -H "Authorization: Bearer <sales_token>"
```

**预期结果：**
- 使用 SQL 聚合查询
- 不再查询所有订单到内存中处理

### 5. 销售管理功能测试

#### 测试场景：查看销售详情

```bash
# 访问销售详情页面
# 浏览器打开：http://localhost:3000/admin/sales/1
```

**预期结果：**
- 显示销售基本信息（名称、手机号、推广码、分润比例）
- 显示销售业绩统计（总成交单数、总成交额、本月数据）
- 显示销售订单列表（分页）

#### 测试场景：销售列表操作

```bash
# 访问销售列表页面
# 浏览器打开：http://localhost:3000/admin/sales
```

**预期结果：**
- 每行显示"详情"、"编辑"、"禁用/启用"按钮
- 点击"详情"跳转到销售详情页面

### 6. 批量操作测试

#### 测试场景：批量发货

```bash
# 1. 访问订单管理页面
# 浏览器打开：http://localhost:3000/admin/orders

# 2. 选择多个待支付订单（勾选复选框）

# 3. 点击"批量发货"按钮

# 4. 确认操作
```

**预期结果：**
- 显示已选择的订单数量
- 确认后批量处理选中的订单
- 订单状态更新为"已支付"
- 显示操作成功提示

#### 测试场景：批量结算

```bash
# 1. 选择多个已支付/已发货订单

# 2. 点击"批量结算"按钮

# 3. 确认操作
```

**预期结果：**
- 批量结算选中的订单
- 生成分润记录
- 显示操作成功提示

### 7. 用户体验测试

#### 测试场景：订单状态显示

```bash
# 访问销售端仪表板
# 浏览器打开：http://localhost:3000/s/dashboard
```

**预期结果：**
- 订单状态显示为中文（待支付、已支付、已发货、已核销、已绑定、已结算）
- 不同状态显示不同颜色的标签

#### 测试场景：管理端订单管理

```bash
# 访问管理端订单管理
# 浏览器打开：http://localhost:3000/admin/orders
```

**预期结果：**
- 订单状态显示完整的中文状态
- 支持批量选择和操作

### 8. 日志监控测试

#### 测试场景：查看应用日志

```bash
# 查看日志文件
tail -f E:/DevEnv/project/sale-system/backend/logs/sales-system.log

# 或者在控制台查看实时日志
```

**预期结果：**
- 日志格式：`yyyy-MM-dd HH:mm:ss [thread] LEVEL logger - message`
- 关键操作有详细日志记录
- 不同模块有不同的日志级别

---

## 🐛 常见问题排查

### 1. 支付回调失败

**问题现象：** 支付成功后订单状态未更新

**排查步骤：**
```bash
# 1. 检查回调URL配置
# 查看 application.yml 中的 notify-url 配置

# 2. 检查网络连通性
curl -X POST http://localhost:8080/api/webhook/wechat -d '{}'

# 3. 查看应用日志
tail -f logs/sales-system.log | grep "微信支付回调"
```

**解决方案：**
- 确保回调URL可访问
- 检查防火墙设置
- 验证签名配置

### 2. 分润计算不准确

**问题现象：** 统计数据中的分润金额与实际不符

**排查步骤：**
```bash
# 1. 检查commissions表数据
mysql -u root -p -e "SELECT * FROM sales_system.commissions;"

# 2. 验证分润计算逻辑
# 查看 CommissionService.calculateCommission 方法

# 3. 检查订单状态
mysql -u root -p -e "SELECT id, order_no, status, sales_id FROM sales_system.orders;"
```

**解决方案：**
- 确保支付回调正确触发分润计算
- 检查分润比例配置

### 3. 下载链接无法使用

**问题现象：** 下载链接返回错误

**排查步骤：**
```bash
# 1. 检查downloads表数据
mysql -u root -p -e "SELECT * FROM sales_system.downloads;"

# 2. 验证token格式
# token必须以 "dl-" 开头

# 3. 检查过期时间
mysql -u root -p -e "SELECT id, download_token, download_count, expire_at FROM sales_system.downloads;"
```

**解决方案：**
- 重新生成下载链接
- 检查产品配置

### 4. 性能问题

**问题现象：** 接口响应慢

**排查步骤：**
```bash
# 1. 查看SQL执行时间
# 在 application.yml 中配置 SQL 日志

# 2. 检查索引是否生效
mysql -u root -p -e "EXPLAIN SELECT * FROM orders WHERE customer_phone = '13800138000';"

# 3. 监控数据库连接
# 查看连接池使用情况
```

**解决方案：**
- 确保索引已创建
- 优化SQL查询
- 调整连接池配置

---

## 📊 测试数据准备

### 1. 创建测试订单

```sql
-- 创建测试订单
INSERT INTO orders (order_no, amount, base_amount, status, payment_method, customer_phone, created_at)
VALUES 
('ORD20260604120000001', 99.00, 99.00, 'pending', 'wechat', '13800138000', NOW()),
('ORD20260604120000002', 88.00, 99.00, 'pending', 'alipay', '13900139000', NOW()),
('ORD20260604120000003', 99.00, 99.00, 'paid', 'wechat', '13700137000', NOW() - INTERVAL 1 DAY);
```

### 2. 创建测试销售

```sql
-- 创建测试销售
INSERT INTO sales (name, phone, password, code, commission_rate, status)
VALUES 
('测试销售1', '13800138001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'TEST001', 10.00, 'active'),
('测试销售2', '13800138002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'TEST002', 15.00, 'active');
```

### 3. 创建测试下载记录

```sql
-- 创建测试下载记录
INSERT INTO downloads (order_id, download_token, download_count, expire_at)
VALUES 
(1, 'dl-test123456789012', 0, DATE_ADD(NOW(), INTERVAL 30 DAY)),
(2, 'dl-test123456789013', 5, DATE_ADD(NOW(), INTERVAL 30 DAY));
```

---

## ✅ 测试检查清单

### 基础功能测试
- [ ] 支付回调正常处理
- [ ] 订单状态正确更新
- [ ] 分润计算准确
- [ ] 授权码正常生成
- [ ] 下载链接安全有效

### 性能测试
- [ ] N+1查询已优化
- [ ] SQL聚合查询正常
- [ ] 索引生效

### 功能完善测试
- [ ] 销售详情页面正常
- [ ] 批量操作功能正常
- [ ] 用户体验优化生效

### 安全测试
- [ ] 下载次数限制有效
- [ ] Token格式验证正常
- [ ] 重复支付防护有效

### 日志监控测试
- [ ] 日志配置生效
- [ ] 关键操作有日志
- [ ] 日志格式正确

---

## 🚀 上线前检查

### 1. 配置检查
- [ ] 数据库连接配置正确
- [ ] 微信/支付宝参数配置正确
- [ ] JWT密钥安全
- [ ] 日志级别合适

### 2. 安全检查
- [ ] HTTPS已配置
- [ ] API限流已实现
- [ ] SQL注入防护
- [ ] XSS防护

### 3. 性能检查
- [ ] 数据库索引已创建
- [ ] 连接池配置合理
- [ ] 缓存已启用

### 4. 监控检查
- [ ] 应用监控已配置
- [ ] 数据库监控已配置
- [ ] 告警机制已建立

---

## 📞 技术支持

如果在测试过程中遇到问题，请：

1. 查看应用日志：`logs/sales-system.log`
2. 查看数据库数据
3. 检查网络连接
4. 参考本文档的常见问题排查部分

如需进一步帮助，请联系开发团队。

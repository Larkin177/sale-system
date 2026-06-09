# 销售分润系统 - 功能更新记录

> 更新日期：2026-06-09

---

## 目录

1. [首页重构（Phase 1）](#1-首页重构phase-1)
2. [授权码核销 API（Phase 2）](#2-授权码核销-apiphase-2)
3. [发货流程完善（Phase 3）](#3-发货流程完善phase-3)
4. [我的订单页面（Phase 4）](#4-我的订单页面phase-4)
5. [管理端邮件配置](#5-管理端邮件配置)
6. [完整测试流程](#6-完整测试流程)

---

## 1. 首页重构（Phase 1）

### 改动说明

重新设计了客户首页，将套餐选择从页面直接展示改为弹窗三步流程。

### 涉及的目录和文件

| 文件 | 操作 |
|---|---|
| `frontend/src/views/customer/Home.vue` | 重写 |

### 首页结构

```
┌─────────────────────────────┐
│  CC-Installer      [我的订单] │ ← 顶部导航栏（新增）
├─────────────────────────────┤
│     Hero 横幅 + 立即购买     │ ← 不变
├─────────────────────────────┤
│  [产品介绍] [使用教程]        │ ← 标签页导航（恢复）
├─────────────────────────────┤
│  功能特性卡片 / 教程列表      │ ← 无套餐卡片
└─────────────────────────────┘
```

### 购买弹窗流程

点击"立即购买"→ 弹窗三步：

| 步骤 | 内容 | 说明 |
|---|---|---|
| **Step 1** | 邮箱 + 图形验证码 | 调用 `/api/captcha/generate` 获取验证码 → 验证通过进入下一步 |
| **Step 2** | 选择平台 + 选择产品 | macOS/Windows 切换 → 显示对应平台的套餐卡片 |
| **Step 3** | 支付方式 + 二维码 | 微信/支付宝切换 → 调用 `/api/pay/create` 创建订单 → 显示二维码 |

### 前端组件改动

- **删除**：页面上原有的套餐定价展示区域
- **删除**：顶部功能特性卡片区域（已整合到"产品介绍"标签页内）
- **新增**：顶部导航栏（站点名 + "我的订单"入口）
- **新增**：多步弹窗购买流程（`el-dialog` 内三步切换）
- **恢复**：下半区"产品介绍" + "使用教程"标签页（与原版一致）
- **新增**：URL 参数 `?s=CODE` 支持销售推广码

---

## 2. 授权码核销 API（Phase 2）

### 改动说明

新增授权码核销系统，安装器可通过 API 查询和核销授权码，支持一机一码。

### 新增文件

| 文件 | 说明 |
|---|---|
| `backend/../entity/LicenseCode.java` | 授权码核销实体 |
| `backend/../mapper/LicenseCodeMapper.java` | MyBatis-Plus Mapper |
| `backend/../controller/AuthRedeemController.java` | 核销 API 控制器 |

### 数据库变更

```sql
CREATE TABLE license_codes (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  code          VARCHAR(2048) NOT NULL UNIQUE,  -- 完整授权码
  code_id       VARCHAR(100) NOT NULL,           -- payload 中的 ID
  order_id      BIGINT NOT NULL,                 -- 关联订单
  consumed      TINYINT DEFAULT 0,               -- 0=未使用 1=已核销
  consumed_at   DATETIME,                        -- 核销时间
  consumed_by   VARCHAR(255),                    -- 机器指纹
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

> `orders.auth_code` 列从 `VARCHAR(512)` 扩容为 `VARCHAR(2048)`（原列长度不够，RSA签名后的授权码约400-500字符）。

### API 接口

#### `POST /api/auth/check`

查询授权码是否已核销。

```json
// Request
{ "code": "AIC-xxxx.xxxx", "machine": "sha256指纹" }

// Response (可用)
{ "code": 200, "data": { "consumed": false } }

// Response (已核销)
{ "code": 200, "data": { "consumed": true } }
```

#### `POST /api/auth/consume`

核销授权码（**幂等**，重复调用静默返回成功）。

```json
// Request
{ "codeId": "xxx", "code": "AIC-xxxx.xxxx", "machine": "sha256指纹" }

// Response
{ "code": 200, "data": { "success": true } }
```

### 修改文件

| 文件 | 改动 |
|---|---|
| `backend/../service/AuthCodeService.java` | 新增 `parseCodeId()` 和 `saveLicenseCode()` |
| `backend/../controller/AdminOrderController.java` | autoDeliver 中调用 `saveLicenseCode` |
| `backend/../controller/WebhookController.java` | autoDeliver 中调用 `saveLicenseCode` |

---

## 3. 发货流程完善（Phase 3）

### 改动说明

完善自动发货流程：支付确认后自动生成授权码、存入 license_codes 表、发送邮件通知客户。

### 完整发货流程

```
管理员确认到账
     ↓
AdminOrderController.confirmPayment()
     ↓
1. 标记订单 paid + 记录支付信息
2. 计算分润 (CommissionService.calculateCommission)
3. 自动发货 (autoDeliver):
   a. 生成授权码 (AuthCodeService.generateAuthCode)
   b. 保存到 orders.auth_code
   c. 保存到 license_codes 表
   d. 发送邮件通知客户 (sendDeliveryEmail)
```

### 邮件模板

邮件内容使用管理端可配置的模板（`system_config.delivery_template`），支持以下变量：

```
{site_name}     站点名称
{site_url}      站点地址
{order_no}      订单号
{amount}        订单金额
{email}         客户邮箱
{download_url}  下载链接（套餐配置或订单查询链接）
{order_url}     订单查询链接
{auth_code}     授权码
{package_name}  套餐名称
{product_name}  产品名称
{hours}         授权有效期(小时)
```

默认模板（HTML）：
```html
<div>
  <h2>{product_name} - 订单已发货</h2>
  <p>感谢您的购买！</p>
  <p><strong>订单号：</strong>{order_no}</p>
  <p><strong>授权码：</strong><code>{auth_code}</code></p>
  <p>🔐 授权码有效期 {hours} 小时，一机一码</p>
  <a href="{download_url}">📥 下载安装器</a>
</div>
```

### 修改文件

| 文件 | 改动 |
|---|---|
| `backend/../controller/AdminOrderController.java` | autoDeliver 末尾调用 `sendDeliveryEmail()` |
| `backend/../controller/WebhookController.java` | autoDeliver 末尾调用 `sendDeliveryEmail()` |
| `backend/../controller/CustomerController.java` | 新增 `/api/customer/order?orderNo=` 订单号查询接口 |

---

## 4. 我的订单页面（Phase 4）

### 改动说明

重写客户订单查询页面，支持邮箱查询和订单号直达，完善授权码展示与操作。

### 页面路径

`/orders`（客户前端路由）

### 功能

| 功能 | 说明 |
|---|---|
| 邮箱查询 | 输入邮箱查询所有订单（自动填充已存邮箱） |
| 订单号直达 | 邮件链接携带 `?orderNo=xxx`，打开自动定位到该订单 |
| 状态标签 | 待支付/审核中/已付款/已发货/已核销，颜色区分 |
| 授权码展示 | 等宽字体显示完整授权码，带"一键复制"按钮 |
| 授权码状态 | 显示"未使用"/"已核销"标签，核销后显示核销时间 |
| 有效期提示 | 未核销时显示有效期和一机一码提示 |
| 下载按钮 | 已发货状态可下载 |

### 修改文件

| 文件 | 操作 |
|---|---|
| `frontend/src/views/customer/Orders.vue` | 重写 |

---

## 5. 管理端邮件配置

### 改动说明

在管理端"系统配置"页面增加邮件服务器和发货模板配置。

### 配置项

**邮件服务器**（`system_config` 表）：

| Key | 说明 |
|---|---|
| `mail_host` | SMTP 服务器地址 |
| `mail_port` | SMTP 端口 |
| `mail_username` | 发件邮箱 |
| `mail_password` | 邮箱密码/授权码 |
| `mail_from_name` | 发件人显示名称 |
| `site_url` | 站点地址（用于链接生成） |

**发货模板**：

| Key | 说明 |
|---|---|
| `delivery_template` | 发货邮件模板（支持HTML） |

### 页面预览

配置页面底部会实时渲染模板预览（使用示例变量替换）。

### 修改文件

| 文件 | 操作 |
|---|---|
| `frontend/src/views/admin/Config.vue` | 新增邮件服务器 + 发货模板卡片 |

---

## 6. 完整测试流程

### 前置条件

1. 后端运行在 `http://localhost:8080`
2. 前端运行在 `http://localhost:3000`
3. MySQL 运行在 `localhost:3308`，数据库 `sales_system`

### 测试步骤

#### 6.1 首页购买流程

```
1. 访问 http://localhost:3000
2. 点击 Hero 区域的"立即购买"按钮
   → 弹出 Step 1：验证邮箱
3. 输入邮箱，点击验证码图片加载验证码
   → 输入验证码，点击"下一步"
4. → 进入 Step 2：选择套餐
5. 切换 macOS / Windows 标签页
   → 显示对应平台的套餐（Claude Code / Codex CLI / 组合包）
6. 点击选择一个套餐（高亮），点击"下一步"
7. → 进入 Step 3：支付
8. 切换 微信/支付宝 查看二维码
9. 点击"我已支付"
   → 弹出"支付确认已提交"提示
```

#### 6.2 管理端确认收款

```
1. 访问 http://localhost:3000/admin/login
2. 用 admin/admin123 登录
3. 进入"订单管理"
4. 找到状态为"审核中"的订单
5. 点击"确认收款"
   → 订单状态变为"已发货"
   → 自动生成授权码
   → 自动发送邮件（控制台/日志可查看模拟邮件）
6. 检查数据库：
   SELECT * FROM license_codes;         -- 应有新记录
   SELECT auth_code, auth_status FROM orders;  -- auth_status=active
```

#### 6.3 授权码核销测试

```bash
# 1. 从数据库获取一个授权码
mysql -h127.0.0.1 -P3308 -uroot -p123456
USE sales_system;
SELECT code FROM license_codes WHERE consumed=0 LIMIT 1;

# 2. 查询授权码状态
curl -X POST http://localhost:8080/api/auth/check \
  -H "Content-Type: application/json" \
  -d '{"code":"AIC-xxx.xxx","machine":"test"}'
# 预期: {"data":{"consumed":false}}

# 3. 核销授权码
curl -X POST http://localhost:8080/api/auth/consume \
  -H "Content-Type: application/json" \
  -d '{"code":"AIC-xxx.xxx","machine":"test-machine"}'
# 预期: {"data":{"success":true}}

# 4. 再次查询
curl -X POST http://localhost:8080/api/auth/check \
  -H "Content-Type: application/json" \
  -d '{"code":"AIC-xxx.xxx","machine":"test"}'
# 预期: {"data":{"consumed":true}}

# 5. 幂等测试（重复核销）
curl -X POST http://localhost:8080/api/auth/consume \
  -H "Content-Type: application/json" \
  -d '{"code":"AIC-xxx.xxx","machine":"another-machine"}'
# 预期: 静默返回200，不会报错

# 6. 验证数据库
SELECT consumed, consumed_at, consumed_by FROM license_codes WHERE code='AIC-xxx.xxx';
SELECT auth_status, auth_used_at, auth_machine FROM orders WHERE id=<order_id>;
# 预期: consumed=1, auth_status='consumed'
```

#### 6.4 我的订单页面

```
1. 访问 http://localhost:3000/orders
2. 输入之前购买使用的邮箱，点击"查询"
   → 显示该邮箱的所有订单
3. 查看已发货订单：
   → 应显示授权码
   → 点击"复制"按钮复制授权码
   → 显示授权码状态标签
   → 显示有效期提示
4. 测试直接通过订单号访问：
   http://localhost:3000/orders?orderNo=ORDxxxx
```

#### 6.5 管理端邮件配置

```
1. 访问 http://localhost:3000/admin/config
2. 找到"邮件服务器"卡片
3. 配置 SMTP 信息（如 QQ邮箱）
4. 找到"发货邮件模板"卡片
5. 编辑模板内容（支持HTML），查看右侧实时预览
6. 点击"保存全部配置"
```

### API 快速验证脚本

```bash
# 1. 创建订单
curl -X POST http://localhost:8080/api/pay/create \
  -H "Content-Type: application/json" \
  -d '{"packageId":1,"email":"test@test.com","salesCode":""}'

# 2. 获取订单列表找到ID
curl http://localhost:8080/api/admin/orders?page=1\&size=10

# 3. 模拟支付（测试用）
curl -X POST http://localhost:8080/api/admin/orders/{id}/simulate-pay

# 4. 按邮箱查订单
curl "http://localhost:8080/api/customer/orders?email=test@test.com"

# 5. 按订单号查订单
curl "http://localhost:8080/api/customer/order?orderNo=ORDxxxx"
```

---

## 注意事项

1. **orders.auth_code 列已扩容**：从 `VARCHAR(512)` → `VARCHAR(2048)`，已有开发数据库已执行迁移
2. **license_codes.code 列索引**：唯一索引使用前缀255，避免超长 key 限制
3. **邮件发送**：未配置 SMTP 时仅打印日志（mock 模式），不影响订单流程
4. **核销幂等**：`/api/auth/consume` 重复调用不会报错，始终返回 200
5. **授权码不存在**：`/api/auth/check` 对不存在的授权码返回 `consumed: true`（安全策略）

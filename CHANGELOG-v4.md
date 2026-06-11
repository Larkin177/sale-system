# v4 更新文档

> 更新日期：2026-06-11

---

## 目录

1. [数据库变更](#1-数据库变更)
2. [后端新增 API](#2-后端新增-api)
3. [后端修改](#3-后端修改)
4. [前端新增页面](#4-前端新增页面)
5. [前端修改](#5-前端修改)
6. [安装器修改](#6-安装器修改)
7. [Cloudflare Worker](#7-cloudflare-worker)

---

## 1. 数据库变更

### 1.1 新建表

```sql
-- 教程分类表
CREATE TABLE tutorial_categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL COMMENT '分类名称',
  slug VARCHAR(50) UNIQUE NOT NULL COMMENT '分类标识',
  sort_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 销售收款码表
CREATE TABLE sales_payment_codes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sales_id BIGINT NOT NULL,
  code_type VARCHAR(20) NOT NULL COMMENT 'wechat/alipay',
  code_url VARCHAR(500) NOT NULL COMMENT '收款码图片URL',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_sales_id (sales_id)
);

-- 结算记录表
CREATE TABLE settlement_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sales_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL COMMENT '结算金额',
  status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/completed',
  proof_url VARCHAR(500) COMMENT '转账凭证截图URL',
  admin_note VARCHAR(500) COMMENT '管理员备注',
  settled_by BIGINT COMMENT '管理员ID',
  settled_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_sales_id (sales_id),
  INDEX idx_status (status)
);
```

### 1.2 已有表变更

| 表 | 变更 |
|---|---|
| `orders` | 新增 `customer_email` 列（v6 已加） |
| `sales` | 新增 `email` 列 |
| `product_packages` | `delivery_template` 列（v6 已加） |
| `license_codes` | 新建（v2 SQL 脚本，之前遗漏） |

### 1.3 数据变更

```sql
-- 定价调整（sql/v7-standard-prices.sql）
UPDATE products SET base_price=15, min_price=10, max_price=20, auth_validity_hours=24;
UPDATE product_packages SET price=15/25, auth_validity_hours=24;
UPDATE system_config SET config_value='15' WHERE config_key='base_price';
```

---

## 2. 后端新增 API

### 2.1 邮件发送

**`POST /api/admin/email/send`**
- 功能：管理员手动发送邮件
- 请求体：`{ to, subject, content }`
- 文件：`EmailController.java`

### 2.2 结算系统

**`GET /api/admin/commissions/summary`**
- 功能：按销售汇总分润数据
- 返回：每位活跃销售的 totalCommission / settledCommission / pendingCommission / lastSettledAt
- 文件：`SettlementController.java`

**`GET /api/admin/commissions/sales/{salesId}`**
- 功能：查看某销售的分润明细
- 返回：销售信息、分润记录列表、月度趋势数据
- 文件：`SettlementController.java`

**`POST /api/admin/commissions/settle`**
- 功能：创建结算记录
- 请求体：`{ salesId, amount, adminId }`
- 文件：`SettlementController.java`

**`PUT /api/admin/commissions/settle/{id}/proof`**
- 功能：上传结算凭证，完成结算
- 请求体：`{ proofUrl, note }`
- 说明：同时将该销售所有待结算分润标记为已结算
- 文件：`SettlementController.java`

**`PUT /api/admin/sales/{id}/rate`**
- 功能：修改销售的分润比例
- 请求体：`{ rate }`
- 文件：`SettlementController.java`

### 2.3 销售端

**`GET /api/sales/payment-code?salesId=X`**
- 功能：获取销售的收款码列表
- 文件：`SettlementController.java`

**`POST /api/sales/payment-code`**
- 功能：上传/更新收款码（删除同类型旧码）
- 请求体：`{ salesId, codeType, codeUrl }`
- 文件：`SettlementController.java`

**`GET /api/sales/commissions?salesId=X`**
- 功能：销售查看自己的分润
- 返回：totalCommission / settledCommission / pendingCommission / commissions列表
- 文件：`SettlementController.java`

**`POST /api/sales/forgot-password`**
- 功能：销售找回密码，发送新密码到注册邮箱
- 请求体：`{ email }`
- 文件：`SalesAuthController.java`
- 注意：该接口在 AuthFilter 白名单中，无需登录

### 2.4 教程分类

**`GET /api/categories`**
- 功能：公开接口，获取所有教程分类
- 文件：`TutorialController.java`

**`GET /api/admin/categories`**
- 功能：管理端获取教程分类
- 文件：`TutorialController.java`

**`POST /api/admin/categories`**
- 功能：新增/编辑教程分类
- 文件：`TutorialController.java`

**`DELETE /api/admin/categories/{id}`**
- 功能：删除教程分类
- 文件：`TutorialController.java`

### 2.5 Cloudflare Worker 注册

在 `autoDeliver()` 中新增，发货时自动将授权码注册到 Cloudflare Worker：

```
POST https://auth.wonderhow.store/api/auth/register
Body: { code, codeId, version: "sale-system" }
```

文件：`AdminOrderController.java`（`autoDeliver` 方法）

---

## 3. 后端修改

### 3.1 AdminOrderController.java

| 改动 | 说明 |
|---|---|
| `confirmPayment` / `simulatePayment` | 调用 `autoDeliver`，新增 Worker 注册 |
| `autoDeliver` | Cloudflare Worker 注册（`try-catch`）、`saveLicenseCode`（`try-catch`）、`sendDeliveryEmail` 结果日志 |
| `rejectPayment` | 状态改为 `rejected`（不再打回 pending）、自动发送拒绝通知邮件 |
| `sendDeliveryEmail` | 新增结果日志、相对路径转绝对URL |
| `@GetMapping` 列表 | 查询每个订单的销售姓名（`salesMapper` 关联） |
| 导入 | 新增 `SalesMapper`、`HttpURLConnection`、`URI` |

**拒绝订单完整流程：**
1. 设置 `status = "rejected"`
2. 从 `system_config` 读取 `reject_template`
3. 若无模板则使用默认HTML
4. 替换 `{site_name}` `{order_no}` `{amount}` `{reason}` `{email}`
5. 调用 `emailService.sendEmail()` 发送

### 3.2 CustomerController.java

| 改动 | 说明 |
|---|---|
| 新增字段 | 订单API返回 `authValidityHours`（从套餐查询）、`downloadUrl`（从套餐查询） |
| 新增方法 | `getAuthValidityHours()`、`getDownloadUrl()` |
| 新增依赖 | `ProductPackageMapper` |

### 3.3 PayController.java

| 改动 | 说明 |
|---|---|
| `markPaid` | 增加接收 `paymentMethod` 参数，保存到订单 |

### 3.4 OrderService.java

| 改动 | 说明 |
|---|---|
| `createOrderWithPackage` | 新增 `order.setPackageId(pkg.getId())`（之前漏了） |
| `claimOrder` | 支持邮箱验证认领（手机号或邮箱二选一） |

### 3.5 SalesService.java

| 改动 | 说明 |
|---|---|
| `registerSales` | 新增邮箱查重、自定义推广码支持（自动生成兜底） |

### 3.6 SiteSettingController.java

| 改动 | 说明 |
|---|---|
| 文件上传 | 文件名改为 `原始名_短UUID.扩展名`（之前是 `UUID.扩展名`） |
| 新增格式 | `.exe` `.msi` `.dmg` `.pkg` |
| URL路径 | `/uploads/` → `/downloads/` |

### 3.7 EmailService.java

| 改动 | 说明 |
|---|---|
| `sendEmail` | 新增 `\n` → `<br/>` 转换、纯文本URL → 可点击链接 |
| `getMailSender` | 新增 SSL 支持（端口465）、新增超时配置（10s） |

### 3.8 WebConfig.java

| 改动 | 说明 |
|---|---|
| 静态资源 | `/uploads/**` → `/downloads/**` |

### 3.9 AuthFilter.java

| 改动 | 说明 |
|---|---|
| 白名单 | 新增 `/api/sales/forgot-password` |

### 3.10 实体/DTO变更

| 文件 | 变更 |
|---|---|
| `Order.java` | 新增 `salesName` 字段（`@TableField(exist = false)`） |
| `Sales.java` | 新增 `email` 字段 |
| `SalesRegisterRequest.java` | 新增 `email`、`code` 字段，移除 `inviteCode` |
| `ClaimOrderRequest.java` | 新增 `email` 字段 |

---

## 4. 前端新增页面

### 4.1 邮件管理 `/admin/email`

**文件：** `EmailSettings.vue`

| 模块 | 说明 |
|---|---|
| 邮件服务器配置 | SMTP 服务器/端口/发件邮箱/授权码/发件人/站点地址 |
| 发货邮件模板 | HTML模板，支持变量替换，保存到 `system_config.delivery_template` |
| 拒绝通知模板 | 同上，保存到 `system_config.reject_template` |
| 测试发送 | 输入收件箱，发送测试邮件 |

**API 对接：**
- 读取配置：`GET /admin/config`
- 保存配置：`PUT /admin/config/{key}`
- 发送测试：`POST /admin/email/send`

### 4.2 分润结算（管理端） `/admin/settle`

**文件：** `Settle.vue`

| 模块 | 说明 |
|---|---|
| 销售汇总表 | 销售姓名/推广码/分润比例/总佣金/已结算/待结算/上次结算 |
| 分润比例 | 可编辑（`el-input-number`），实时保存 |
| 明细弹窗 | 汇总统计（4项）、月度趋势柱状图、分润记录列表 |
| 结算弹窗 | 收款码展示（从 `sales_payment_codes` 查微信码）、上传凭证截图、备注 |

### 4.3 销售分润 `/s/commission`

**文件：** `Commission.vue`

| 模块 | 说明 |
|---|---|
| 统计卡片 | 总佣金/已结算/待结算 |
| 分润比例 | 展示当前比例 |
| 分润列表 | ID/订单/分润金额/比例/状态/时间 |

### 4.4 销售收款码 `/s/payment-code`

**文件：** `PaymentCode.vue`

| 模块 | 说明 |
|---|---|
| 微信收款码 | 上传/更换/预览 |
| 支付宝收款码 | 上传/更换/预览 |

**API 对接：**
- 查询：`GET /api/sales/payment-code?salesId=X`
- 保存：`POST /api/sales/payment-code`

### 4.5 销售找回密码 `/s/forgot-password`

**文件：** `ForgotPassword.vue`

流程：输入注册邮箱 → 后端生成新密码 → 发送到邮箱 → 提示成功

---

## 5. 前端修改

### 5.1 订单管理 `Orders.vue`

| 改动 | 说明 |
|---|---|
| 绑定销售列 | 改为显示 `salesName` |
| 客户信息列 | 合并手机号+邮箱 |
| 平台列 | 新增，Mac/Win 标签 |
| 套餐列 | 新增，`white-space:nowrap` 防换行 |
| 授权码列 | 自动查询 Cloudflare Worker 状态（`auth.wonderhow.store/api/auth/check`） |
| 邮件按钮 | 每行新增「📧 邮件」按钮，弹窗自定义发送 |
| 拒绝状态 | 新增 `rejected: '已拒绝'` 状态显示 |

### 5.2 系统配置 `Config.vue`

| 改动 | 说明 |
|---|---|
| 移除 | 邮件服务器卡片、发货邮件模板卡片、相关 JS 代码（已迁移到 EmailSettings.vue） |

### 5.3 客户首页 `Home.vue`

| 改动 | 说明 |
|---|---|
| 教程缩略图 | 新增图片类型显示（直接使用 mediaUrl）、标题文字兜底 |
| 教程分类 | 动态加载（`GET /api/categories`），不再硬编码 |
| 教程 Markdown | 新增 `marked` 库渲染、Markdown 样式 |
| 购买支付 | `markPaid` 发送 `paymentMethod` 参数 |
| 启动加载 | 添加 `loadingScreen`，初始化完成后隐藏 |

### 5.4 客户订单页 `Orders.vue`

| 改动 | 说明 |
|---|---|
| 授权有效期 | 显示套餐实际配置（不再硬编码 72） |
| 下载按钮 | 使用 API 返回的 `downloadUrl`，直接跳转 |

### 5.5 管理端登录 `Login.vue`

| 改动 | 说明 |
|---|---|
| 记住密码 | 同时保存用户名+密码到 localStorage（JSON格式） |

### 5.6 销售端登录 `Login.vue`

| 改动 | 说明 |
|---|---|
| 记住密码 | 同上 |
| 忘记密码 | 新增「忘记密码？」链接到 `/s/forgot-password` |

### 5.7 销售注册 `Register.vue`

| 改动 | 说明 |
|---|---|
| 新增字段 | 邮箱（必填）、推广码（自定义，不重复） |
| 提交 | `POST /api/sales/register` 新增 `email`、`code` |

### 5.8 推广链接 `Link.vue`

| 改动 | 说明 |
|---|---|
| 简化 | 移除平台/套餐选择器 |
| 链接格式 | `/?s=CODE`（之前含 packageId 和价格参数） |

### 5.9 认领订单 `Claim.vue`

| 改动 | 说明 |
|---|---|
| 新增字段 | 客户邮箱（手机号或邮箱二选一） |
| 提交 | `POST /api/sales/claim` 新增 `email` |

### 5.10 导航栏

| 文件 | 改动 |
|---|---|
| `AdminLayout.vue` | 新增「邮件管理」菜单项 |
| `SalesLayout.vue` | 新增「分润结算」「收款码管理」菜单项 |

### 5.11 路由

**`router/index.js` 新增路由：**

| 路径 | 页面 | 权限 |
|---|---|---|
| `/admin/email` | EmailSettings.vue | admin |
| `/s/commission` | Commission.vue | sales |
| `/s/payment-code` | PaymentCode.vue | sales |
| `/s/forgot-password` | ForgotPassword.vue | 无 |

### 5.12 套餐管理 `Packages.vue`

| 改动 | 说明 |
|---|---|
| 上传文件 | 新增 `el-upload` 上传下载文件（文件类型已后端扩展） |

### 5.13 教程管理 `Tutorials.vue`

| 改动 | 说明 |
|---|---|
| 分类动态加载 | 从 API 获取分类列表（之前硬编码3个） |
| 分类管理 | 页面底部新增分类增删改功能 |
| Markdown上传 | 新增「上传 .md 文件」按钮 |
| 缩略图上传 | 输入框旁新增「上传图片」 |

---

## 6. 安装器修改

### 6.1 图标修复

| 文件 | 改动 |
|---|---|
| `win/main.js` | BrowserWindow `icon` 从 `icon.png` 改为 `icon.ico`，新增 `app.setAppUserModelId()`，路径同时检查 dev/packaged |
| `win/package.json` | `build.win.signAndEditExecutable`、NSIS 图标配置、`files` 包含 `icon.ico` |
| `win/build/icon.ico` | 多分辨率 ICO（16/32/48/256） |

### 6.2 授权码 API 地址

| 文件 | 改动 |
|---|---|
| `win/main.js` `mac/main.js` | `AUTH_API_BASE` 从空字符串改为 `https://auth.wonderhow.store/api/auth` |

### 6.3 授权有效期

| 文件 | 改动 |
|---|---|
| `win/main.js` `mac/main.js` | `saveLicense(codeId, expiresAt)` 存储真实过期时间 |
| `checkLocalLicense()` | 读取存储的过期时间返回，不再硬编码"永久有效" |
| `verifyAuthCode` | 传递 `payload.exp` 到 `saveLicense` |

### 6.4 加载画面

| 文件 | 改动 |
|---|---|
| `renderer/index.html` | 新增 `loadingScreen`（白色+spinner） |
| `renderer/app.js` | 初始化完成后 `finally` 隐藏 loading |

---

## 7. Cloudflare Worker

### 文件：`E:\DevEnv\ai-installer\v3\worker.js`

**API 接口（部署在 `https://auth.wonderhow.store`）：**

| 接口 | 功能 | 存储 |
|---|---|---|
| `POST /api/auth/register` | 注册授权码 | KV `codeId:{codeId}` → 完整记录 |
| `POST /api/auth/check` | 查询是否已核销 | 从完整授权码提取 codeId，查 KV |
| `POST /api/auth/consume` | 核销授权码 | 更新 KV 记录为已消费 |

**关键实现：**
- 使用 `addEventListener('fetch')` 格式（Service Worker）
- CORS 跨域支持
- 通过 `extractCodeId()` 从授权码中解析 payload 提取 codeId
- KV key 使用 `codeId:{codeId}`，避免完整授权码过长（777字符超限）
- KV 命名空间：`CC_INSTALLER_AUTH`，绑定变量：`AUTH_KV`

### 自定义域名

- 域名：`auth.wonderhow.store`
- DNS：Cloudflare 管理（NS: `novalee.ns.cloudflare.com` / `rayden.ns.cloudflare.com`）
- Worker 绑定：Settings → Domains & Routes → Custom Domain

---

# v4.1 更新文档（2026-06-11 晚）

> 注意：以下改动基于 v4 基础版本（commit `57d669f`）之后的增量修改。

---

## 8. 数据库变更（v4.1）

### 8.1 新建表

```sql
-- 分润比例变更日志表
CREATE TABLE commission_rate_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sales_id BIGINT NOT NULL COMMENT '销售ID',
  old_rate DECIMAL(5,2) NOT NULL COMMENT '变更前比例',
  new_rate DECIMAL(5,2) NOT NULL COMMENT '变更后比例',
  created_by BIGINT COMMENT '操作管理员ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_sales_id (sales_id)
);
```

### 8.2 SQL 种子数据

新增 `sql/seed-data.sql`：
- 清空 orders / commissions，重置 product_packages 为干净 6 个套餐
- 插入销售演示数据（周媛，40% 分润比例）
- 系统配置默认分润比例 → 40%
- 功能特性图标更新（零门槛→MagicStick，傻瓜式操作→Mouse，DeepSeek友好→Connection）

---

## 9. 后端新增（v4.1）

### 9.1 新 API

**`GET /api/sales/rate-logs?salesId=X`**
- 功能：获取分润比例变更历史
- 返回：`[{ id, salesId, oldRate, newRate, createdAt }]`
- 文件：`SettlementController.java`

### 9.2 新文件

| 文件 | 说明 |
|---|---|
| `entity/CommissionRateLog.java` | 分润比例变更日志实体 |
| `mapper/CommissionRateLogMapper.java` | 对应 MyBatis Plus Mapper |

### 9.3 公开配置新增

`GET /api/config` 新增返回 `qrcode_logo`（推广二维码中心图标 URL）
- 文件：`ConfigController.java` / `ConfigService.java`

---

## 10. 后端修改（v4.1）

### 10.1 SettlementController.java

| 改动 | 说明 |
|---|---|
| 新增 `orderMapper` 依赖 | 用于查询订单数据计算总销售额 |
| 新增 `enrichCommissions()` | 批量给佣金记录附带订单信息（orderNo, orderAmount, orderStatus, orderCreatedAt），消除 N+1 手动查询 |
| 新增 `getTotalSales()` | 从 orders 表 SUM(amount) 计算销售总销售额 |
| **summary 端点** | `totalSales` 改为从 orders 表查询（之前错赋值为 totalCommission） |
| **detail 端点** | `totalSales` 修复为 0 的 bug，改为正确查询 orders 表 |
| **detail 端点** | `commissions` 改为返回 `enrichCommissions()` 含订单详情 |
| **detail 端点** | 新增返回 `settlementRecords`（结算记录含凭证） |
| **settle 端点** | `POST /admin/commissions/settle` 改为直接完成结算（状态 `completed` + 同时标记待结算分润为已结算），之前需要两次调用（POST + PUT）才能完成 |
| **settle 端点** | 返回 `{ id }` 让前端可继续上传凭证 |
| **rate 端点** | `PUT /admin/sales/{id}/rate` 新增记录比例变更到 `commission_rate_logs` |

### 10.2 CommissionService.java

| 改动 | 说明 |
|---|---|
| 提取 `doCalculate()` | 将分润计算逻辑抽取为私有方法，消除重复代码 |
| 新增 `calculateCommissionForClaim()` | 认领订单时计算分润，绕过订单状态和 sales_id 检查，由调用方传入 salesId |

### 10.3 OrderService.java

| 改动 | 说明 |
|---|---|
| `listUnclaimedOrders()` | 改为排除 `pending/rejected/settled` 状态（之前只查 `paid`），涵盖 `paid/pending_verify/delivered` |
| `claimOrder()` | 改 email 为必填、phone 为选填 |
| `claimOrder()` | 认领前调用 `calculateCommissionForClaim()` 计算分润 |

### 10.4 OrderMapper.java

| 改动 | 说明 |
|---|---|
| `claimOrder` SQL | 之前 `status = 'bound'` → **去掉 status 变更**，认领只绑定 sales_id + 记录认领时间，不修改订单状态 |
| `claimOrder` SQL | 之前 `status = 'paid'` → 改为 `IN ('paid', 'pending_verify', 'delivered')` |

### 10.5 SalesService.java

| 改动 | 说明 |
|---|---|
| `updateSales()` | 新增比例变更检测，修改 commissionRate 时自动写入 `commission_rate_logs` |
| 新增依赖 | `CommissionRateLogMapper` |

### 10.6 ConfigService.java

| 改动 | 说明 |
|---|---|
| `updateConfig()` | 之前只更新已有记录（新 key 存不进去），改为不存在时自动 INSERT |

### 10.7 ClaimOrderRequest.java

| 改动 | 说明 |
|---|---|
| phone | `@NotBlank` 改为无校验（选填） |
| email | 从无校验改为 `@NotBlank`（必填） |

### 10.8 WebConfig.java

| 改动 | 说明 |
|---|---|
| 新增 `passwordEncoder()` Bean | 注册 `BCryptPasswordEncoder`，修复 `SalesAuthController` 因缺少 Bean 启动失败的问题 |

### 10.9 授权有效期默认值改为 24 小时

涉及 5 个文件，所有 `int validityHours = 72` 或 `: 72` 改为 `= 24` / `: 24`：

| 文件 | 行数 |
|---|---|
| `WebhookController.java` | 2 处 (`= 72` → `= 24`) |
| `AlipayController.java` | 1 处 |
| `DeliveryController.java` | 2 处 (`: 72` → `: 24`) |
| `AdminOrderController.java` | 2 处 |

### 10.10 默认分润比例 10% → 40%

`SalesService.java` 两处 `new BigDecimal("10")` → `new BigDecimal("40")`

---

## 11. 前端修改（v4.1）

### 11.1 客户首页 `Home.vue`

| 改动 | 说明 |
|---|---|
| 购买弹窗邮箱 | `buyEmail` 不再从 localStorage 自动填充（消除了之前残留的"1114673990"） |
| 功能特性图标 | 图标映射从 5 个扩展为 30+ 个（新增 MagicStick, Aim, Mouse, Connection, Headset 等） |

### 11.2 系统配置 `Config.vue`

| 改动 | 说明 |
|---|---|
| 推广二维码图标 | 新增配置卡片：支持上传图片 / 输入 URL，保存到 `system_config.qrcode_logo` |
| 二维码预览 | 上传后实时生成带图标的二维码预览（引入 `qrcode` 库在 canvas 上叠加图标） |

### 11.3 站点设置 `SiteSettings.vue`

| 改动 | 说明 |
|---|---|
| 移除支付二维码 | 删除微信/支付宝收款码上传区块（已迁移到系统配置） |
| 移除相关函数 | 删除 `handleQrcodeUpload()`、`wechat_qrcode`/`alipay_qrcode` 默认值及 CSS |
| 图标选择器 | 从 5 个选项扩展为 30+ 个，加 `filterable` 支持搜索 |

### 11.4 分润结算 `Settle.vue`

| 改动 | 说明 |
|---|---|
| 汇总表列 | 改为：销售姓名 / 推广码 / **总销售额** / **总佣金** / **已结算** / 当前分润比例 / 待结算 / 操作 |
| 分润比例列 | 从可编辑 `el-input-number` 改为纯文字展示（编辑移至销售管理） |
| 明细弹窗 | 加宽至 1000px；新增「结算记录」表格（含凭证预览）；分润列表列改为：序号 / 订单编号 / 订单金额 / 分润比例 / 分润金额 / 订单状态 / 订单创建时间 |
| 分页 | 汇总表 + 明细弹窗佣金列表均新增分页 |

### 11.5 销售端业绩概览 `Dashboard.vue`

| 改动 | 说明 |
|---|---|
| 分润统计 | 在原有 4 个统计卡片下方新增一行 4 个卡片（总佣金 / 已结算 / 待结算 / 当前分润比例） |
| API | 新增请求 `/sales/commissions` 获取分润数据 |

### 11.6 销售端分润结算 `Commission.vue`

| 改动 | 说明 |
|---|---|
| 结算记录 | 改为左右两栏布局（左 14/24 结算记录，右 10/24 分润比例变更记录） |
| 比例变更记录 | 调用 `GET /api/sales/rate-logs`，显示变更前 / 变更后 / 变更时间 |
| 分润明细列 | 改为：序号 / 订单编号 / 订单金额 / 分润比例 / 分润金额 / 订单状态 / 订单创建时间 |
| 分页 | 分润明细列表新增分页 |

### 11.7 推广链接 `Link.vue`

| 改动 | 说明 |
|---|---|
| 二维码尺寸 | 200px → 240px |
| 颜色 | 深色块从纯黑改为 `#1a1a2e` |
| Logo叠加 | 新增中心图标叠加（白色圆角背景 + 阴影效果 + 内边距） |
| 样式 | 新增独立白卡片容器包裹二维码（圆角阴影） |

### 11.8 认领订单 `Claim.vue`

| 改动 | 说明 |
|---|---|
| 邮箱 | 改为必填（置顶、加 required 规则） |
| 手机号 | 改为选填（标注「选填」、去掉 required） |
| 错误提示 | 捕获 API 错误并显示到界面上 |

### 11.9 导航栏 `SalesLayout.vue`

| 改动 | 说明 |
|---|---|
| 排行榜 | 整块菜单注释掉（保留代码） |
| 认领订单 | 图标从无效的 `Claim` 改为 `Collection`（Element Plus 内置） |

### 11.10 订单管理 `Orders.vue`（管理端 + 销售端）

| 改动 | 说明 |
|---|---|
| 分页布局 | `layout` 全部加上 `total`，左下角显示「共 X 条」 |

### 11.11 套餐管理 `Packages.vue` + 教程管理 `Tutorials.vue`

| 改动 | 说明 |
|---|---|
| `Packages.vue` | 分页加上 `total` |
| `Tutorials.vue` | **新增**完整前端分页（每页10条），`page` + `total` + `paginatedTutorials` computed |

### 11.12 销售端订单 `Orders.vue`

| 改动 | 说明 |
|---|---|
| 状态映射 | 移除 `bound` 相关映射 |

### 11.13 其他

| 文件 | 改动 |
|---|---|
| `App.vue` | `<router-view>` 加 `:key="$route.fullPath"`，修复导航后页面不刷新的问题 |
| `router/index.js` | 排行榜路由注释掉 |
| `request.js` | 错误拦截器改为显示 `error.response?.data?.message`（之前只显示通用 "Request failed"） |
| `SalesDetail.vue` / `Dashboard.vue` / `Orders.vue` | 移除所有 `bound` 状态映射（已废弃） |

---

## 12. 已知问题 / 待办

- 推广二维码中心图标需在 系统配置 页面上传并保存后才生效
- 已有 `bound` 状态的订单已手动更新为 `delivered`（2026-06-11）
- `commission_rate_logs` 表中无历史数据，比例变更记录从本次部署后开始生成

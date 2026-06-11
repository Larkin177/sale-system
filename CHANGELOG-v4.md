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

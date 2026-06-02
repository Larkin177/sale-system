# 销售分润系统 更新日志

## v2.0.0 (2026-06-02)

---

### 一、多产品架构

- 新增 `products` 表，支持多个产品的独立管理
- 初始产品：**CC-Installer 一键安装工具**（slug: `cc-installer`）
- 产品属性：名称、描述、版本号、下载链接、基础价格、最低/最高定价、授权开关、授权有效时长、默认分润比例、状态
- 订单表 `orders` 新增字段：`product_id`、`product_name`、`auth_code`、`auth_status`、`auth_used_at`、`auth_machine`、`auth_uses`、`auth_last_validated_at`
- 新增授权码操作日志表 `auth_logs`（记录 validate/revoke/use 操作、机器指纹、IP 地址、结果与原因）

**涉及文件：**
- `entity/Product.java` — 产品实体（products 表映射）
- `mapper/ProductMapper.java` — 产品数据访问
- `service/ProductService.java` — 产品 CRUD 逻辑
- `controller/ProductController.java` — 管理端产品接口（GET/POST/PUT/toggle-status）
- `dto/ProductDTO.java` — 产品创建/更新请求体，含参数校验（slug 格式、价格范围、分润比例 0-100）
- `entity/AuthLog.java` — 授权日志实体（auth_logs 表映射）
- `mapper/AuthLogMapper.java` — 授权日志数据访问
- `sql/v2-schema.sql` — products 表建表、orders 表 ALTER、auth_logs 表建表、索引

---

### 二、授权码云端验证

- `POST /api/auth/validate` — 客户端验证授权码，支持机器指纹绑定
- `POST /api/auth/redeem` — 客户端核销回调（验证成功后标记为 redeemed）
- `POST /api/auth/revoke/{orderId}` — 管理端撤销授权码
- `GET /api/admin/auth/logs` — 管理端查看授权日志（分页）
- 机器指纹绑定机制：首次验证绑定设备，后续验证比对指纹，防止授权码转卖
- 授权码状态流转：`active` → `used` → `redeemed`（正常核销）/ `revoked`（撤销）/ `expired`（过期）
- 授权码格式：`AIC-{payload_base64}.{signature_base64}`，使用 RSA-SHA256 签名
- 无私钥时降级为 Mock 模式：`AIC-MOCK-{12位随机字符}`

**涉及文件：**
- `service/AuthCodeService.java` — RSA 私钥加载、授权码生成（JWT 风格 payload + RSA 签名）
- `service/AuthValidationService.java` — 验证逻辑：查找订单 → 检查状态 → 检查产品授权开关 → 首次绑定/后续比对指纹 → 检查过期 → 更新验证次数 → 记录日志
- `controller/AuthValidationController.java` — validate/redeem/revoke/logs 四个端点
- `dto/AuthValidateRequest.java` — 请求体（authCode + machine，均 @NotBlank）
- `dto/AuthValidateResponse.java` — 响应体（ok/error、expiresAt、uses、authStatus、product）

---

### 三、站点设置（CMS）

- 管理员可通过 `/admin/site-settings` 页面自定义全部客户可见内容
- 可配置项：
  - **基本信息**：站点名称 `site_name`、站点副标题 `site_subtitle`、页脚文字 `footer_text`
  - **Hero 区域**：主标题 `hero_title`、副标题 `hero_subtitle`、渐变起始色 `hero_bg_color`、渐变结束色 `hero_bg_color_end`、背景图片 `hero_image`
  - **特性卡片**（3 组）：图标选择（Trophy/Star/Service/Coin/Position）、自定义图标 URL、标题、描述、点击内容类型（none/image/video/text/download）、内容值
  - **页面标题**：购买页标题/副标题、下载页标题/副标题
  - **支付二维码**：微信收款码 `wechat_qrcode`、支付宝收款码 `alipay_qrcode`
- 客户端接口 `GET /api/site-settings` 仅返回 PUBLIC_KEYS 列表中的设置（安全过滤）
- 管理端接口 `GET/PUT /api/admin/site-settings` 支持全量读取和批量更新
- 保存后前端自动刷新站点名称（`refreshSiteName()`）

**涉及文件：**
- `entity/SiteSetting.java` — 站点设置实体（site_settings 表映射，setting_key 唯一索引）
- `mapper/SiteSettingMapper.java` — 站点设置数据访问
- `service/SiteSettingService.java` — PUBLIC_KEYS 白名单、getAllSettings/getPublicSettings/getSetting/updateSetting/batchUpdate
- `controller/SiteSettingController.java` — 公开接口 + 管理端接口
- `views/admin/SiteSettings.vue` — 管理端站点设置页面，含实时预览面板（渐变 Hero + 特性卡片预览）
- `sql/v2-site-settings.sql` — 默认设置数据（34 项）
- `sql/v2-feature-cards.sql` — 特性卡片扩展字段（icon_url、content_type、content_value）

---

### 四、客户首页

- Hero 区域渐变背景，颜色通过 CSS 变量 `--hero-bg` / `--hero-bg-end` 动态绑定，支持自定义
- 3 个特性卡片，支持自定义图标（内置图标或自定义图片 URL）
- 特性卡片点击弹窗（el-dialog），支持 4 种内容类型：
  - **image**：展示图片
  - **video**：内嵌视频播放器
  - **text**：展示文本内容
  - **download**：提供下载按钮
- 加载骨架屏（Skeleton Loading）动画
- 移动端响应式设计（768px 断点）

**涉及文件：**
- `views/customer/Home.vue` — 首页组件，从 `/api/site-settings` 动态加载内容

---

### 五、支付流程

- **弹窗式支付**：渐变遮罩（`linear-gradient` overlay）+ 圆角弹窗（20px radius）
- **Step 1：手机号验证**
  - 手机号输入（11 位限制，`/^1[3-9]\d{9}$/` 格式校验）
  - 图形验证码（输入满 11 位自动弹出）
  - 短信验证码（6 位，60 秒倒计时防刷）
  - 隐私提示区域
- **Step 2：价格确认 + 支付**
  - 显示订单金额（历史价格或基础价格）
  - 手机号脱敏显示（中间 4 位打码）
  - 支付方式选择（微信支付 / 支付宝，radio-button 组）
  - 提交后展示二维码（优先支付宝动态二维码，降级为静态收款码）
  - 支持更换支付方式
- **价格记忆**：客户手机号绑定价格（customer_prices 表），跨渠道一致
  - 优先级：customer_prices 表 → 历史订单金额 → 基础价格/销售链接价格
- 从首页跳转时自动弹出支付弹窗（`?auto=1` 参数）

**涉及文件：**
- `views/customer/Pay.vue` — 支付页面，含 Hero、特性卡片、支付弹窗（两步流程）
- `controller/PaymentController.java` — `GET /api/payment/qrcode` 返回静态收款码 URL
- `controller/CustomerController.java` — `GET /api/customer/last-order`（历史订单）+ `GET /api/customer/price`（价格记忆）
- `service/OrderService.java` — `getCustomerLastOrder()`、`getCustomerPrice()`、`saveCustomerPrice()`
- `entity/CustomerPrice.java` — 客户价格记忆实体（customer_prices 表映射）
- `mapper/CustomerPriceMapper.java` — 客户价格数据访问
- `sql/v2-customer-prices.sql` — customer_prices 表建表（phone + sales_id 唯一索引）
- `sql/v2-payment-qrcode.sql` — 微信/支付宝收款码设置项

---

### 六、验证码系统

- **图形验证码**（Java AWT 生成）：
  - 尺寸 120x40px，4 位随机字符（A-Z + 0-9）
  - 随机字体（Arial/Verdana/Times New Roman）、随机颜色、随机旋转角度
  - 噪声线（5-8 条）+ 噪声点（50-100 个）
  - Base64 编码返回，前端直接展示
  - 5 分钟过期，ConcurrentHashMap 存储，每分钟自动清理
  - 接口：`GET /api/captcha/generate`（返回 id + image）、`POST /api/captcha/verify`
- **短信验证码**（6 位数字）：
  - 手机号格式校验（`^1[3-9]\d{9}$`）
  - 60 秒防刷：查询最近 60 秒内是否已发送
  - 5 分钟有效期
  - 使用后标记为 used，不可重复使用
  - 接口：`POST /api/verification/send`、`POST /api/verification/verify`
  - 开发环境仅打印日志（`[SMS] Phone: xxx Code: xxx`）

**涉及文件：**
- `controller/CaptchaController.java` — 图形验证码生成与验证，含定时清理
- `controller/VerificationController.java` — 短信验证码发送与验证
- `service/VerificationService.java` — 验证码业务逻辑（格式校验、防刷、过期检查）
- `entity/VerificationCode.java` — 验证码实体（verification_codes 表映射）
- `mapper/VerificationCodeMapper.java` — 验证码数据访问
- `sql/v2-verification.sql` — verification_codes 表建表

---

### 七、支付集成

- **支付宝沙箱对接**：
  - 使用 `AlipayTradePrecreate API` 生成收款二维码
  - 配置项：`alipay.app-id`、`alipay.private-key`、`alipay.alipay-public-key`、`alipay.gateway-url`、`alipay.notify-url`
  - RSA2 签名验证
- **Mock 模式**：未配置 APPID 时自动降级，返回模拟二维码 URL（`https://sandbox.alipay.com/mock/qr/{orderNo}`）
- **异步回调通知**：
  - `POST /api/alipay/notify` — 接收支付宝异步通知
  - 签名验证（`AlipaySignature.rsaCheckV1`）
  - 交易状态检查（`TRADE_SUCCESS` / `TRADE_FINISHED`）
  - 自动更新订单状态为 paid，记录交易号，触发分润计算
- **管理端可配置**：
  - 微信/支付宝静态收款码（site_settings 表）
  - 支付宝状态查询接口 `GET /api/alipay/status`

**涉及文件：**
- `service/AlipayService.java` — AlipayClient 初始化、createQrCode、verifyNotify
- `controller/AlipayController.java` — create、notify、status 三个端点
- `pom.xml` — 新增 `alipay-sdk-java` 依赖
- `resources/application.yml` — 支付宝沙箱配置项

---

### 八、管理端

- **站点设置页面**（`/admin/site-settings`）：
  - 分区表单：基本信息、Hero 区域、功能特性（3 组）、页面标题、支付二维码
  - 右侧实时预览面板（渐变 Hero + 特性卡片缩略图）
  - 颜色选择器（el-color-picker）支持透明度
  - 保存后自动刷新全局站点名称
- **订单管理**（`/admin/orders`）：
  - 模拟支付按钮（`POST /api/admin/orders/{id}/simulate-pay`，仅测试环境使用）
  - **自动发货**：支付完成后（模拟支付/支付宝回调）自动执行：
    - 生成授权码（AuthCodeService，RSA 签名）
    - 保存授权码到订单（auth_code、auth_status=active、product_id=1）
    - 构建发货消息（替换模板变量：{site_name} {order_no} {amount} {auth_code} {download_url}）
    - 订单状态自动变为 `delivered`（已发货）
    - 开发环境打印短信日志（生产环境对接短信 API）
  - 订单状态展示：待支付 → 已支付 → 已自动发货 → 已核销
  - 无需手动点击"发货"按钮，支付即发货
- **发货消息模板**：
  - 存储于 `system_config` 表（`delivery_template` 键）
  - 支持变量：`{site_name}`、`{order_no}`、`{amount}`、`{phone}`、`{download_url}`、`{auth_code}`
  - 管理端可编辑模板内容
- **JWT 鉴权**：
  - `AuthInterceptor` 拦截所有 `/api/**` 请求
  - 管理端接口仅 admin 角色可访问
  - 销售端接口仅 sales 角色可访问
  - OPTIONS 请求放行，Token 通过 `Authorization: Bearer {token}` 传递
- **动态系统名称**：站点设置中的 `site_name` 自动应用到管理端/销售端标题栏

**涉及文件：**
- `controller/AdminOrderController.java` — 订单列表、未认领订单、模拟支付 + 自动发货
- `controller/AlipayController.java` — 支付宝回调 + 自动发货
- `controller/DeliveryController.java` — 发货预览、发送发货消息、下载 token 生成
- `service/AuthCodeService.java` — 授权码生成（RSA 签名，与 AI Installer 兼容）
- `config/AuthInterceptor.java` — JWT 鉴权拦截器（角色检查、Token 解析）
- `config/WebConfig.java` — 拦截器注册，排除公开路径（登录/支付/下载/验证码/授权验证等）
- `views/admin/Orders.vue` — 订单管理页面（模拟支付 + 状态展示，无需手动发货）
- `views/admin/Config.vue` — 基础配置页面
- `components/AdminLayout.vue` — 管理端布局（动态标题 + 站点设置菜单项）
- `sql/v2-delivery-message.sql` — 发货消息模板（扩展 config_value 为 TEXT 类型）
- `sql/v2-delivery-auth-code.sql` — 更新发货模板增加 `{auth_code}` 变量

---

### 九、销售端

- **推广链接生成**（`/s/link`）：自定义价格 + 生成专属推广链接和二维码
- **业绩概览**（`/s/dashboard`）：总成交额、本月成交额、订单数量统计
- **订单认领**（`/s/claim`）：查看未认领订单列表，一键认领
- **排行榜**（`/s/leaderboard`）：销售业绩排名
- 登录页面支持"记住密码"功能（localStorage 存储）

**涉及文件：**
- `views/sales/Link.vue` — 推广链接页面
- `views/sales/Dashboard.vue` — 业绩概览页面
- `views/sales/Claim.vue` — 订单认领页面
- `views/sales/Leaderboard.vue` — 排行榜页面
- `views/sales/Login.vue` — 销售登录（记住密码 + 自动回填）
- `components/SalesLayout.vue` — 销售端布局（动态标题）

---

### 十、技术改进

- **JWT 鉴权拦截器**（`AuthInterceptor` + `WebConfig`）：
  - 统一拦截 `/api/**`，按路径和角色控制访问权限
  - 排除公开接口：登录、注册、支付、下载、验证码、授权验证、站点设置等
  - 统一错误响应格式（`ApiResponse`）
- **UTF-8 编码修复**：确保 Windows 环境下 curl 请求中文正常（`charset=UTF-8`）
- **前端路由守卫**：
  - 检查 Token 和角色，未登录跳转登录页，角色不匹配跳转首页
  - 动态设置浏览器标签标题（根据路径前缀区分管理端/销售端/客户端）
  - 站点名称缓存机制，避免每次导航重复请求
- **登录记住密码**：
  - 管理端和销售端均支持（`localStorage` 存储账号密码）
  - 页面加载时自动回填
- **动态标题**：管理端和销售端布局组件从站点设置读取 `site_name`，实时更新标题栏
- **前端 API 扩展**（`api/config.js`）：新增站点设置、客户价格、验证码、图形验证码、支付二维码等 API 方法

**涉及文件：**
- `config/AuthInterceptor.java` — 拦截器实现
- `config/WebConfig.java` — 路径排除配置
- `router/index.js` — 路由守卫 + 动态标题 + 站点名称缓存
- `views/admin/Login.vue` — 管理端登录（记住密码）
- `views/sales/Login.vue` — 销售端登录（记住密码）
- `components/AdminLayout.vue` — 动态标题 + 站点设置菜单
- `components/SalesLayout.vue` — 动态标题
- `api/config.js` — API 方法扩展
- `utils/request.js` — Axios 请求封装（Token 自动注入 + 统一错误处理）

---

### 文件变更清单

#### 后端新增（19 个文件）

| 文件 | 说明 |
|------|------|
| `entity/Product.java` | 产品实体（products 表） |
| `entity/AuthLog.java` | 授权日志实体（auth_logs 表） |
| `entity/SiteSetting.java` | 站点设置实体（site_settings 表） |
| `entity/VerificationCode.java` | 验证码实体（verification_codes 表） |
| `entity/CustomerPrice.java` | 客户价格记忆实体（customer_prices 表） |
| `mapper/ProductMapper.java` | 产品数据访问 |
| `mapper/AuthLogMapper.java` | 授权日志数据访问 |
| `mapper/SiteSettingMapper.java` | 站点设置数据访问 |
| `mapper/VerificationCodeMapper.java` | 验证码数据访问 |
| `mapper/CustomerPriceMapper.java` | 客户价格数据访问 |
| `service/ProductService.java` | 产品 CRUD 逻辑 |
| `service/AuthValidationService.java` | 授权码验证逻辑（绑定/比对/过期/日志） |
| `service/SiteSettingService.java` | 站点设置读写（PUBLIC_KEYS 白名单） |
| `service/VerificationService.java` | 短信验证码逻辑（格式校验/防刷/过期） |
| `service/AlipayService.java` | 支付宝 SDK 封装（QR 码生成/签名验证/Mock 模式） |
| `service/AuthCodeService.java` | 授权码生成（RSA 签名 + JWT 风格 payload） |
| `controller/ProductController.java` | 产品管理接口 |
| `controller/AuthValidationController.java` | 授权验证接口（validate/redeem/revoke/logs） |
| `controller/SiteSettingController.java` | 站点设置接口（公开 + 管理端） |

#### 后端新增控制器（7 个文件）

| 文件 | 说明 |
|------|------|
| `controller/VerificationController.java` | 短信验证码发送/验证接口 |
| `controller/CaptchaController.java` | 图形验证码生成/验证接口（Java AWT） |
| `controller/AlipayController.java` | 支付宝支付接口（create/notify/status） |
| `controller/PaymentController.java` | 静态支付二维码接口 |
| `controller/CustomerController.java` | 客户端公开接口（历史订单/价格记忆） |
| `controller/DeliveryController.java` | 发货接口（预览/发送/下载 token） |

#### 后端新增配置（2 个文件）

| 文件 | 说明 |
|------|------|
| `config/AuthInterceptor.java` | JWT 鉴权拦截器（角色检查 + Token 解析） |
| `config/WebConfig.java` | 拦截器注册 + 路径排除配置 |

#### 后端新增 DTO（3 个文件）

| 文件 | 说明 |
|------|------|
| `dto/ProductDTO.java` | 产品创建/更新请求体 |
| `dto/AuthValidateRequest.java` | 授权验证请求体 |
| `dto/AuthValidateResponse.java` | 授权验证响应体 |

#### 后端修改

| 文件 | 变更 |
|------|------|
| `entity/Order.java` | 新增 v2 字段：productId、productName、authCode、authStatus、authUsedAt、authMachine、authUses、authLastValidatedAt |
| `controller/AdminOrderController.java` | 新增模拟支付 + 自动发货（生成授权码+发短信） |
| `controller/PayController.java` | 支付宝集成 + 价格记忆（customer_prices 查询与保存） |
| `service/OrderService.java` | 新增 `getCustomerLastOrder()`、`getCustomerPrice()`、`saveCustomerPrice()` |
| `resources/application.yml` | 新增支付宝沙箱配置项 |
| `pom.xml` | 新增 `alipay-sdk-java` 依赖 |

#### 前端新增（1 个文件）

| 文件 | 说明 |
|------|------|
| `views/admin/SiteSettings.vue` | 站点设置管理页面（表单 + 实时预览面板） |

#### 前端修改（10 个文件）

| 文件 | 变更 |
|------|------|
| `views/customer/Home.vue` | 动态内容加载（Hero/特性卡片/自定义图标/点击弹窗） |
| `views/customer/Pay.vue` | 弹窗支付流程（图形验证码 + 短信验证码 + 两步支付 + 二维码 + 价格记忆） |
| `views/customer/Download.vue` | 动态标题（从站点设置加载） |
| `views/admin/Login.vue` | 记住密码 + 自动回填 |
| `views/admin/Config.vue` | 基础配置页面 |
| `views/admin/Orders.vue` | 模拟支付 + 状态展示（已自动发货/已核销），无需手动发货 |
| `views/sales/Login.vue` | 记住密码 + 自动回填 |
| `components/AdminLayout.vue` | 动态标题 + 站点设置菜单项 |
| `components/SalesLayout.vue` | 动态标题 |
| `router/index.js` | 路由守卫 + 动态标题 + 站点名称缓存 + 新路由（/admin/site-settings） |
| `api/config.js` | 新增 API 方法：站点设置、客户价格、验证码、图形验证码、支付二维码 |
| `utils/request.js` | Axios 封装（Token 注入 + 统一错误处理） |

#### SQL 脚本（8 个文件）

| 文件 | 说明 |
|------|------|
| `sql/v2-schema.sql` | products 表建表 + orders 表 ALTER + auth_logs 表建表 |
| `sql/v2-site-settings.sql` | site_settings 表建表 + 默认设置数据（34 项） |
| `sql/v2-feature-cards.sql` | 特性卡片扩展字段（icon_url、content_type、content_value） |
| `sql/v2-verification.sql` | verification_codes 表建表 |
| `sql/v2-customer-prices.sql` | customer_prices 表建表 |
| `sql/v2-payment-qrcode.sql` | 微信/支付宝收款码设置项 |
| `sql/v2-delivery-message.sql` | 发货消息模板 + config_value 扩展为 TEXT |
| `sql/v2-delivery-auth-code.sql` | 更新发货模板增加 `{auth_code}` 变量 |

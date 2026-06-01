# 销售分润系统

一套完整的销售分润管理系统，包含客户端、销售端、管理端三个端。

## 功能概述

### 客户端
- 首页展示（工具介绍、销售排行榜）
- 通过销售推广链接购买
- 支付购买（微信/支付宝）
- 下载软件

### 销售端
- 注册/登录
- 业绩概览（总成交单数、总成交额、本月数据）
- 推广链接管理（自定义价格、生成二维码）
- 我的订单列表
- 认领订单
- 排行榜

### 管理端
- 数据总览（总营收、总分润、总订单数、活跃销售）
- 销售管理（添加/编辑/禁用销售）
- 订单管理（查看所有订单）
- 分润结算
- 系统配置（价格、分润比例）

## 业务流程

```
1. 销售注册账号 → 获得唯一推广码
2. 销售生成推广链接 → http://域名/pay?s=推广码&p=价格
3. 销售分享链接给客户
4. 客户点击链接 → 填写手机号 → 选择支付方式
5. 客户付款到管理员账户
6. 支付成功 → 系统给客户手机号发送下载链接
7. 订单自动绑定到销售 → 销售获得分润佣金
```

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + Pinia + Vite |
| 后端 | Spring Boot 3 + MyBatis-Plus + JWT |
| 数据库 | MySQL 8 |
| 部署 | Docker Compose |

## 项目结构

```
sale-system/
├── backend/                          # 后端项目
│   ├── src/main/java/com/sales/
│   │   ├── config/                   # 配置类
│   │   │   ├── CorsConfig.java       # 跨域配置
│   │   │   ├── DataInitializer.java  # 数据初始化
│   │   │   ├── GlobalExceptionHandler.java  # 全局异常处理
│   │   │   └── MyBatisPlusConfig.java # MyBatis-Plus配置
│   │   ├── controller/               # 控制器
│   │   │   ├── AuthController.java   # 认证（登录）
│   │   │   ├── SalesController.java  # 销售管理（管理端）
│   │   │   ├── SalesRegisterController.java # 销售注册
│   │   │   ├── OrderController.java  # 订单（销售端）
│   │   │   ├── AdminOrderController.java # 订单（管理端）
│   │   │   ├── PayController.java    # 支付
│   │   │   ├── ConfigController.java # 系统配置
│   │   │   ├── LeaderboardController.java # 排行榜
│   │   │   ├── CommissionController.java # 分润结算
│   │   │   ├── DownloadController.java # 下载
│   │   │   └── WebhookController.java # 支付回调
│   │   ├── dto/                      # 数据传输对象
│   │   ├── entity/                   # 实体类
│   │   ├── mapper/                   # MyBatis-Plus Mapper
│   │   ├── service/                  # 服务层
│   │   └── util/                     # 工具类
│   │       └── JwtUtil.java          # JWT工具
│   └── src/main/resources/
│       └── application.yml           # 配置文件
│
├── frontend/                         # 前端项目
│   ├── src/
│   │   ├── api/                      # API接口
│   │   │   ├── auth.js              # 认证API
│   │   │   ├── sales.js             # 销售API
│   │   │   ├── order.js             # 订单API
│   │   │   ├── config.js            # 配置API
│   │   │   └── admin.js             # 管理端API
│   │   ├── components/               # 公共组件
│   │   │   ├── SalesLayout.vue       # 销售端布局
│   │   │   └── AdminLayout.vue       # 管理端布局
│   │   ├── router/                   # 路由配置
│   │   ├── stores/                   # Pinia状态管理
│   │   │   └── auth.js              # 认证状态
│   │   ├── utils/                    # 工具函数
│   │   │   └── request.js           # Axios封装
│   │   └── views/                    # 页面组件
│   │       ├── customer/             # 客户端页面
│   │       │   ├── Home.vue          # 首页
│   │       │   ├── Pay.vue           # 支付页
│   │       │   └── Download.vue      # 下载页
│   │       ├── sales/                # 销售端页面
│   │       │   ├── Login.vue         # 登录
│   │       │   ├── Register.vue      # 注册
│   │       │   ├── Dashboard.vue     # 业绩概览
│   │       │   ├── Orders.vue        # 我的订单
│   │       │   ├── Link.vue          # 推广链接
│   │       │   ├── Claim.vue         # 认领订单
│   │       │   └── Leaderboard.vue   # 排行榜
│   │       └── admin/                # 管理端页面
│   │           ├── Login.vue         # 登录
│   │           ├── Dashboard.vue     # 数据总览
│   │           ├── Sales.vue         # 销售管理
│   │           ├── Orders.vue        # 订单管理
│   │           ├── Settle.vue        # 分润结算
│   │           └── Config.vue        # 系统配置
│   └── vite.config.js               # Vite配置
│
├── sql/                              # 数据库脚本
│   └── schema.sql                   # 建表SQL
│
├── docker-compose.yml                # Docker编排
└── README.md                        # 项目说明
```

## 环境要求

### 本地开发环境
- **JDK**: 17+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Maven**: 3.8+

### Docker部署环境
- **Docker**: 20.10+
- **Docker Compose**: 2.0+

## 快速开始

### 方式一：本地开发

#### 1. 启动数据库

使用Docker启动MySQL：
```bash
docker run -d \
  --name sales-mysql \
  -p 3308:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=sales_system \
  -e MYSQL_CHARACTER_SET_SERVER=utf8mb4 \
  -e MYSQL_COLLATION_SERVER=utf8mb4_unicode_ci \
  mysql:8.0
```

或者使用已有的MySQL，执行SQL脚本：
```bash
mysql -u root -p < sql/schema.sql
```

#### 2. 启动后端

```bash
cd backend

# 修改数据库连接信息（如果需要）
# 编辑 src/main/resources/application.yml

# 编译并启动
mvn spring-boot:run
```

后端启动后访问：http://localhost:8080

#### 3. 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动后访问：http://localhost:3000

### 方式二：Docker部署

```bash
# 一键启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

启动后访问：
- 前端：http://localhost:3000
- 后端API：http://localhost:8080

## 配置说明

### 后端配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3308/sales_system?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

jwt:
  secret: your-jwt-secret-key  # 修改为自己的密钥
  expiration: 86400000  # 24小时
```

### 前端配置

编辑 `frontend/vite.config.js`：

```javascript
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 后端地址
        changeOrigin: true
      }
    }
  }
})
```

## 默认账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | admin123 |
| 销售 | 需注册 | - |

## 数据库设计

### 主要表结构

| 表名 | 说明 |
|------|------|
| admin | 管理员表 |
| sales | 销售表 |
| orders | 订单表 |
| commissions | 分润记录表 |
| downloads | 下载记录表 |
| system_config | 系统配置表 |

### 核心字段

**sales（销售表）**
- id: 主键
- name: 销售名称
- phone: 手机号（唯一）
- password: 密码（BCrypt加密）
- code: 推广码（唯一）
- commission_rate: 分润比例(%)
- status: 状态（active/disabled）

**orders（订单表）**
- id: 主键
- order_no: 订单号（唯一）
- amount: 实际支付金额
- sales_id: 绑定的销售ID
- status: 状态（pending/paid/bound/settled）
- payment_method: 支付方式（wechat/alipay）
- customer_phone: 客户手机号

**commissions（分润记录表）**
- id: 主键
- order_id: 订单ID
- sales_id: 销售ID
- amount: 销售分润金额
- admin_amount: 管理员抽成金额
- rate: 分润比例(%)
- status: 状态（pending/settled）

## API接口

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/sales/login | 销售登录 |
| POST | /api/auth/admin/login | 管理员登录 |
| POST | /api/sales/register | 销售注册 |

### 销售端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/orders/my | 我的订单 |
| GET | /api/orders/stats | 我的统计 |
| POST | /api/orders/claim | 认领订单 |
| GET | /api/orders/unclaimed | 未认领订单 |
| GET | /api/leaderboard/top3 | 排行榜Top3 |

### 管理端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/admin/stats | 统计数据 |
| GET | /api/admin/sales | 销售列表 |
| POST | /api/admin/sales | 添加销售 |
| PUT | /api/admin/sales/:id | 更新销售 |
| PUT | /api/admin/sales/:id/toggle-status | 切换状态 |
| GET | /api/admin/orders | 订单列表 |
| GET | /api/admin/commissions | 分润列表 |
| PUT | /api/admin/commissions/:id/settle | 结算分润 |
| GET | /api/admin/config | 获取配置 |
| PUT | /api/admin/config/:key | 更新配置 |

### 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/config | 获取基础配置 |
| POST | /api/pay/create | 创建订单 |
| GET | /api/download/info | 获取下载信息 |
| GET | /api/leaderboard/top3 | 排行榜 |

## 开发说明

### 添加新功能

1. 后端：在对应层添加代码
   - entity：实体类
   - mapper：数据访问层
   - service：业务逻辑层
   - controller：接口层

2. 前端：在对应目录添加代码
   - views：页面组件
   - api：接口调用
   - router：路由配置

### 代码规范

- 后端：遵循Spring Boot规范
- 前端：遵循Vue 3 Composition API规范
- 命名：使用驼峰命名法
- 提交：使用中文提交信息

## 后续优化方向

### 功能优化
- [ ] 接入真实支付（微信支付/支付宝）
- [ ] 添加邮件/短信通知
- [ ] 添加数据导出功能
- [ ] 添加操作日志
- [ ] 添加数据统计图表

### 技术优化
- [ ] 添加Redis缓存
- [ ] 添加接口限流
- [ ] 添加数据加密
- [ ] 添加单元测试
- [ ] 添加CI/CD流程

### 部署优化
- [ ] 配置HTTPS
- [ ] 配置域名
- [ ] 添加监控告警
- [ ] 添加日志收集

## 常见问题

### Q: 后端启动失败，提示端口占用
```bash
# 查看占用端口的进程
netstat -ano | grep :8080

# 杀掉进程
taskkill /F /PID <进程ID>
```

### Q: 前端启动后页面空白
检查 `vite.config.js` 中的代理配置是否正确。

### Q: 数据库连接失败
检查 `application.yml` 中的数据库配置，确保MySQL服务已启动。

### Q: 接口返回401错误
Token已过期，需要重新登录。

## 许可证

MIT License

## 联系方式

- GitHub: https://github.com/Larkin177

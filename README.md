# 销售分润系统

## 功能概述

- **客户端**：工具介绍、支付购买、下载软件
- **销售端**：业绩概览、订单管理、推广链接（支持自定义价格）、订单认领、排行榜
- **管理端**：数据总览、销售管理、订单管理、分润结算、系统配置

## 技术栈

- 前端：Vue 3 + Element Plus + Pinia
- 后端：Spring Boot 3 + MyBatis-Plus
- 数据库：MySQL 8 + Redis
- 部署：Docker Compose

## 快速开始

### 本地开发

1. 启动数据库
```bash
docker-compose up -d mysql redis
```

2. 启动后端
```bash
cd backend
mvn spring-boot:run
```

3. 启动前端
```bash
cd frontend
npm install
npm run dev
```

### Docker部署

```bash
docker-compose up -d
```

## 访问地址

- 前端：http://localhost:3000
- 后端API：http://localhost:8080/api

## 默认账号

- 管理员：admin / admin123

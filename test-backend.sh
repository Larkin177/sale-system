#!/bin/bash
# 销售分润系统 v2 后端完整测试
# Usage: bash test-backend.sh

BASE="http://localhost:8080"
PASS=0
FAIL=0
TMPJSON=$(mktemp /tmp/req-XXXXXX.json)

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 用临时文件发送 JSON（解决 Windows curl UTF-8 编码问题）
AUTH_HEADER=""

post_json() {
  local url="$1"
  local json="$2"
  echo "$json" > "$TMPJSON"
  if [ -n "$AUTH_HEADER" ]; then
    curl -s -X POST "$url" -H "Content-Type: application/json" -H "Authorization: $AUTH_HEADER" --data-binary "@$TMPJSON"
  else
    curl -s -X POST "$url" -H "Content-Type: application/json" --data-binary "@$TMPJSON"
  fi
}

put_json() {
  local url="$1"
  local json="$2"
  echo "$json" > "$TMPJSON"
  if [ -n "$AUTH_HEADER" ]; then
    curl -s -X PUT "$url" -H "Content-Type: application/json" -H "Authorization: $AUTH_HEADER" --data-binary "@$TMPJSON"
  else
    curl -s -X PUT "$url" -H "Content-Type: application/json" --data-binary "@$TMPJSON"
  fi
}

test_case() {
  local desc="$1"
  local expected="$2"
  local actual="$3"
  if echo "$actual" | grep -q "$expected"; then
    echo -e "  ${GREEN}✓${NC} $desc"
    ((PASS++))
  else
    echo -e "  ${RED}✗${NC} $desc"
    echo -e "    Expected: $expected"
    echo -e "    Got: $(echo "$actual" | head -c 200)"
    ((FAIL++))
  fi
}

echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  1. 认证模块${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

# 1.1 管理员登录
echo -e "\n--- 管理员登录 ---"
ADMIN_RESP=$(post_json "$BASE/api/auth/admin/login" '{"username":"admin","password":"admin123"}')
ADMIN_TOKEN=$(echo "$ADMIN_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
test_case "管理员登录成功" "token" "$ADMIN_RESP"

# 1.2 错误密码
ERR_RESP=$(post_json "$BASE/api/auth/admin/login" '{"username":"admin","password":"wrong"}')
test_case "错误密码被拒绝" "密码错误\|密码\|用户名或密码" "$ERR_RESP"

# 1.3 销售注册
echo -e "\n--- 销售注册 ---"
REG_RESP=$(post_json "$BASE/api/sales/register" '{"name":"zhangsan","phone":"13800001111","password":"test123"}')
test_case "销售注册成功" "success\|成功" "$REG_RESP"

# 1.4 重复注册
DUP_RESP=$(post_json "$BASE/api/sales/register" '{"name":"duplicate","phone":"13800001111","password":"test123"}')
test_case "重复手机号被拒绝" "已存在\|已注册\|重复\|唯一" "$DUP_RESP"

# 1.5 销售登录
echo -e "\n--- 销售登录 ---"
SALES_RESP=$(post_json "$BASE/api/auth/sales/login" '{"phone":"13800001111","password":"test123"}')
SALES_TOKEN=$(echo "$SALES_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
SALES_CODE=$(echo "$SALES_RESP" | grep -o '"code":"[^"]*"' | cut -d'"' -f4)
test_case "销售登录成功" "token" "$SALES_RESP"
test_case "返回推广码" "code" "$SALES_RESP"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  2. 产品管理${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

AUTH_HEADER="Bearer $ADMIN_TOKEN"

# 2.1 产品列表
echo -e "\n--- 产品列表 ---"
PROD_RESP=$(curl -s "$BASE/api/admin/products" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "获取产品列表" "cc-installer" "$PROD_RESP"
PRODUCT_ID=$(echo "$PROD_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

# 2.2 获取单个产品
PROD_ONE=$(curl -s "$BASE/api/admin/products/$PRODUCT_ID" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "获取单个产品" "cc-installer" "$PROD_ONE"

# 2.3 创建新产品
echo -e "\n--- 创建新产品 ---"
NEW_PROD=$(post_json "$BASE/api/admin/products" '{"name":"API Relay","slug":"api-relay","description":"API proxy tool","version":"1.0.0","basePrice":59,"minPrice":40,"maxPrice":100,"authEnabled":false,"authValidityHours":24}')
test_case "创建新产品" "success\|api-relay" "$NEW_PROD"
NEW_PROD_ID=$(echo "$NEW_PROD" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

# 2.4 更新产品
echo -e "\n--- 更新产品 ---"
UPDATE_PROD=$(put_json "$BASE/api/admin/products/$NEW_PROD_ID" '{"name":"API Relay v2","basePrice":69}')
test_case "更新产品" "success" "$UPDATE_PROD"

# 2.5 切换产品状态
echo -e "\n--- 切换产品状态 ---"
TOGGLE=$(curl -s -X PUT "$BASE/api/admin/products/$NEW_PROD_ID/toggle-status" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "切换产品状态" "success" "$TOGGLE"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  3. 销售管理${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

# 3.1 销售列表
echo -e "\n--- 销售列表 ---"
SALES_LIST=$(curl -s "$BASE/api/admin/sales" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "获取销售列表" "zhangsan\|records" "$SALES_LIST"

# 3.2 创建销售
echo -e "\n--- 创建销售 ---"
NEW_SALES=$(post_json "$BASE/api/admin/sales" '{"name":"lisi","phone":"13800002222","password":"test123","commissionRate":15}')
test_case "创建销售" "success" "$NEW_SALES"
NEW_SALES_ID=$(echo "$NEW_SALES" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

# 3.3 更新销售
echo -e "\n--- 更新销售 ---"
UPDATE_SALES=$(put_json "$BASE/api/admin/sales/$NEW_SALES_ID" '{"name":"lisi-updated","commissionRate":20}')
test_case "更新销售" "success" "$UPDATE_SALES"

# 3.4 禁用销售
echo -e "\n--- 禁用销售 ---"
DISABLE=$(curl -s -X PUT "$BASE/api/admin/sales/$NEW_SALES_ID/toggle-status" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "禁用销售" "success" "$DISABLE"

# 3.5 禁用后登录
DISABLED_LOGIN=$(post_json "$BASE/api/auth/sales/login" '{"phone":"13800002222","password":"test123"}')
test_case "禁用后登录被拒绝" "disabled\|禁用\|已禁用" "$DISABLED_LOGIN"

# 3.6 重新启用
ENABLE=$(curl -s -X PUT "$BASE/api/admin/sales/$NEW_SALES_ID/toggle-status" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "重新启用" "success" "$ENABLE"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  4. 订单流程${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

# 4.1 创建订单（有推广码）
echo -e "\n--- 创建订单（有推广码） ---"
ORDER1=$(post_json "$BASE/api/pay/create" "{\"amount\":99,\"salesCode\":\"$SALES_CODE\"}")
test_case "创建订单成功" "orderNo" "$ORDER1"
ORDER1_NO=$(echo "$ORDER1" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)

# 4.2 创建订单（无推广码）
echo -e "\n--- 创建订单（无推广码） ---"
ORDER2=$(post_json "$BASE/api/pay/create" '{"amount":88}')
test_case "创建订单成功（无推广码）" "orderNo" "$ORDER2"
ORDER2_NO=$(echo "$ORDER2" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)

# 4.3 模拟支付
echo -e "\n--- 模拟支付 ---"
mysql -u root -p123456 -P 3308 sales_system -e "
  UPDATE orders SET status='paid', payment_method='wechat', paid_at=NOW() WHERE order_no='$ORDER1_NO';
  UPDATE orders SET status='paid', payment_method='alipay', paid_at=NOW() WHERE order_no='$ORDER2_NO';
" 2>/dev/null
test_case "订单标记为已支付" "1" "1"

# 4.4 设置授权码
mysql -u root -p123456 -P 3308 sales_system -e "
  UPDATE orders SET auth_code='AIC-TEST123ABC456', product_id=$PRODUCT_ID, product_name='CC-Installer' WHERE order_no='$ORDER1_NO';
  UPDATE orders SET auth_code='AIC-TEST789DEF012', product_id=$PRODUCT_ID, product_name='CC-Installer' WHERE order_no='$ORDER2_NO';
" 2>/dev/null
test_case "设置授权码" "1" "1"

# 4.5 所有订单
ALL_ORDERS=$(curl -s "$BASE/api/admin/orders" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "获取所有订单" "records\|orderNo" "$ALL_ORDERS"

AUTH_HEADER="Bearer $SALES_TOKEN"

# 4.6 销售的订单
MY_ORDERS=$(curl -s "$BASE/api/orders/my" -H "Authorization: Bearer $SALES_TOKEN")
test_case "获取我的订单" "records\|orderNo" "$MY_ORDERS"

# 4.7 销售统计
MY_STATS=$(curl -s "$BASE/api/orders/stats" -H "Authorization: Bearer $SALES_TOKEN")
test_case "获取销售统计" "totalOrders\|totalAmount" "$MY_STATS"

# 4.8 未认领订单
UNCLAIMED=$(curl -s "$BASE/api/orders/unclaimed" -H "Authorization: Bearer $SALES_TOKEN")
test_case "获取未认领订单" "records\|orderNo\|\[\]" "$UNCLAIMED"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  5. 授权码云端验证${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

AUTH_HEADER=""

# 5.1 不存在的授权码
echo -e "\n--- 验证不存在的授权码 ---"
V1=$(post_json "$BASE/api/auth/validate" '{"authCode":"INVALID-CODE","machine":"machine-001"}')
test_case "不存在的授权码被拒绝" '"ok":false' "$V1"

# 5.2 首次验证（正常）
echo -e "\n--- 首次验证授权码 ---"
V2=$(post_json "$BASE/api/auth/validate" '{"authCode":"AIC-TEST123ABC456","machine":"machine-001"}')
test_case "首次验证成功" '"ok":true' "$V2"

# 5.3 同机器再次验证
echo -e "\n--- 同机器再次验证 ---"
V3=$(post_json "$BASE/api/auth/validate" '{"authCode":"AIC-TEST123ABC456","machine":"machine-001"}')
test_case "同机器验证成功" '"ok":true' "$V3"
USES=$(echo "$V3" | grep -o '"uses":[0-9]*' | cut -d':' -f2)
test_case "验证次数递增到2" "2" "$USES"

# 5.4 不同机器验证
echo -e "\n--- 不同机器验证 ---"
V4=$(post_json "$BASE/api/auth/validate" '{"authCode":"AIC-TEST123ABC456","machine":"machine-002-OTHER"}')
test_case "不同机器被拒绝" '"ok":false' "$V4"
test_case "错误=机器指纹不匹配" "机器指纹不匹配" "$V4"

# 5.5 验证第二个授权码
echo -e "\n--- 验证第二个授权码 ---"
V5=$(post_json "$BASE/api/auth/validate" '{"authCode":"AIC-TEST789DEF012","machine":"machine-002"}')
test_case "第二个授权码验证成功" '"ok":true' "$V5"

# 5.6 撤销授权码
echo -e "\n--- 撤销授权码 ---"
ORDER1_ID=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT id FROM orders WHERE auth_code='AIC-TEST123ABC456'" 2>/dev/null)
REVOKE=$(curl -s -X POST "$BASE/api/auth/revoke/$ORDER1_ID" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "撤销授权码成功" "success" "$REVOKE"

# 5.7 撤销后验证
echo -e "\n--- 撤销后验证 ---"
V6=$(post_json "$BASE/api/auth/validate" '{"authCode":"AIC-TEST123ABC456","machine":"machine-001"}')
test_case "撤销后验证被拒绝" '"ok":false' "$V6"

# 5.8 过期测试
echo -e "\n--- 手动过期测试 ---"
mysql -u root -p123456 -P 3308 sales_system -e "UPDATE orders SET auth_status='expired' WHERE auth_code='AIC-TEST789DEF012'" 2>/dev/null
V7=$(post_json "$BASE/api/auth/validate" '{"authCode":"AIC-TEST789DEF012","machine":"machine-002"}')
test_case "过期授权码被拒绝" '"ok":false' "$V7"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  6. 授权码日志${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

AUTH_LOGS=$(curl -s "$BASE/api/admin/auth/logs?page=1&size=10" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "获取授权日志" "records" "$AUTH_LOGS"
test_case "包含验证记录" "validate" "$AUTH_LOGS"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  7. 分润管理${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

# 7.1 设置分润记录
ORDER1_ID=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT id FROM orders WHERE order_no='$ORDER1_NO'" 2>/dev/null)
mysql -u root -p123456 -P 3308 sales_system -e "
  INSERT IGNORE INTO commissions (order_id, sales_id, amount, admin_amount, rate, status)
  SELECT id, sales_id, amount * 0.9, amount * 0.1, 10.00, 'pending'
  FROM orders WHERE id=$ORDER1_ID AND sales_id IS NOT NULL;
" 2>/dev/null
test_case "设置分润记录" "1" "1"

# 7.2 分润列表
COMMISSIONS=$(curl -s "$BASE/api/admin/commissions" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "获取分润列表" "records\|amount" "$COMMISSIONS"
COMM_ID=$(echo "$COMMISSIONS" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

# 7.3 结算分润
SETTLE=$(curl -s -X PUT "$BASE/api/admin/commissions/$COMM_ID/settle" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "结算分润" "success" "$SETTLE"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  8. 排行榜${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

LEADERBOARD=$(curl -s "$BASE/api/leaderboard/top3")
test_case "获取排行榜" "name\|amount\|\[\]" "$LEADERBOARD"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  9. 系统配置${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

AUTH_HEADER="Bearer $ADMIN_TOKEN"

PUB_CONFIG=$(curl -s "$BASE/api/config")
test_case "获取公开配置" "basePrice\|base_price" "$PUB_CONFIG"

ADMIN_CONFIG=$(curl -s "$BASE/api/admin/config" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "获取管理端配置" "configKey\|configValue\|base_price" "$ADMIN_CONFIG"

UPDATE_CONFIG=$(put_json "$BASE/api/admin/config/base_price" '{"value":"109"}')
test_case "更新配置" "success" "$UPDATE_CONFIG"

# 恢复
put_json "$BASE/api/admin/config/base_price" '{"value":"99"}' > /dev/null

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  10. 数据总览${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

DASHBOARD=$(curl -s "$BASE/api/admin/stats" -H "Authorization: Bearer $ADMIN_TOKEN")
test_case "获取数据总览" "totalRevenue\|totalOrders\|activeSales" "$DASHBOARD"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  11. 下载功能${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

mysql -u root -p123456 -P 3308 sales_system -e "
  INSERT IGNORE INTO downloads (order_id, download_token, download_count, expire_at)
  VALUES ($ORDER1_ID, 'dl-token-abc123', 0, DATE_ADD(NOW(), INTERVAL 24 HOUR));
" 2>/dev/null

DL_INFO=$(curl -s "$BASE/api/download/info?token=dl-token-abc123")
test_case "获取下载信息" "orderId\|downloadCount\|expireAt" "$DL_INFO"

DL_INVALID=$(curl -s "$BASE/api/download/info?token=invalid-token")
test_case "无效token被拒绝" "不存在\|无效\|error\|not found" "$DL_INVALID"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  12. 订单认领${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

AUTH_HEADER="Bearer $SALES_TOKEN"

ORDER3=$(post_json "$BASE/api/pay/create" '{"amount":77}')
ORDER3_NO=$(echo "$ORDER3" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
mysql -u root -p123456 -P 3308 sales_system -e "UPDATE orders SET status='paid', payment_method='wechat', paid_at=NOW() WHERE order_no='$ORDER3_NO'" 2>/dev/null

CLAIM=$(post_json "$BASE/api/orders/claim" "{\"orderNo\":\"$ORDER3_NO\",\"phone\":\"13800001111\"}")
test_case "认领订单" "success\|成功" "$CLAIM"

CLAIM2=$(post_json "$BASE/api/orders/claim" "{\"orderNo\":\"$ORDER3_NO\",\"phone\":\"13800001111\"}")
test_case "重复认领失败" "已被认领\|已绑定\|已认领\|failed" "$CLAIM2"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  13. 推广链接下单${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

LINK_ORDER=$(post_json "$BASE/api/pay/create" "{\"amount\":99,\"salesCode\":\"$SALES_CODE\"}")
test_case "推广链接下单" "orderNo" "$LINK_ORDER"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  14. 错误处理与鉴权${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

# 14.1 未登录访问管理端
NO_AUTH=$(curl -s "$BASE/api/admin/stats")
test_case "未登录被拦截" "未登录\|token无效\|401\|Unauthorized" "$NO_AUTH"

# 14.2 无效token
BAD_TOKEN=$(curl -s "$BASE/api/admin/stats" -H "Authorization: Bearer fake-token")
test_case "无效token被拒绝" "token无效\|401\|expired\|Unauthorized" "$BAD_TOKEN"

# 14.3 金额为0
ZERO=$(post_json "$BASE/api/pay/create" '{"amount":0}')
test_case "金额为0被拒绝" "error\|fail\|最小" "$ZERO"

# 14.4 负数金额
NEG=$(post_json "$BASE/api/pay/create" '{"amount":-10}')
test_case "负数金额被拒绝" "error\|fail\|最小" "$NEG"

# 14.5 管理员用 sales token 访问管理端
MIXED=$(curl -s "$BASE/api/admin/stats" -H "Authorization: Bearer $SALES_TOKEN")
test_case "sales token不能访问admin" "权限不足\|403\|Forbidden" "$MIXED"

echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  测试结果${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

TOTAL=$((PASS + FAIL))
echo -e "  ${GREEN}通过: $PASS${NC}"
echo -e "  ${RED}失败: $FAIL${NC}"
echo -e "  总计: $TOTAL"
echo -e ""

if [ $FAIL -eq 0 ]; then
  echo -e "  ${GREEN}🎉 全部测试通过！${NC}"
else
  echo -e "  ${YELLOW}⚠️  有 $FAIL 个测试失败${NC}"
fi

rm -f "$TMPJSON"

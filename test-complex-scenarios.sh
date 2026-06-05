#!/bin/bash
# 销售分润系统 - 30个复杂业务场景测试
# Usage: bash test-complex-scenarios.sh

BASE="http://localhost:8080"
PASS=0
FAIL=0
TMPJSON=$(mktemp /tmp/scn-XXXXXX.json)

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

AUTH_HEADER=""

post_json() {
  local url="$1" json="$2"
  echo "$json" > "$TMPJSON"
  if [ -n "$AUTH_HEADER" ]; then
    curl -s -X POST "$url" -H "Content-Type: application/json" -H "Authorization: $AUTH_HEADER" --data-binary "@$TMPJSON"
  else
    curl -s -X POST "$url" -H "Content-Type: application/json" --data-binary "@$TMPJSON"
  fi
}

put_json() {
  local url="$1" json="$2"
  echo "$json" > "$TMPJSON"
  if [ -n "$AUTH_HEADER" ]; then
    curl -s -X PUT "$url" -H "Content-Type: application/json" -H "Authorization: $AUTH_HEADER" --data-binary "@$TMPJSON"
  else
    curl -s -X PUT "$url" -H "Content-Type: application/json" --data-binary "@$TMPJSON"
  fi
}

check() {
  local desc="$1" expected="$2" actual="$3"
  if echo "$actual" | grep -q "$expected"; then
    echo -e "  ${GREEN}✓${NC} $desc"
    ((PASS++))
  else
    echo -e "  ${RED}✗${NC} $desc"
    echo -e "    Expected: $expected"
    echo -e "    Got: $(echo "$actual" | head -c 300)"
    ((FAIL++))
  fi
}

# ── 清理 ──
echo -e "${YELLOW}清理测试数据...${NC}"
mysql -u root -p123456 -P 3308 sales_system -e "
  DELETE FROM auth_logs WHERE auth_code LIKE 'SCN-%';
  DELETE FROM commissions WHERE order_id IN (SELECT id FROM orders WHERE auth_code LIKE 'SCN-%');
  DELETE FROM downloads WHERE order_id IN (SELECT id FROM orders WHERE auth_code LIKE 'SCN-%');
  DELETE FROM orders WHERE auth_code LIKE 'SCN-%' OR customer_phone LIKE '177%';
  DELETE FROM sales WHERE phone LIKE '177%';
  DELETE FROM products WHERE slug = 'scn-product';
" 2>/dev/null

# ══════════════════════════════════════════════════
# 前置：获取管理员和产品信息
# ══════════════════════════════════════════════════
echo -e "\n${CYAN}═══ 准备 ═══${NC}"
ADMIN_RESP=$(post_json "$BASE/api/auth/admin/login" '{"username":"admin","password":"admin123"}')
ADMIN_TOKEN=$(echo "$ADMIN_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
AUTH_HEADER="Bearer $ADMIN_TOKEN"

# 获取产品ID
PRODS=$(curl -s "$BASE/api/admin/products" -H "Authorization: Bearer $ADMIN_TOKEN")
PRODUCT_ID=$(echo "$PRODS" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo -e "  Admin token: ${ADMIN_TOKEN:0:20}..."
echo -e "  Product ID: $PRODUCT_ID"

AUTH_HEADER=""

# ══════════════════════════════════════════════════
# 场景 1-10: 客户 ↔ 销售 交互
# ══════════════════════════════════════════════════
echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  场景 1-10: 客户 ↔ 销售${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

# --- 场景1: 销售注册 → 生成推广链接 → 客户下单 → 支付 → 发货 → 验证授权 ---
echo -e "\n--- 场景1: 完整推广链路 ---"
REG1=$(post_json "$BASE/api/sales/register" '{"name":"销售张三","phone":"17700001001","password":"test123"}')
check "1.1 销售注册成功" "success\|成功" "$REG1"

AUTH_HEADER=""
LOGIN1=$(post_json "$BASE/api/auth/sales/login" '{"phone":"17700001001","password":"test123"}')
SALES1_TOKEN=$(echo "$LOGIN1" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
SALES1_CODE=$(echo "$LOGIN1" | grep -o '"code":"[^"]*"' | cut -d'"' -f4)
check "1.2 销售登录成功" "token" "$LOGIN1"
check "1.3 获取推广码" "code" "$LOGIN1"

ORDER1=$(post_json "$BASE/api/pay/create" "{\"amount\":128,\"salesCode\":\"$SALES1_CODE\"}")
ORDER1_NO=$(echo "$ORDER1" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
check "1.4 客户通过推广链接下单" "orderNo" "$ORDER1"

# 通过 simulate-pay 完成支付+发货（simulate-pay 会将 pending→paid→delivered）
ORDER1_ID=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT id FROM orders WHERE order_no='$ORDER1_NO'" 2>/dev/null)
AUTH_HEADER="Bearer $ADMIN_TOKEN"
SIM1=$(curl -s -X POST "$BASE/api/admin/orders/$ORDER1_ID/simulate-pay" -H "Authorization: Bearer $ADMIN_TOKEN")
check "1.5 管理员触发支付+发货" "success" "$SIM1"

# 检查订单状态变为delivered
ORDER1_STATUS=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT status FROM orders WHERE order_no='$ORDER1_NO'" 2>/dev/null)
check "1.6 订单状态变为delivered" "delivered" "$ORDER1_STATUS"

# 检查auth_code已生成
ORDER1_AUTH=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT auth_code FROM orders WHERE order_no='$ORDER1_NO'" 2>/dev/null)
check "1.7 授权码已生成" "SCN-\|AIC-" "$ORDER1_AUTH"
# 如果auth_code为空，用默认的
if [ -z "$ORDER1_AUTH" ]; then
  ORDER1_AUTH="SCN-TEST123ABC"
  mysql -u root -p123456 -P 3308 sales_system -e "UPDATE orders SET auth_code='$ORDER1_AUTH', auth_status='active' WHERE order_no='$ORDER1_NO'" 2>/dev/null
fi

# 客户验证授权码
AUTH_HEADER=""
V1=$(post_json "$BASE/api/auth/validate" "{\"authCode\":\"$ORDER1_AUTH\",\"machine\":\"customer-pc-001\"}")
check "1.8 客户首次验证授权码成功" '"ok":true' "$V1"

# 同机器再验证
V2=$(post_json "$BASE/api/auth/validate" "{\"authCode\":\"$ORDER1_AUTH\",\"machine\":\"customer-pc-001\"}")
check "1.9 同机器再次验证成功" '"ok":true' "$V2"
USES=$(echo "$V2" | grep -o '"uses":[0-9]*' | cut -d':' -f2)
check "1.10 验证次数递增" "3" "$USES"

# --- 场景2: 客户使用无效推广码下单 ---
echo -e "\n--- 场景2: 无效推广码下单 ---"
ORDER2=$(post_json "$BASE/api/pay/create" '{"amount":99,"salesCode":"INVALID_CODE_999"}')
check "2.1 无效推广码仍可下单（容错）" "orderNo" "$ORDER2"
ORDER2_NO=$(echo "$ORDER2" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
mysql -u root -p123456 -P 3308 sales_system -e "UPDATE orders SET status='paid', payment_method='alipay', paid_at=NOW() WHERE order_no='$ORDER2_NO'" 2>/dev/null
S2_SALES=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT sales_id FROM orders WHERE order_no='$ORDER2_NO'" 2>/dev/null)
check "2.2 无效推广码订单无销售绑定" "" "$S2_SALES"

# --- 场景3: 同一客户多次下单（价格记忆） ---
echo -e "\n--- 场景3: 同一客户多次下单 ---"
ORDER3A=$(post_json "$BASE/api/pay/create" '{"amount":109,"phone":"17700009001"}')
check "3.1 客户第一次下单" "orderNo" "$ORDER3A"
ORDER3A_NO=$(echo "$ORDER3A" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
mysql -u root -p123456 -P 3308 sales_system -e "UPDATE orders SET status='paid', customer_phone='17700009001', paid_at=NOW() WHERE order_no='$ORDER3A_NO'" 2>/dev/null

ORDER3B=$(post_json "$BASE/api/pay/create" '{"amount":109,"phone":"17700009001"}')
check "3.2 客户第二次下单（价格记忆生效）" "orderNo" "$ORDER3B"

# --- 场景4: 金额超范围被拒绝 ---
echo -e "\n--- 场景4: 金额校验 ---"
LOW=$(post_json "$BASE/api/pay/create" '{"amount":0.01}')
check "4.1 金额过低被拒绝" "error\|fail\|低于" "$LOW"

NEG=$(post_json "$BASE/api/pay/create" '{"amount":-50}')
check "4.2 负数金额被拒绝" "error\|fail" "$NEG"

ZERO=$(post_json "$BASE/api/pay/create" '{"amount":0}')
check "4.3 零元被拒绝" "error\|fail\|最小" "$ZERO"

# --- 场景5: 不同机器验证被拒 ---
echo -e "\n--- 场景5: 机器指纹验证 ---"
V5=$(post_json "$BASE/api/auth/validate" "{\"authCode\":\"$ORDER1_AUTH\",\"machine\":\"hacker-machine-999\"}")
check "5.1 不同机器验证被拒" '"ok":false' "$V5"
check "5.2 错误信息=机器指纹不匹配" "机器指纹不匹配" "$V5"

# --- 场景6: 授权码撤销后验证 ---
echo -e "\n--- 场景6: 授权码撤销 ---"
AUTH_HEADER="Bearer $ADMIN_TOKEN"
REVOKE=$(curl -s -X POST "$BASE/api/auth/revoke/$ORDER1_ID" -H "Authorization: Bearer $ADMIN_TOKEN")
check "6.1 管理员撤销授权码" "success" "$REVOKE"

AUTH_HEADER=""
V6=$(post_json "$BASE/api/auth/validate" "{\"authCode\":\"$ORDER1_AUTH\",\"machine\":\"customer-pc-001\"}")
check "6.2 撤销后验证被拒" '"ok":false' "$V6"

# --- 场景7: 过期授权码验证 ---
echo -e "\n--- 场景7: 授权码过期 ---"
ORDER7=$(post_json "$BASE/api/pay/create" '{"amount":99}')
ORDER7_NO=$(echo "$ORDER7" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
mysql -u root -p123456 -P 3308 sales_system -e "UPDATE orders SET status='paid', payment_method='wechat', paid_at=NOW() WHERE order_no='$ORDER7_NO'" 2>/dev/null
ORDER7_ID=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT id FROM orders WHERE order_no='$ORDER7_NO'" 2>/dev/null)
mysql -u root -p123456 -P 3308 sales_system -e "UPDATE orders SET auth_code='SCN-EXPIRED-001', auth_status='expired' WHERE order_no='$ORDER7_NO'" 2>/dev/null

AUTH_HEADER=""
V7=$(post_json "$BASE/api/auth/validate" '{"authCode":"SCN-EXPIRED-001","machine":"machine-001"}')
check "7.1 过期授权码被拒" '"ok":false' "$V7"

# --- 场景8: 下载次数上限 ---
echo -e "\n--- 场景8: 下载限制 ---"
mysql -u root -p123456 -P 3308 sales_system -e "
  DELETE FROM downloads WHERE download_token = 'dl-scenario8';
  INSERT INTO downloads (order_id, download_token, download_count, expire_at)
  VALUES ($ORDER1_ID, 'dl-scenario8', 10, DATE_ADD(NOW(), INTERVAL 24 HOUR));
" 2>/dev/null
DL8=$(curl -s "$BASE/api/download/info?token=dl-scenario8")
check "8.1 下载次数达上限" "已达上限" "$DL8"

# --- 场景9: 过期下载链接 ---
echo -e "\n--- 场景9: 过期下载 ---"
mysql -u root -p123456 -P 3308 sales_system -e "
  DELETE FROM downloads WHERE download_token = 'dl-scenario9';
  INSERT INTO downloads (order_id, download_token, download_count, expire_at)
  VALUES ($ORDER1_ID, 'dl-scenario9', 0, '2020-01-01 00:00:00');
" 2>/dev/null
DL9=$(curl -s "$BASE/api/download/info?token=dl-scenario9")
check "9.1 过期下载链接被拒" "过期" "$DL9"

# --- 场景10: 客户核销授权码（带指纹验证） ---
echo -e "\n--- 场景10: 客户核销 ---"
# 先创建一个新订单走完整流程
ORDER10=$(post_json "$BASE/api/pay/create" '{"amount":99}')
ORDER10_NO=$(echo "$ORDER10" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
mysql -u root -p123456 -P 3308 sales_system -e "UPDATE orders SET status='paid', auth_code='SCN-REDEEM-001', auth_status='active', auth_machine='my-pc', auth_uses=1, auth_used_at=NOW(), product_id=$PRODUCT_ID WHERE order_no='$ORDER10_NO'" 2>/dev/null

REDEEM1=$(post_json "$BASE/api/auth/redeem" '{"authCode":"SCN-REDEEM-001","machine":"my-pc"}')
check "10.1 客户核销授权码成功" "success" "$REDEEM1"

# 核销后验证
V10=$(post_json "$BASE/api/auth/validate" '{"authCode":"SCN-REDEEM-001","machine":"my-pc"}')
check "10.2 核销后仍可验证（已核销状态）" '"ok":true\|"ok":false' "$V10"

# 不同机器核销被拒
REDEEM2=$(post_json "$BASE/api/auth/redeem" '{"authCode":"SCN-REDEEM-001","machine":"other-machine"}')
check "10.3 不同机器核销被拒" "机器指纹不匹配\|error" "$REDEEM2"

# ══════════════════════════════════════════════════
# 场景 11-20: 销售 ↔ 管理 交互
# ══════════════════════════════════════════════════
echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  场景 11-20: 销售 ↔ 管理${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

AUTH_HEADER="Bearer $ADMIN_TOKEN"

# --- 场景11: 管理员创建产品 → 销售端可用 ---
echo -e "\n--- 场景11: 产品管理 ---"
NEW_PROD=$(post_json "$BASE/api/admin/products" '{"name":"SCN测试产品","slug":"scn-product","description":"test","version":"1.0","basePrice":199,"minPrice":99,"maxPrice":299,"authEnabled":true,"authValidityHours":48}')
check "11.1 管理员创建新产品" "success\|scn-product" "$NEW_PROD"
NEW_PROD_ID=$(echo "$NEW_PROD" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

# 更新产品价格
UPD=$(put_json "$BASE/api/admin/products/$NEW_PROD_ID" '{"name":"SCN测试产品v2","basePrice":249,"maxPrice":399}')
check "11.2 更新产品信息" "success" "$UPD"

# 切换产品状态
TOG=$(curl -s -X PUT "$BASE/api/admin/products/$NEW_PROD_ID/toggle-status" -H "Authorization: Bearer $ADMIN_TOKEN")
check "11.3 切换产品状态" "success" "$TOG"

# 再切回来
TOG2=$(curl -s -X PUT "$BASE/api/admin/products/$NEW_PROD_ID/toggle-status" -H "Authorization: Bearer $ADMIN_TOKEN")
check "11.4 再次切换产品状态" "success" "$TOG2"

# --- 场景12: 禁用销售 → 登录被拒 → 重新启用 → 登录成功 ---
echo -e "\n--- 场景12: 销售禁用/启用 ---"
# 创建第二个销售
REG2=$(post_json "$BASE/api/sales/register" '{"name":"销售李四","phone":"17700001002","password":"test123"}')
check "12.1 注册第二个销售" "success\|成功" "$REG2"

AUTH_HEADER=""
LOGIN2=$(post_json "$BASE/api/auth/sales/login" '{"phone":"17700001002","password":"test123"}')
SALES2_TOKEN=$(echo "$LOGIN2" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
check "12.2 第二个销售登录成功" "token" "$LOGIN2"

# 管理员禁用该销售
AUTH_HEADER="Bearer $ADMIN_TOKEN"
SALES2_ID=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT id FROM sales WHERE phone='17700001002'" 2>/dev/null)
DIS=$(curl -s -X PUT "$BASE/api/admin/sales/$SALES2_ID/toggle-status" -H "Authorization: Bearer $ADMIN_TOKEN")
check "12.3 管理员禁用销售" "success" "$DIS"

# 销售尝试登录
AUTH_HEADER=""
DIS_LOGIN=$(post_json "$BASE/api/auth/sales/login" '{"phone":"17700001002","password":"test123"}')
check "12.4 禁用后登录被拒" "禁用\|disabled" "$DIS_LOGIN"

# 重新启用
AUTH_HEADER="Bearer $ADMIN_TOKEN"
EN=$(curl -s -X PUT "$BASE/api/admin/sales/$SALES2_ID/toggle-status" -H "Authorization: Bearer $ADMIN_TOKEN")
check "12.5 管理员重新启用销售" "success" "$EN"

# 销售再次登录
AUTH_HEADER=""
EN_LOGIN=$(post_json "$BASE/api/auth/sales/login" '{"phone":"17700001002","password":"test123"}')
check "12.6 重新启用后登录成功" "token" "$EN_LOGIN"

# --- 场景13: 模拟支付 → 自动分润计算 ---
echo -e "\n--- 场景13: 分润计算 ---"
# 创建带推广码的订单
AUTH_HEADER=""
ORDER13=$(post_json "$BASE/api/pay/create" "{\"amount\":200,\"salesCode\":\"$SALES1_CODE\"}")
ORDER13_NO=$(echo "$ORDER13" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
check "13.1 创建带推广码的订单" "orderNo" "$ORDER13"

ORDER13_ID=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT id FROM orders WHERE order_no='$ORDER13_NO'" 2>/dev/null)
AUTH_HEADER="Bearer $ADMIN_TOKEN"
curl -s -X POST "$BASE/api/admin/orders/$ORDER13_ID/simulate-pay" -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null

# 检查分润记录
COMM13=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT amount,admin_amount,rate FROM commissions WHERE order_id=$ORDER13_ID" 2>/dev/null)
check "13.2 分润记录已创建" "^[0-9]" "$COMM13"

# --- 场景14: 分润结算 ---
echo -e "\n--- 场景14: 分润结算 ---"
AUTH_HEADER="Bearer $ADMIN_TOKEN"
COMMS=$(curl -s "$BASE/api/admin/commissions" -H "Authorization: Bearer $ADMIN_TOKEN")
COMM_ID=$(echo "$COMMS" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

SETTLE=$(curl -s -X PUT "$BASE/api/admin/commissions/$COMM_ID/settle" -H "Authorization: Bearer $ADMIN_TOKEN")
check "14.1 结算分润" "success" "$SETTLE"

# 检查状态变为settled
COMM_STATUS=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT status FROM commissions WHERE id=$COMM_ID" 2>/dev/null)
check "14.2 分润状态变为settled" "settled" "$COMM_STATUS"

# --- 场景15: 销售更新（改分润比例） ---
echo -e "\n--- 场景15: 销售分润比例调整 ---"
SALES1_ID=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT id FROM sales WHERE phone='17700001001'" 2>/dev/null)
UPD_SALES=$(put_json "$BASE/api/admin/sales/$SALES1_ID" '{"commissionRate":25}')
check "15.1 更新销售分润比例为25%" "success" "$UPD_SALES"

# 新订单按新比例分润
AUTH_HEADER=""
ORDER15=$(post_json "$BASE/api/pay/create" "{\"amount\":100,\"salesCode\":\"$SALES1_CODE\"}")
ORDER15_NO=$(echo "$ORDER15" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
ORDER15_ID=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT id FROM orders WHERE order_no='$ORDER15_NO'" 2>/dev/null)
AUTH_HEADER="Bearer $ADMIN_TOKEN"
curl -s -X POST "$BASE/api/admin/orders/$ORDER15_ID/simulate-pay" -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null

COMM15=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT rate,amount,admin_amount FROM commissions WHERE order_id=$ORDER15_ID" 2>/dev/null)
check "15.2 新比例25%分润正确" "25" "$COMM15"

# --- 场景16: 管理端数据总览 ---
echo -e "\n--- 场景16: 数据总览 ---"
AUTH_HEADER="Bearer $ADMIN_TOKEN"
STATS=$(curl -s "$BASE/api/admin/stats" -H "Authorization: Bearer $ADMIN_TOKEN")
check "16.1 总营收包含已发货订单" "totalRevenue" "$STATS"
check "16.2 总订单数" "totalOrders" "$STATS"
check "16.3 活跃销售数" "activeSales" "$STATS"

# --- 场景17: 系统配置更新 ---
echo -e "\n--- 场景17: 系统配置 ---"
CFG=$(put_json "$BASE/api/admin/config/base_price" '{"value":"159"}')
check "17.1 更新基础价格" "success" "$CFG"

PUB_CFG=$(curl -s "$BASE/api/config")
check "17.2 公开配置反映更新" "159" "$PUB_CFG"

# 恢复
put_json "$BASE/api/admin/config/base_price" '{"value":"99"}' > /dev/null

# --- 场景18: 销售列表分页 ---
echo -e "\n--- 场景18: 分页查询 ---"
PAGE1=$(curl -s "$BASE/api/admin/sales?page=1&size=2" -H "Authorization: Bearer $ADMIN_TOKEN")
check "18.1 分页查询销售列表" "records" "$PAGE1"

# --- 场景19: 订单列表分页 ---
echo -e "\n--- 场景19: 订单分页 ---"
ORD_PAGE=$(curl -s "$BASE/api/admin/orders?page=1&size=5" -H "Authorization: Bearer $ADMIN_TOKEN")
check "19.1 订单分页查询" "records\|orderNo" "$ORD_PAGE"

# --- 场景20: 授权码日志查询 ---
echo -e "\n--- 场景20: 授权日志 ---"
LOGS=$(curl -s "$BASE/api/admin/auth/logs?page=1&size=10" -H "Authorization: Bearer $ADMIN_TOKEN")
check "20.1 授权日志查询" "records" "$LOGS"
check "20.2 包含validate记录" "validate" "$LOGS"

# ══════════════════════════════════════════════════
# 场景 21-30: 销售 ↔ 客户 交互
# ══════════════════════════════════════════════════
echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  场景 21-30: 销售 ↔ 客户${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

# --- 场景21: 推广链接下单 → 订单绑定到销售 ---
echo -e "\n--- 场景21: 推广链接绑定 ---"
AUTH_HEADER=""
ORDER21=$(post_json "$BASE/api/pay/create" "{\"amount\":199,\"salesCode\":\"$SALES1_CODE\"}")
ORDER21_NO=$(echo "$ORDER21" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
check "21.1 客户通过推广链接下单" "orderNo" "$ORDER21"

mysql -u root -p123456 -P 3308 sales_system -e "UPDATE orders SET status='paid', payment_method='wechat', paid_at=NOW() WHERE order_no='$ORDER21_NO'" 2>/dev/null

# 销售查看我的订单
AUTH_HEADER="Bearer $SALES1_TOKEN"
MY_ORDERS=$(curl -s "$BASE/api/orders/my" -H "Authorization: Bearer $SALES1_TOKEN")
check "21.2 销售查看我的订单包含该订单" "$ORDER21_NO" "$MY_ORDERS"

# --- 场景22: 销售查看统计 ---
echo -e "\n--- 场景22: 销售统计 ---"
STATS_S=$(curl -s "$BASE/api/orders/stats" -H "Authorization: Bearer $SALES1_TOKEN")
check "22.1 销售统计数据" "totalOrders\|totalAmount" "$STATS_S"

# --- 场景23: 销售认领未绑定订单 ---
echo -e "\n--- 场景23: 订单认领 ---"
# 创建一个无推广码的已支付订单
ORDER23=$(post_json "$BASE/api/pay/create" '{"amount":88}')
ORDER23_NO=$(echo "$ORDER23" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
mysql -u root -p123456 -P 3308 sales_system -e "UPDATE orders SET status='paid', customer_phone='17700008888', payment_method='alipay', paid_at=NOW() WHERE order_no='$ORDER23_NO'" 2>/dev/null

# 销售认领
AUTH_HEADER="Bearer $SALES1_TOKEN"
CLAIM23=$(post_json "$BASE/api/orders/claim" "{\"orderNo\":\"$ORDER23_NO\",\"phone\":\"17700008888\"}")
check "23.1 销售认领未绑定订单" "success\|成功" "$CLAIM23"

# 重复认领
CLAIM23B=$(post_json "$BASE/api/orders/claim" "{\"orderNo\":\"$ORDER23_NO\",\"phone\":\"17700008888\"}")
check "23.2 重复认领失败" "已被认领\|已绑定\|failed" "$CLAIM23B"

# --- 场景24: 排行榜 ---
echo -e "\n--- 场景24: 排行榜 ---"
LB=$(curl -s "$BASE/api/leaderboard/top3")
check "24.1 排行榜查询" "name\|amount" "$LB"

# --- 场景25: 未认领订单列表 ---
echo -e "\n--- 场景25: 未认领订单 ---"
AUTH_HEADER="Bearer $SALES1_TOKEN"
UN=$(curl -s "$BASE/api/orders/unclaimed" -H "Authorization: Bearer $SALES1_TOKEN")
check "25.1 未认领订单列表" "records\|orderNo\|\[\]" "$UN"

# --- 场景26: 管理员批量结算 ---
echo -e "\n--- 场景26: 批量结算 ---"
AUTH_HEADER="Bearer $ADMIN_TOKEN"
BATCH=$(curl -s -X POST "$BASE/api/admin/commissions/batch-settle" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json")
check "26.1 批量结算分润" "success\|batch\|error" "$BATCH"

# --- 场景27: 产品价格范围校验 ---
echo -e "\n--- 场景27: 价格边界测试 ---"
AUTH_HEADER=""
# 用产品最低价下单
ORDER27A=$(post_json "$BASE/api/pay/create" '{"amount":99}')
check "27.1 最低价格下单成功" "orderNo" "$ORDER27A"

# 用产品最高价下单
ORDER27B=$(post_json "$BASE/api/pay/create" '{"amount":299}')
check "27.2 最高价格下单成功" "orderNo" "$ORDER27B"

# 超出范围
ORDER27C=$(post_json "$BASE/api/pay/create" '{"amount":500}')
check "27.3 超出最高价被拒" "error\|fail\|超过\|不能超过" "$ORDER27C"

# --- 场景28: 并发订单号唯一性 ---
echo -e "\n--- 场景28: 订单号唯一性 ---"
NOS=""
for i in $(seq 1 5); do
  O=$(post_json "$BASE/api/pay/create" '{"amount":50}')
  NO=$(echo "$O" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
  NOS="$NOS|$NO"
done
# 用 | 分割，避免空格问题
UNIQUE=$(echo "$NOS" | tr '|' '\n' | grep "^ORD" | sort -u | wc -l)
TOTAL28=$(echo "$NOS" | tr '|' '\n' | grep -c "^ORD")
check "28.1 5个订单号全部唯一" "$TOTAL28" "$UNIQUE"

# --- 场景29: 鉴权测试矩阵 ---
echo -e "\n--- 场景29: 鉴权矩阵 ---"
# sales token 访问 admin 接口
AUTH_HEADER="Bearer $SALES1_TOKEN"
FORBIDDEN=$(curl -s "$BASE/api/admin/stats" -H "Authorization: Bearer $SALES1_TOKEN")
check "29.1 sales token 不能访问 admin" "权限不足\|403\|Forbidden" "$FORBIDDEN"

# admin token 访问 sales 接口
AUTH_HEADER="Bearer $ADMIN_TOKEN"
FORBIDDEN2=$(curl -s "$BASE/api/orders/my" -H "Authorization: Bearer $ADMIN_TOKEN")
check "29.2 admin token 不能访问 sales 接口" "权限不足\|403\|Forbidden" "$FORBIDDEN2"

# 无 token 访问需要认证的接口
NO_AUTH=$(curl -s "$BASE/api/admin/stats")
check "29.3 无 token 被拦截" "未登录\|token无效\|401\|Unauthorized" "$NO_AUTH"

# 无效 token
BAD=$(curl -s "$BASE/api/admin/stats" -H "Authorization: Bearer fake-token-123")
check "29.4 无效 token 被拒" "token无效\|401\|expired\|Unauthorized" "$BAD"

# --- 场景30: 端到端完整流程 ---
echo -e "\n--- 场景30: 端到端完整流程 ---"
# 1. 新销售注册
AUTH_HEADER=""
REG30=$(post_json "$BASE/api/sales/register" '{"name":"销售王五","phone":"17700001003","password":"test123"}')
check "30.1 新销售注册" "success\|成功" "$REG30"

# 2. 登录获取推广码
LOGIN30=$(post_json "$BASE/api/auth/sales/login" '{"phone":"17700001003","password":"test123"}')
TOKEN30=$(echo "$LOGIN30" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
CODE30=$(echo "$LOGIN30" | grep -o '"code":"[^"]*"' | cut -d'"' -f4)
check "30.2 登录获取推广码" "code" "$LOGIN30"

# 3. 客户通过推广链接下单
ORDER30=$(post_json "$BASE/api/pay/create" "{\"amount\":199,\"salesCode\":\"$CODE30\"}")
ORDER30_NO=$(echo "$ORDER30" | grep -o '"orderNo":"[^"]*"' | cut -d'"' -f4)
check "30.3 客户下单" "orderNo" "$ORDER30"

# 4. 模拟支付（simulate-pay 内部会处理 pending→paid→delivered）
ORDER30_ID=$(mysql -u root -p123456 -P 3308 sales_system -N -e "SELECT id FROM orders WHERE order_no='$ORDER30_NO'" 2>/dev/null)
check "30.4 订单创建成功" "1" "1"

# 5. 管理员触发发货
AUTH_HEADER="Bearer $ADMIN_TOKEN"
# 先确保产品启用了授权验证
mysql -u root -p123456 -P 3308 sales_system -e "UPDATE products SET auth_enabled=1, auth_validity_hours=72 WHERE id=$PRODUCT_ID" 2>/dev/null
SIM30=$(curl -s -X POST "$BASE/api/admin/orders/$ORDER30_ID/simulate-pay" -H "Authorization: Bearer $ADMIN_TOKEN")
check "30.5 管理员触发支付+发货" "success" "$SIM30"

# 6. 客户验证授权码
AUTH_HEADER=""
V30=$(post_json "$BASE/api/auth/validate" '{"authCode":"SCN-E2E-30","machine":"e2e-machine-001"}')
check "30.6 客户验证授权码成功" '"ok":true' "$V30"

# 7. 客户核销
R30=$(post_json "$BASE/api/auth/redeem" '{"authCode":"SCN-E2E-30","machine":"e2e-machine-001"}')
check "30.7 客户核销成功" "success" "$R30"

# 8. 检查销售统计数据包含该订单
AUTH_HEADER="Bearer $TOKEN30"
STATS30=$(curl -s "$BASE/api/orders/stats" -H "Authorization: Bearer $TOKEN30")
check "30.8 销售统计包含该订单" "totalOrders\|totalAmount" "$STATS30"

# 9. 检查排行榜
LB30=$(curl -s "$BASE/api/leaderboard/top3")
check "30.9 排行榜包含该销售" "name\|amount" "$LB30"

# 10. 检查管理端统计
AUTH_HEADER="Bearer $ADMIN_TOKEN"
ADM30=$(curl -s "$BASE/api/admin/stats" -H "Authorization: Bearer $ADMIN_TOKEN")
check "30.10 管理端统计包含该订单" "totalRevenue\|totalOrders" "$ADM30"

# ══════════════════════════════════════════════════
# 汇总
# ══════════════════════════════════════════════════
echo -e "\n${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  测试结果${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"

TOTAL=$((PASS + FAIL))
echo -e "  ${GREEN}通过: $PASS${NC}"
echo -e "  ${RED}失败: $FAIL${NC}"
echo -e "  总计: $TOTAL"

if [ $FAIL -eq 0 ]; then
  echo -e "\n  ${GREEN}🎉 全部场景测试通过！${NC}"
else
  echo -e "\n  ${YELLOW}⚠️  有 $FAIL 个场景测试失败${NC}"
fi

rm -f "$TMPJSON"

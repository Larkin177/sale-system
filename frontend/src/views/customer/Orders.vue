<template>
  <div class="orders-page">
    <!-- 顶部导航栏 -->
    <nav class="top-nav">
      <div class="nav-inner">
        <span class="nav-brand" @click="$router.push('/')">{{ siteName || 'CC-Installer' }}</span>
        <div class="nav-links">
          <el-button text size="small" @click="$router.push('/')">🏠 返回首页</el-button>
        </div>
      </div>
    </nav>

    <div class="orders-body">
      <h2 class="page-title">📋 我的订单</h2>

      <!-- 输入邮箱查询（无订单号时显示） -->
      <div v-if="!orderNo && orders.length === 0 && !loading" class="email-form">
        <p class="form-tip">请输入购买时填写的邮箱地址查询订单</p>
        <el-input v-model="emailInput" placeholder="your@email.com" size="large" @keyup.enter="lookupByEmail" />
        <el-button type="primary" size="large" class="search-btn" @click="lookupByEmail" :loading="loading">
          查询订单
        </el-button>
        <p class="form-hint" v-if="savedEmail">💡 已自动关联最后使用的邮箱: {{ savedEmail }}</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state" v-loading="loading"></div>

      <!-- 无订单 -->
      <div v-if="!loading && orders.length === 0 && searched" class="empty-state">
        <el-empty description="没有找到相关订单" />
      </div>

      <!-- 订单列表 -->
      <div v-if="orders.length > 0" class="orders-list">
        <div v-if="email" class="email-display">
          📧 {{ email }}
          <el-button size="small" text @click="resetSearch">更换邮箱</el-button>
        </div>

        <div v-for="o in orders" :key="o.orderNo" class="order-card">
          <!-- 订单头部 -->
          <div class="order-head">
            <div class="order-head-left">
              <span class="order-label">订单号</span>
              <span class="order-no">{{ o.orderNo }}</span>
            </div>
            <el-tag
              :type="statusTagType(o.status)"
              size="small"
              effect="dark"
              class="order-status-tag"
            >
              {{ statusText(o.status) }}
            </el-tag>
          </div>

          <!-- 订单内容 -->
          <div class="order-body">
            <div class="order-info">
              <div class="info-row">
                <span class="info-label">套餐</span>
                <span class="info-value">{{ o.packageName || o.productName || '通用套餐' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">金额</span>
                <span class="info-value price">¥{{ o.amount }}</span>
              </div>
              <div class="info-row" v-if="o.createdAt">
                <span class="info-label">时间</span>
                <span class="info-value">{{ formatDate(o.createdAt) }}</span>
              </div>
            </div>
          </div>

          <!-- 授权码区域（已发货状态） -->
          <div v-if="o.authCode && (o.status === 'delivered' || o.status === 'consumed')" class="auth-section">
            <div class="auth-header">
              <span class="auth-label">🔐 授权码</span>
              <el-tag
                :type="o.authStatus === 'consumed' ? 'danger' : 'success'"
                size="small"
                effect="light"
              >
                {{ o.authStatus === 'consumed' ? '已核销' : '未使用' }}
              </el-tag>
            </div>

            <div class="auth-code-box">
              <code class="auth-code-text">{{ o.authCode }}</code>
              <el-button
                type="primary"
                size="small"
                @click="copyAuthCode(o.authCode)"
                class="copy-btn"
              >
                📋 复制
              </el-button>
            </div>

            <p class="auth-tip" v-if="o.authStatus !== 'consumed'">
              ⚠️ 授权码有效期 {{ o.authValidityHours || 72 }} 小时，一机一码，激活后即失效
            </p>
            <p class="auth-tip consumed" v-else>
              ✅ 该授权码已于 {{ formatDate(o.authUsedAt) }} 核销
            </p>

            <!-- 下载按钮 -->
            <el-button
              type="primary"
              size="large"
              class="download-btn"
              @click="handleDownload(o)"
            >
              📥 下载安装器
            </el-button>
          </div>

          <!-- 待审核状态 -->
          <div v-if="o.status === 'pending_verify'" class="pending-section">
            <el-result icon="warning" title="等待审核" sub-title="管理员确认到账后将自动发货，请留意查收邮件">
              <template #extra>
                <el-button type="primary" @click="refresh">刷新状态</el-button>
              </template>
            </el-result>
          </div>

          <!-- 待支付状态 -->
          <div v-if="o.status === 'pending'" class="pending-section">
            <el-result icon="info" title="等待支付" sub-title="订单已创建，请完成支付">
            </el-result>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()

const siteName = ref('')
const emailInput = ref('')
const email = ref('')
const orders = ref([])
const loading = ref(false)
const searched = ref(false)
const orderNo = ref('')

const savedEmail = ref(localStorage.getItem('customer_email') || '')

const statusMap = {
  pending: '待支付',
  pending_verify: '审核中',
  paid: '已付款',
  delivered: '已发货',
  consumed: '已核销'
}

function statusText(s) {
  return statusMap[s] || s
}

function statusTagType(s) {
  if (s === 'delivered' || s === 'consumed') return 'success'
  if (s === 'pending_verify') return 'warning'
  if (s === 'paid') return 'primary'
  return 'info'
}

function formatDate(d) {
  if (!d) return ''
  const date = new Date(d)
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')} ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`
}

async function lookupByEmail() {
  const val = emailInput.value || savedEmail.value
  if (!val) {
    ElMessage.warning('请输入邮箱')
    return
  }
  loading.value = true
  searched.value = true
  try {
    email.value = val
    localStorage.setItem('customer_email', val)
    const res = await request.get('/customer/orders?email=' + encodeURIComponent(val))
    orders.value = res.data || []
    if (orders.value.length === 0) {
      ElMessage.info('没有找到相关订单')
    }
  } catch (e) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

async function lookupByOrderNo(no) {
  loading.value = true
  searched.value = true
  try {
    const res = await request.get('/customer/order?orderNo=' + encodeURIComponent(no))
    if (res.data) {
      orders.value = [res.data]
    } else {
      orders.value = []
      ElMessage.info('没有找到该订单')
    }
  } catch (e) {
    ElMessage.error('查询失败')
    orders.value = []
  } finally {
    loading.value = false
  }
}

function copyAuthCode(code) {
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success('授权码已复制')
  }).catch(() => {
    // Fallback
    const ta = document.createElement('textarea')
    ta.value = code
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('授权码已复制')
  })
}

function handleDownload(o) {
  // Try to open the download link from package config
  // Currently we redirect to the order detail which has the download
  ElMessage.info('下载功能开发中，请稍候...')
}

function refresh() {
  if (orderNo.value) {
    lookupByOrderNo(orderNo.value)
  } else if (email.value) {
    lookupByEmail()
  }
}

function resetSearch() {
  email.value = ''
  orders.value = []
  searched.value = false
  emailInput.value = savedEmail.value || ''
}

onMounted(async () => {
  // 加载站点名称
  try {
    const res = await request.get('/site-settings')
    siteName.value = res.data?.site_name || 'CC-Installer'
  } catch (e) {}

  // 从 URL 获取订单号（邮件链接直达）
  orderNo.value = route.query.orderNo || ''

  // 自动填充邮箱
  if (savedEmail.value) {
    emailInput.value = savedEmail.value
  }

  if (orderNo.value) {
    // 直接查询订单号
    await lookupByOrderNo(orderNo.value)
  } else if (savedEmail.value) {
    // 自动查询
    await lookupByEmail()
  }
})
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: #f5f7fa;
}

/* Nav */
.top-nav {
  background: white;
  border-bottom: 1px solid #eef0f6;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-brand {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  cursor: pointer;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Body */
.orders-body {
  max-width: 680px;
  margin: 0 auto;
  padding: 40px 24px;
}

.page-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 32px;
}

/* Email Form */
.email-form {
  background: white;
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.form-tip {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 16px;
  text-align: center;
}

.search-btn {
  width: 100%;
  height: 48px;
  margin-top: 16px;
  font-size: 16px;
  border-radius: 12px;
}

.form-hint {
  font-size: 13px;
  color: #f59e0b;
  margin-top: 12px;
  text-align: center;
}

/* States */
.loading-state {
  padding: 80px 0;
}

.empty-state {
  padding: 60px 0;
}

/* Orders List */
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.email-display {
  font-size: 14px;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  padding: 12px 16px;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

/* Order Card */
.order-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f9fafb;
  border-bottom: 1px solid #f3f4f6;
}

.order-head-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.order-label {
  font-size: 11px;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.order-no {
  font-size: 13px;
  color: #374151;
  font-family: monospace;
}

.order-status-tag {
  flex-shrink: 0;
}

.order-body {
  padding: 16px 20px;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 14px;
  color: #6b7280;
}

.info-value {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
}

.info-value.price {
  font-size: 18px;
  font-weight: 700;
  color: #667eea;
}

/* Auth Section */
.auth-section {
  border-top: 1px solid #f3f4f6;
  padding: 16px 20px;
}

.auth-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.auth-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.auth-code-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f3f4f6;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 10px;
}

.auth-code-text {
  flex: 1;
  font-size: 12px;
  color: #374151;
  word-break: break-all;
  font-family: 'Courier New', monospace;
  line-height: 1.6;
}

.copy-btn {
  flex-shrink: 0;
}

.auth-tip {
  font-size: 12px;
  color: #f59e0b;
  margin: 0 0 12px;
}

.auth-tip.consumed {
  color: #22c55e;
}

.download-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 10px;
}

/* Pending Section */
.pending-section {
  padding: 16px 20px;
}

/* Responsive */
@media (max-width: 768px) {
  .orders-body { padding: 24px 16px; }
  .page-title { font-size: 22px; }
  .email-form { padding: 20px; }
}
</style>

<template>
  <div class="orders-page">
    <div class="orders-container">
      <h2>My Orders</h2>

      <div v-if="!email" class="email-form">
        <p>Enter your email to view your orders</p>
        <el-input v-model="emailInput" placeholder="your@email.com" size="large" @keyup.enter="lookup" />
        <el-button type="primary" size="large" @click="lookup" :loading="loading" style="margin-top:12px;width:100%">Search</el-button>
      </div>

      <div v-if="email && orders.length === 0 && !loading">
        <el-empty description="No orders found" />
      </div>

      <div v-if="orders.length > 0" class="orders-list">
        <div class="email-display">📧 {{ email }} <el-button size="small" text @click="email='';orders=[]">Change</el-button></div>
        
        <div v-for="o in orders" :key="o.orderNo" class="order-card">
          <div class="order-head">
            <span class="order-no">{{ o.orderNo }}</span>
            <el-tag :type="o.status==='delivered'?'success':o.status==='pending_verify'?'warning':'info'" size="small">{{ statusText(o.status) }}</el-tag>
          </div>
          <div class="order-body">
            <span>{{ o.productName || o.packageName || 'Package' }}</span>
            <span class="order-amount">¥{{ o.amount }}</span>
          </div>
          <div v-if="o.authCode" class="auth-section">
            <div class="auth-label">Auth Code</div>
            <div class="auth-code-row">
              <code class="auth-code">{{ o.authCode }}</code>
              <el-button size="small" @click="copy(o.authCode)">Copy</el-button>
            </div>
          </div>
          <div v-if="o.status==='delivered'" class="download-section">
            <el-button type="primary" :href="downloadUrl" target="_blank">
              📥 Download CC-Installer
            </el-button>
          </div>
          <div v-if="o.status==='pending_verify'" class="pending-hint">
            Payment submitted, waiting for admin review...
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const email = ref('')
const emailInput = ref('')
const orders = ref([])
const loading = ref(false)
const downloadUrl = ref('')

onMounted(async () => {
  // Auto-detect from localStorage
  const saved = localStorage.getItem('customer_email')
  if (saved) {
    emailInput.value = saved
    email.value = saved
    await lookup()
  }

  // Get download URL from config
  try {
    const res = await request.get('/admin/config')
    downloadUrl.value = res.data?.site_url ? res.data.site_url + '/download' : '#'
  } catch(e) {}
})

const statusText = (s) => ({pending:'Pending',pending_verify:'Reviewing',paid:'Paid',delivered:'Delivered',redeemed:'Redeemed'}[s]||s)

async function lookup() {
  if (!emailInput.value) return
  loading.value = true
  try {
    email.value = emailInput.value
    localStorage.setItem('customer_email', email.value)
    const res = await request.get('/customer/orders?email=' + encodeURIComponent(email.value))
    orders.value = res.data || []
  } catch(e) {
    ElMessage.error('Lookup failed')
  } finally { loading.value = false }
}

function copy(text) {
  navigator.clipboard.writeText(text)
  ElMessage.success('Copied')
}
</script>

<style scoped>
.orders-page { min-height: 100vh; background: #f5f7fa; }
.orders-container { max-width: 600px; margin: 0 auto; padding: 40px 24px; }
.orders-container h2 { margin: 0 0 24px; }
.email-form { background: #fff; padding: 24px; border-radius: 12px; }
.email-display { margin-bottom: 16px; font-size: 14px; color: #666; display: flex; align-items: center; gap: 8px; }
.order-card { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,.04); }
.order-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-no { font-size: 13px; color: #999; }
.order-body { display: flex; justify-content: space-between; font-size: 15px; }
.order-amount { font-weight: 600; color: #667eea; }
.auth-section { margin-top: 12px; padding-top: 12px; border-top: 1px solid #f3f4f6; }
.auth-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.auth-code-row { display: flex; align-items: center; gap: 8px; }
.auth-code { font-size: 12px; background: #f3f4f6; padding: 4px 8px; border-radius: 4px; word-break: break-all; flex: 1; }
.download-section { margin-top: 12px; }
.pending-hint { font-size: 12px; color: #e6a23c; margin-top: 8px; }
</style>

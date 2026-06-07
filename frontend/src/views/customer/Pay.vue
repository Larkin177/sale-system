<template>
  <div class="pay-page" v-loading="pageLoading">
    <div v-if="!pageLoading" class="hero" :style="{'--hero-bg': site.hero_bg_color || '#667eea','--hero-bg-end': site.hero_bg_color_end || '#764ba2'}">
      <div class="hero-content">
        <h1>{{ site.hero_title || 'Professional Tools' }}</h1>
        <p>{{ site.hero_subtitle || 'Efficient, stable, secure' }}</p>
      </div>
    </div>

    <div class="pay-body" v-if="!showQrCode">
      <!-- Package Select -->
      <div class="package-select" v-if="payStep === 'select'">
        <h2>{{ config.product_page_title || 'Select Package' }}</h2>
        <p class="sub">{{ config.product_page_subtitle }}</p>
        <el-tabs v-model="platform" class="platform-tabs">
          <el-tab-pane label="Mac" name="mac" />
          <el-tab-pane label="Windows" name="windows" />
        </el-tabs>
        <div class="packages-grid">
          <div class="pkg-card" v-for="p in filteredPackages" :key="p.id" :class="{selected:selectedPkg===p}" @click="selectedPkg=p">
            <h3>{{ p.name }}</h3>
            <div class="pkg-price">¥{{ p.price }}</div>
            <p v-if="p.description" class="pkg-desc">{{ p.description }}</p>
          </div>
        </div>
      </div>

      <!-- Email + CAPTCHA -->
      <div class="email-section" v-if="payStep === 'email'">
        <h2>Enter your email</h2>
        <p class="privacy-notice">🔒 Your email is only used to notify you when your order is ready. We never share it.</p>
        <el-input v-model="emailInput" placeholder="your@email.com" size="large" class="email-input" @keyup.enter="goPay" />
        <p class="email-reminder">📨 发货提醒将发送至该邮箱，请务必填写正确</p>
        
        <div class="captcha-section" v-if="emailInput">
          <div class="captcha-row">
            <el-input v-model="captchaAnswer" placeholder="CAPTCHA" maxlength="4" size="large" class="captcha-input" @keyup.enter="goPay" />
            <div class="captcha-img" @click="loadCaptcha" title="Refresh">
              <img v-if="captchaImage" :src="captchaImage" alt="captcha" />
              <span v-else>Loading...</span>
            </div>
          </div>
        </div>

        <el-button type="primary" size="large" class="submit-btn" @click="goPay" :disabled="!canPay" :loading="loading">
          Pay ¥{{ selectedPkg?.price || 0 }}
        </el-button>
      </div>
    </div>

    <!-- Payment QR -->
    <div class="qr-section" v-if="showQrCode">
      <h2>Scan to Pay ¥{{ orderAmount }}</h2>
      <el-tabs v-model="paymentMethod" class="pay-tabs">
        <el-tab-pane label="WeChat" name="wechat" />
        <el-tab-pane label="Alipay" name="alipay" />
      </el-tabs>
      <div class="qr-box">
        <img v-if="currentQr" :src="currentQr" class="qr-img" />
        <p v-else>No QR code</p>
      </div>
      <p class="order-info">Order: {{ orderNo }}</p>
      <p class="mode-hint">{{ isStaticMode ? 'After payment, click confirm' : 'Auto detected' }}</p>
      <el-button v-if="isStaticMode" type="success" size="large" @click="markPaid" :loading="submitting">
        I have paid
      </el-button>
      
    <!-- Paid confirmation dialog -->
    <el-dialog v-model="paidDialogVisible" title="✅ 支付确认已提交" width="440px" :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false" center>
      <div class="paid-dialog-body">
        <div class="paid-step">
          <div class="paid-step-num">1</div>
          <div class="paid-step-text">
            <strong>等待管理员确认收款</strong>
            <p>管理员核对到账后会自动确认</p>
          </div>
        </div>
        <div class="paid-step">
          <div class="paid-step-num">2</div>
          <div class="paid-step-text">
            <strong>查收邮件通知</strong>
            <p>确认后我们会向 <b>{{ emailInput }}</b> 发送通知邮件</p>
          </div>
        </div>
        <div class="paid-step">
          <div class="paid-step-num">3</div>
          <div class="paid-step-text">
            <strong>回到本站获取授权码</strong>
            <p>点击邮件中的链接，或直接访问 <a href="/orders">我的订单</a> 查看授权码并下载</p>
          </div>
        </div>
      </div>
      <div class="paid-countdown">
        <span>{{ countdown }} 秒后自动关闭</span>
      </div>
      <template #footer>
        <el-button type="primary" @click="closePaidDialog" :disabled="countdown > 0">
          {{ countdown > 0 ? '请等待 ' + countdown + 's' : '我知道了' }}
        </el-button>
      </template>
    </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const pageLoading = ref(true)
const site = ref({})
const config = ref({})
const packages = ref([])
const platform = ref('windows')
const selectedPkg = ref(null)
const payStep = ref('select')
const emailInput = ref('')
const captchaAnswer = ref('')
const captchaImage = ref('')
const captchaId = ref('')
const loading = ref(false)
const showQrCode = ref(false)
const orderNo = ref('')
const orderAmount = ref('')
const qrWechat = ref('')
const qrAlipay = ref('')
const qrAlipayDyn = ref('')
const wechatMode = ref('static')
const alipayMode = ref('static')
const paymentMethod = ref('wechat')
const submitting = ref(false)
const paidSubmitted = ref(false)
const paidDialogVisible = ref(false)
const countdown = ref(10)
let countdownTimer = null

const filteredPackages = computed(() => packages.value.filter(p => p.platform === platform.value))
const isStaticMode = computed(() => paymentMethod.value === 'wechat' ? wechatMode.value==='static' : alipayMode.value==='static')
const currentQr = computed(() => {
  if (paymentMethod.value==='wechat') return qrWechat.value
  if (alipayMode.value==='api' && qrAlipayDyn.value) return qrAlipayDyn.value
  return qrAlipay.value
})
const canPay = computed(() => emailInput.value && captchaAnswer.value && selectedPkg.value)

async function loadCaptcha() {
  try {
    const res = await request.get('/captcha/generate')
    captchaId.value = res.data?.captchaId || ''
    captchaImage.value = res.data?.image || ''
  } catch(e) {}
}

watch(emailInput, () => { if (emailInput.value && !captchaImage.value) loadCaptcha() })

function goPay() {
  if (!emailInput.value) { ElMessage.warning('Enter email'); return }
  if (!captchaAnswer.value) { ElMessage.warning('Enter CAPTCHA'); return }
  localStorage.setItem('customer_email', emailInput.value)
  payStep.value = 'pay'
}

async function markPaid() {
  submitting.value = true
  try {
    await request.post('/pay/mark-paid', { orderNo: orderNo.value })
    paidSubmitted.value = true
    paidDialogVisible.value = true
    countdown.value = 10
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(countdownTimer)
    }, 1000)
  } catch(e) { ElMessage.error('Failed') }
  finally { submitting.value = false }
}

function closePaidDialog() {
  if (countdownTimer) clearInterval(countdownTimer)
  paidDialogVisible.value = false
}

onMounted(async () => {
  try {
    const [s, c, p] = await Promise.all([
      request.get('/site-settings').catch(()=>({data:{}})),
      request.get('/config').catch(()=>({data:{}})),
      request.get('/packages').catch(()=>({data:[]}))
    ])
    site.value = s.data || {}
    config.value = c.data || {}
    packages.value = p.data || []

    // Detect platform
    const ua = navigator.userAgent
    if (ua.includes('Mac')) platform.value = 'mac'
    
    // Pre-select from URL
    const pid = route.query.packageId
    if (pid) selectedPkg.value = packages.value.find(p => p.id == pid)

    // 从 Home 跳转过来时跳过套餐选择，直接进入邮箱步骤
    if (route.query.auto === '1' && selectedPkg.value) {
      payStep.value = 'email'
    }

    // Restore email
    const saved = localStorage.getItem('customer_email')
    if (saved) emailInput.value = saved

    pageLoading.value = false
    loadCaptcha()
  } catch(e) { pageLoading.value = false }
})

// Create order when entering pay step
watch(payStep, async (step) => {
  if (step === 'pay' && selectedPkg.value) {
    loading.value = true
    try {
      const res = await request.post('/pay/create', {
        packageId: selectedPkg.value.id,
        email: emailInput.value,
        salesCode: route.query.s,
        paymentMethod: paymentMethod.value
      })
      orderNo.value = res.data.orderNo
      orderAmount.value = res.data.amount
      qrWechat.value = res.data.wechatQrcode || ''
      qrAlipay.value = res.data.alipayQrcode || ''
      qrAlipayDyn.value = res.data.alipayQrCode || ''
      wechatMode.value = res.data.wechatMode || 'static'
      alipayMode.value = res.data.alipayMode || 'static'
      showQrCode.value = true
    } catch(e) { ElMessage.error('Order creation failed') }
    finally { loading.value = false }
  }
})
</script>

<style scoped>
.pay-page { min-height: 100vh; background: #f5f7fa; }
.hero { background: linear-gradient(135deg, var(--hero-bg), var(--hero-bg-end)); padding: 60px 24px; text-align: center; color: #fff; }
.hero h1 { font-size: 36px; margin: 0 0 8px; }
.pay-body { max-width: 700px; margin: 40px auto; padding: 0 24px; background: #fff; border-radius: 16px; padding: 32px; box-shadow: 0 2px 16px rgba(0,0,0,.06); }
.packages-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(200px,1fr)); gap: 12px; margin-top: 16px; }
.pkg-card { border: 2px solid #e5e7eb; border-radius: 12px; padding: 16px; cursor: pointer; transition: all .2s; }
.pkg-card:hover { border-color: #667eea; }
.pkg-card.selected { border-color: #667eea; background: #eef2ff; }
.pkg-card h3 { margin: 0 0 8px; font-size: 16px; }
.pkg-price { font-size: 28px; font-weight: 700; color: #667eea; }
.email-input { margin-top: 12px; }
.captcha-section { margin-top: 12px; }
.captcha-row { display: flex; gap: 12px; }
.captcha-input { flex: 1; }
.captcha-img { width: 120px; height: 40px; border-radius: 8px; overflow: hidden; cursor: pointer; border: 1px solid #ddd; }
.captcha-img img { width: 100%; height: 100%; object-fit: cover; }
.submit-btn { width: 100%; height: 48px; margin-top: 20px; font-size: 16px; border-radius: 12px; }
.qr-section { max-width: 500px; margin: 60px auto; text-align: center; }
.qr-box { width: 220px; height: 220px; margin: 20px auto; border: 2px solid #e5e7eb; border-radius: 12px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.qr-img { width: 100%; height: 100%; object-fit: contain; }
.order-info { font-size: 13px; color: #999; }
.success-msg { color: #22c55e; margin-top: 12px; }
.mode-hint { font-size: 12px; color: #999; margin: 4px 0; }
.privacy-notice { font-size: 12px; color: #999; margin: 8px 0; }
.sub { color: #666; margin: 4px 0 16px; }

.email-reminder {
  font-size: 13px;
  color: #f59e0b;
  margin: 8px 0 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.paid-dialog-body {
  padding: 8px 0;
}

.paid-step {
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
  align-items: flex-start;
}

.paid-step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.paid-step-text strong {
  display: block;
  font-size: 15px;
  margin-bottom: 2px;
}

.paid-step-text p {
  margin: 0;
  font-size: 13px;
  color: #666;
}

.paid-countdown {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin-top: 8px;
}

</style>

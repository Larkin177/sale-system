<template>
  <div class="pay-page" v-loading="pageLoading">
    <!-- Hero Section -->
    <div
      v-if="!pageLoading"
      class="hero"
      :style="{
        '--hero-bg': siteSettings.hero_bg_color || '#667eea',
        '--hero-bg-end': siteSettings.hero_bg_color_end || '#764ba2'
      }"
    >
      <div class="hero-content">
        <h1 class="hero-title">{{ siteSettings.hero_title || '专业软件工具' }}</h1>
        <p class="hero-subtitle">{{ siteSettings.hero_subtitle || '高效、稳定、安全的解决方案' }}</p>
        <el-button type="primary" size="large" class="hero-btn" @click="openPayModal">
          立即购买
        </el-button>
      </div>
    </div>
    <div v-else class="hero hero-skeleton">
      <div class="hero-content">
        <div class="skeleton-line skeleton-title"></div>
        <div class="skeleton-line skeleton-subtitle"></div>
        <div class="skeleton-btn"></div>
      </div>
    </div>

    <!-- Features Section -->
    <div class="features-section" v-if="featureList.length > 0">
      <div class="features">
        <div
          class="feature-card"
          v-for="(item, index) in featureList"
          :key="index"
          @click="openFeatureModal(item)"
        >
          <div class="feature-icon">
            <img
              v-if="item.icon_url"
              :src="item.icon_url"
              alt=""
              class="feature-custom-icon"
            />
            <el-icon v-else :size="40">
              <component :is="item.icon" />
            </el-icon>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </div>

    <!-- Payment Modal -->
    <el-dialog
      v-model="showPayModal"
      :show-close="true"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      width="440px"
      class="pay-dialog"
      destroy-on-close
    >
      <template #header>
        <div class="pay-dialog-header">
          <div class="pay-dialog-steps">
            <div class="step-indicator" :class="{ active: payStep === 1, done: payStep === 2 }">
              <span class="step-num">1</span>
              <span class="step-label">验证手机</span>
            </div>
            <div class="step-line" :class="{ active: payStep === 2 }"></div>
            <div class="step-indicator" :class="{ active: payStep === 2 }">
              <span class="step-num">2</span>
              <span class="step-label">确认支付</span>
            </div>
          </div>
        </div>
      </template>

      <div class="pay-dialog-body">
        <!-- Step 1: Phone Input -->
        <transition name="step-fade" mode="out-in">
          <div v-if="payStep === 1" key="step1" class="pay-step">
            <h3 class="step-title">请输入您的手机号码</h3>

            <div class="privacy-notice">
              <span class="lock-icon">&#128274;</span>
              <p>您的手机号码仅用于发送工具下载链接，平台严格保护用户隐私，绝不向第三方泄露。</p>
            </div>

            <div class="phone-input-wrapper">
              <el-input
                v-model="phoneInput"
                placeholder="请输入11位手机号码"
                size="large"
                :prefix-icon="Phone"
                maxlength="11"
                class="phone-input"
                @keyup.enter="checkPhone"
              />
            </div>

            <!-- 图形验证码 -->
            <div class="captcha-section" v-if="phoneInput.length === 11 && !codeSent">
              <div class="captcha-row">
                <el-input
                  v-model="captchaAnswer"
                  placeholder="请输入图形验证码"
                  maxlength="4"
                  size="large"
                  class="captcha-input"
                  @keyup.enter="sendCode"
                />
                <div class="captcha-img" @click="loadCaptcha" :title="'点击刷新'">
                  <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                  <span v-else-if="captchaLoading" class="captcha-loading">加载中...</span>
                </div>
              </div>
            </div>

            <!-- 验证码 -->
            <div class="code-section" v-if="codeSent || phoneInput.length === 11">
              <div class="code-input-row">
                <el-input
                  v-model="verificationCode"
                  placeholder="请输入6位验证码"
                  maxlength="6"
                  size="large"
                  :prefix-icon="Message"
                  class="phone-input"
                />
                <el-button
                  size="large"
                  :disabled="codeCountdown > 0 || phoneInput.length !== 11"
                  :loading="codeSending"
                  @click="sendCode"
                  class="code-btn"
                >
                  {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </div>

            <el-button
              type="primary"
              size="large"
              class="pay-submit-btn"
              @click="checkPhone"
              :loading="codeVerifying || phoneLoading"
              :disabled="phoneInput.length !== 11 || !verificationCode"
            >
              {{ codeVerifying ? '验证中...' : phoneLoading ? '查询中...' : '下一步' }}
            </el-button>
          </div>

          <!-- Step 2: Payment -->
          <div v-else key="step2" class="pay-step">
            <!-- Step 2: Payment form (before order creation) -->
            <div v-if="!showQrCode" class="payment-form-view">
              <h3 class="step-title" v-if="phoneHistory">
                检测到您之前的购买记录，以下为您专属的价格
              </h3>
              <h3 class="step-title" v-else>
                {{ pageSettings.pay_subtitle || '确认您的订单信息' }}
              </h3>

              <!-- Sales code tag -->
              <div class="sales-info" v-if="salesCode">
                <el-tag type="success" effect="dark" round>专属链接</el-tag>
              </div>

              <!-- Price display -->
              <div class="price-display">
                <span class="price-label">订单金额</span>
                <span class="price">¥{{ price }}</span>
              </div>

              <!-- Phone display with mask -->
              <div class="phone-display">
                <el-icon><Phone /></el-icon>
                <span>{{ maskedPhone }}</span>
                <el-link type="primary" :underline="false" @click="goBackToStep1" class="change-phone">
                  更换
                </el-link>
              </div>

              <!-- Payment method -->
              <div class="payment-methods">
                <h4>选择支付方式</h4>
                <el-radio-group v-model="paymentMethod" class="payment-radio-group">
                  <el-radio-button value="wechat" class="payment-option">
                    <el-icon><ChatDotRound /></el-icon> 微信支付
                  </el-radio-button>
                  <el-radio-button value="alipay" class="payment-option">
                    <el-icon><Wallet /></el-icon> 支付宝
                  </el-radio-button>
                </el-radio-group>
              </div>

              <el-button
                type="primary"
                size="large"
                class="pay-submit-btn"
                @click="handlePay"
                :loading="loading"
              >
                立即支付
              </el-button>
            </div>

            <!-- Step 2: QR code display (after order creation) -->
            <div v-if="showQrCode" class="payment-qr-view">
              <div class="qr-header">
                <el-tag type="success" effect="dark">订单已创建</el-tag>
              </div>

              <div class="qr-price">
                <span class="qr-amount">¥{{ price }}</span>
              </div>

              <div class="qr-code-container">
                <img v-if="qrCodeImage" :src="qrCodeImage" alt="支付二维码" class="qr-code-img" />
                <div v-else class="qr-code-placeholder">
                  <el-icon :size="48"><Wallet /></el-icon>
                  <p>暂未配置支付二维码</p>
                </div>
              </div>

              <div class="qr-info">
                <p>请使用{{ paymentMethod === 'wechat' ? '微信' : '支付宝' }}扫描二维码完成支付</p>
                <p class="qr-order-no">订单号：{{ orderNo }}</p>
              </div>

              <el-button type="success" size="large" class="paid-btn" @click="handlePaid">
                我已支付
              </el-button>

              <el-button text @click="showQrCode = false" class="change-method-btn">
                更换支付方式
              </el-button>
            </div>
          </div>
        </transition>
      </div>
    </el-dialog>

    <!-- Feature Modal (separate from payment) -->
    <el-dialog
      v-model="showFeatureModal"
      :title="modalFeature?.title"
      width="600px"
      :close-on-click-modal="true"
      class="feature-dialog"
    >
      <!-- Image -->
      <div v-if="modalFeature?.content_type === 'image'" class="modal-content">
        <img :src="modalFeature.content_value" alt="" class="modal-image" />
      </div>
      <!-- Video -->
      <div v-else-if="modalFeature?.content_type === 'video'" class="modal-content">
        <video :src="modalFeature.content_value" controls class="modal-video"></video>
      </div>
      <!-- Text -->
      <div v-else-if="modalFeature?.content_type === 'text'" class="modal-content modal-text">
        {{ modalFeature.content_value }}
      </div>
      <!-- Download -->
      <div v-else-if="modalFeature?.content_type === 'download'" class="modal-content">
        <el-button type="primary" size="large" @click="downloadFile(modalFeature.content_value)">
          <el-icon><Download /></el-icon> 点击下载
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, Wallet, Phone, Trophy, Star, Service, Coin, Position, Download, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getCustomerLastOrder, sendVerificationCode as sendVerificationCodeAPI, verifyCode as verifyCodeAPI, generateCaptcha as generateCaptchaAPI, verifyCaptcha as verifyCaptchaAPI, getPaymentQrCode as getPaymentQrCodeAPI, getCustomerPrice as getCustomerPriceAPI } from '@/api/config'

const route = useRoute()
const salesCode = ref(route.query.s || '')
const customPrice = ref(route.query.p ? parseInt(route.query.p) : null)
const basePrice = ref(99)
const price = ref(99)
const minPrice = ref(80)
const maxPrice = ref(150)
const paymentMethod = ref('wechat')
const loading = ref(false)
const pageLoading = ref(true)

// Payment modal state
const showPayModal = ref(false)
const payStep = ref(1)
const phoneInput = ref('')
const phoneLoading = ref(false)
const phoneHistory = ref(null)
const verificationCode = ref('')
const codeSent = ref(false)
const codeCountdown = ref(0)
const codeSending = ref(false)
const codeVerifying = ref(false)
const captchaId = ref('')
const captchaImage = ref('')
const captchaAnswer = ref('')
const captchaLoading = ref(false)

// QR code display state
const showQrCode = ref(false)
const qrCodeImage = ref('')
const orderNo = ref('')
const wechatPayMode = ref('static')
const alipayPayMode = ref('static')
const isStaticMode = ref(true)

// Load CAPTCHA when phone number reaches 11 digits
watch(phoneInput, (val) => {
  if (val.length === 11 && !codeSent.value) {
    loadCaptcha()
  }
})

// Feature modal
const showFeatureModal = ref(false)
const modalFeature = ref(null)

const pageSettings = ref({
  pay_title: '购买软件工具',
  pay_subtitle: ''
})

const siteSettings = ref({
  hero_title: '专业软件工具',
  hero_subtitle: '高效、稳定、安全的解决方案',
  hero_bg_color: '#667eea',
  hero_bg_color_end: '#764ba2',
  feature_1_icon: 'Trophy',
  feature_1_icon_url: '',
  feature_1_title: '品质保证',
  feature_1_desc: '经过严格测试，稳定可靠',
  feature_1_content_type: 'none',
  feature_1_content_value: '',
  feature_2_icon: 'Star',
  feature_2_icon_url: '',
  feature_2_title: '持续更新',
  feature_2_desc: '定期更新，功能不断增强',
  feature_2_content_type: 'none',
  feature_2_content_value: '',
  feature_3_icon: 'Service',
  feature_3_icon_url: '',
  feature_3_title: '技术支持',
  feature_3_desc: '专业团队提供技术支持',
  feature_3_content_type: 'none',
  feature_3_content_value: ''
})

const iconMap = { trophy: Trophy, star: Star, service: Service, coin: Coin, position: Position, Trophy, Star, Service, Coin, Position }

const featureList = computed(() => [
  {
    icon: iconMap[siteSettings.value.feature_1_icon] || iconMap[siteSettings.value.feature_1_icon?.toLowerCase()] || Trophy,
    icon_url: siteSettings.value.feature_1_icon_url || '',
    title: siteSettings.value.feature_1_title || '品质保证',
    desc: siteSettings.value.feature_1_desc || '经过严格测试，稳定可靠',
    content_type: siteSettings.value.feature_1_content_type || 'none',
    content_value: siteSettings.value.feature_1_content_value || ''
  },
  {
    icon: iconMap[siteSettings.value.feature_2_icon] || iconMap[siteSettings.value.feature_2_icon?.toLowerCase()] || Star,
    icon_url: siteSettings.value.feature_2_icon_url || '',
    title: siteSettings.value.feature_2_title || '持续更新',
    desc: siteSettings.value.feature_2_desc || '定期更新，功能不断增强',
    content_type: siteSettings.value.feature_2_content_type || 'none',
    content_value: siteSettings.value.feature_2_content_value || ''
  },
  {
    icon: iconMap[siteSettings.value.feature_3_icon] || iconMap[siteSettings.value.feature_3_icon?.toLowerCase()] || Service,
    icon_url: siteSettings.value.feature_3_icon_url || '',
    title: siteSettings.value.feature_3_title || '技术支持',
    desc: siteSettings.value.feature_3_desc || '专业团队提供技术支持',
    content_type: siteSettings.value.feature_3_content_type || 'none',
    content_value: siteSettings.value.feature_3_content_value || ''
  }
])

const maskedPhone = computed(() => {
  const p = phoneInput.value
  if (p.length === 11) {
    return p.slice(0, 3) + '****' + p.slice(7)
  }
  return p
})

const openPayModal = () => {
  showPayModal.value = true
}

const goBackToStep1 = () => {
  payStep.value = 1
  phoneHistory.value = null
  verificationCode.value = ''
  captchaAnswer.value = ''
  // 倒计时延续，不重置
  // 重新加载验证码
  if (phoneInput.value.length === 11) {
    loadCaptcha()
  }
}

const loadCaptcha = async () => {
  captchaLoading.value = true
  try {
    const res = await generateCaptchaAPI()
    if (res?.data) {
      captchaId.value = res.data.id
      captchaImage.value = 'data:image/png;base64,' + res.data.image
    }
  } catch (e) {
    console.error('Failed to load captcha')
  } finally {
    captchaLoading.value = false
  }
}

const openFeatureModal = (item) => {
  if (item.content_type && item.content_type !== 'none') {
    modalFeature.value = item
    showFeatureModal.value = true
  }
}

const downloadFile = (url) => {
  window.open(url, '_blank')
}

// Send verification code
let countdownTimer = null
// 手机号格式校验
const isValidPhone = (phone) => /^1[3-9]\d{9}$/.test(phone)

const sendCode = async () => {
  if (phoneInput.value.length !== 11 || codeCountdown.value > 0) return
  if (!isValidPhone(phoneInput.value)) {
    ElMessage.warning('请输入正确的手机号码')
    return
  }
  if (!captchaAnswer.value) {
    ElMessage.warning('请输入验证码')
    return
  }

  // Verify CAPTCHA first
  try {
    await verifyCaptchaAPI(captchaId.value, captchaAnswer.value)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '验证码错误')
    loadCaptcha() // refresh captcha
    captchaAnswer.value = ''
    return
  }

  // CAPTCHA passed, send SMS
  codeSending.value = true
  try {
    await sendVerificationCodeAPI(phoneInput.value)
    codeSent.value = true
    codeCountdown.value = 60
    countdownTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) {
        clearInterval(countdownTimer)
      }
    }, 1000)
    ElMessage.success('验证码已发送')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '发送失败')
  } finally {
    codeSending.value = false
  }
}

// Check phone and load price
const checkPhone = async () => {
  if (phoneInput.value.length !== 11) {
    ElMessage.warning('请输入11位手机号码')
    return
  }
  if (!verificationCode.value || verificationCode.value.length !== 6) {
    ElMessage.warning('请输入6位验证码')
    return
  }

  // Verify code first
  codeVerifying.value = true
  try {
    await verifyCodeAPI(phoneInput.value, verificationCode.value)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '验证码错误')
    codeVerifying.value = false
    return
  }

  // Then check customer price (priority: customer_prices > order history)
  phoneLoading.value = true
  try {
    // First check customer_prices table
    const priceRes = await getCustomerPriceAPI(phoneInput.value)
    if (priceRes?.data?.price) {
      phoneHistory.value = { amount: priceRes.data.price }
      price.value = priceRes.data.price
    } else {
      // No remembered price, check order history
      const res = await getCustomerLastOrder(phoneInput.value)
      if (res?.data?.amount) {
        phoneHistory.value = res.data
        price.value = res.data.amount
      } else {
        phoneHistory.value = null
        // Use base price or sales link price
        price.value = customPrice.value || basePrice.value
      }
    }
    payStep.value = 2
  } catch (e) {
    phoneHistory.value = null
    price.value = customPrice.value || basePrice.value
    payStep.value = 2
  } finally {
    phoneLoading.value = false
    codeVerifying.value = false
  }
}

onMounted(async () => {
  try {
    const [configRes, settingsRes] = await Promise.allSettled([
      request.get('/config'),
      request.get('/site-settings')
    ])

    if (configRes.status === 'fulfilled' && configRes.value?.data) {
      const data = configRes.value.data
      minPrice.value = parseInt(data.min_price) || 80
      maxPrice.value = parseInt(data.max_price) || 150
      basePrice.value = parseInt(data.base_price) || 99
      price.value = basePrice.value
    }

    if (settingsRes.status === 'fulfilled' && settingsRes.value?.data) {
      const data = settingsRes.value.data
      pageSettings.value = { ...pageSettings.value, ...data }
      siteSettings.value = { ...siteSettings.value, ...data }
    }
  } catch (e) {
    console.error('Failed to load config', e)
  } finally {
    pageLoading.value = false
    // 从首页跳转过来时自动弹出支付弹窗
    if (route.query.auto === '1') {
      openPayModal()
    }
  }
})

const handlePay = async () => {
  if (!phoneInput.value) {
    ElMessage.warning('请输入手机号')
    return
  }

  loading.value = true
  try {
    const res = await request.post('/pay/create', {
      amount: price.value,
      salesCode: salesCode.value,
      phone: phoneInput.value,
      paymentMethod: paymentMethod.value
    })

    // Order created
    orderNo.value = res.data.orderNo

    // Capture payment modes
    wechatPayMode.value = res.data.wechatMode || 'static'
    alipayPayMode.value = res.data.alipayMode || 'static'
    isStaticMode.value = paymentMethod.value === 'wechat' 
      ? wechatPayMode.value === 'static' 
      : alipayPayMode.value === 'static'

    // 优先使用支付宝动态二维码（从订单创建接口返回）
    if (res.data.alipayQrCode) {
      qrCodeImage.value = res.data.alipayQrCode
    } else if (res.data.wechatQrcode && paymentMethod.value === 'wechat') {
      // 微信用管理端配置的静态二维码
      qrCodeImage.value = res.data.wechatQrcode
    } else if (res.data.alipayQrcode && paymentMethod.value === 'alipay') {
      // 支付宝降级用管理端配置的静态二维码
      qrCodeImage.value = res.data.alipayQrcode
    }
    showQrCode.value = true
  } catch (e) {
    ElMessage.error('创建订单失败')
  } finally {
    loading.value = false
  }
}

// Handle paid confirmation
const handlePaid = async () => {
  if (isStaticMode.value) {
    // Static mode: submit to admin for review
    try {
      await request.post('/pay/mark-paid', { orderNo: orderNo.value })
      ElMessage.success('支付确认已提交，请等待管理员审核后自动发货')
    } catch (e) {
      ElMessage.error('提交失败，请重试')
      return
    }
  } else {
    ElMessage.success('支付确认已提交，请等待系统处理')
  }
  showQrCode.value = false
  showPayModal.value = false
  // Reset state
  payStep.value = 1
  phoneInput.value = ''
  verificationCode.value = ''
  captchaAnswer.value = ''
  phoneHistory.value = null
  orderNo.value = ''
  paidSubmitted.value = false
  qrCodeImage.value = ''
  codeSent.value = false
  captchaId.value = ''
  captchaImage.value = ''
}
</script>

<style scoped>
.pay-page {
  min-height: 100vh;
  background: #f8f9fc;
}

/* Hero Section */
.hero {
  background: linear-gradient(135deg, var(--hero-bg) 0%, var(--hero-bg-end) 100%);
  padding: 120px 24px 80px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.hero::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 60%);
  animation: heroFloat 15s ease-in-out infinite;
}

@keyframes heroFloat {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  33% { transform: translate(30px, -30px) rotate(5deg); }
  66% { transform: translate(-20px, 20px) rotate(-3deg); }
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 700px;
  margin: 0 auto;
}

.hero-title {
  font-size: 52px;
  font-weight: 700;
  color: white;
  margin: 0 0 20px;
  letter-spacing: -0.5px;
  line-height: 1.2;
}

.hero-subtitle {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 40px;
  font-weight: 300;
}

.hero-btn {
  padding: 14px 48px;
  font-size: 16px;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.hero-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}

/* Features Section */
.features-section {
  padding: 80px 24px;
  background: #f8f9fc;
}

.features {
  display: flex;
  justify-content: center;
  gap: 32px;
  max-width: 1000px;
  margin: 0 auto;
  flex-wrap: wrap;
}

.feature-card {
  background: white;
  border-radius: 16px;
  padding: 40px 32px;
  text-align: center;
  width: 280px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  cursor: pointer;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: white;
}

.feature-custom-icon {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.feature-card h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 12px;
}

.feature-card p {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
  margin: 0;
}

/* ==================== */
/* Payment Modal Styles */
/* ==================== */

/* Gradient overlay behind the dialog */
.pay-dialog :deep(.el-overlay) {
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.6) 0%, rgba(30, 30, 60, 0.7) 100%) !important;
}

.pay-dialog :deep(.el-dialog) {
  border-radius: 20px;
  max-width: 440px;
  width: calc(100% - 32px);
  margin: 0 auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.pay-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 0;
  margin: 0;
}

.pay-dialog :deep(.el-dialog__body) {
  padding: 0 24px 24px;
}

.pay-dialog :deep(.el-dialog__title) {
  display: none;
}

/* Step indicators in dialog header */
.pay-dialog-header {
  padding-bottom: 8px;
}

.pay-dialog-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.step-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  opacity: 0.4;
  transition: opacity 0.3s ease;
}

.step-indicator.active {
  opacity: 1;
}

.step-indicator.done {
  opacity: 0.7;
}

.step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.step-indicator.active .step-num {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.step-indicator.done .step-num {
  background: #10b981;
  color: white;
}

.step-label {
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
}

.step-indicator.active .step-label {
  color: #1a1a2e;
}

.step-line {
  width: 40px;
  height: 2px;
  background: #e5e7eb;
  margin: 0 12px;
  border-radius: 1px;
  transition: background 0.3s ease;
}

.step-line.active {
  background: linear-gradient(90deg, #10b981, #667eea);
}

/* Dialog body */
.pay-dialog-body {
  min-height: 240px;
}

.pay-step {
  padding-top: 8px;
}

.step-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 16px;
  text-align: center;
  line-height: 1.4;
}

/* Privacy notice */
.privacy-notice {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 20px;
}

.lock-icon {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 1px;
}

.privacy-notice p {
  margin: 0;
  font-size: 12.5px;
  color: #166534;
  line-height: 1.6;
}

/* Phone input */
.phone-input-wrapper {
  margin-bottom: 20px;
}

.phone-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 4px 12px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: box-shadow 0.2s ease;
}

.phone-input :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.4) inset;
}

.phone-input :deep(.el-input__inner) {
  font-size: 16px;
  height: 24px;
}

/* Submit button */
.pay-submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 12px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.pay-submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.pay-submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Verification code section */
.code-section {
  margin-top: 16px;
}

.code-input-row {
  display: flex;
  gap: 12px;
}

.code-btn {
  flex-shrink: 0;
  min-width: 120px;
}

/* CAPTCHA section */
.captcha-section {
  margin-top: 12px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  flex-shrink: 0;
  transition: border-color 0.2s;
}

.captcha-img:hover {
  border-color: #6366f1;
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-loading {
  font-size: 12px;
  color: #909399;
}

/* Price display */
.price-display {
  text-align: center;
  margin-bottom: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 12px;
}

.price-label {
  display: block;
  color: #92400e;
  font-size: 13px;
  margin-bottom: 4px;
}

.price {
  font-size: 42px;
  font-weight: 700;
  color: #dc2626;
  letter-spacing: -1px;
}

/* Sales info */
.sales-info {
  text-align: center;
  margin-bottom: 16px;
}

/* Phone display */
.phone-display {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f8f9fc;
  border-radius: 10px;
  margin-bottom: 20px;
  font-size: 15px;
  color: #374151;
}

.change-phone {
  margin-left: auto;
  font-size: 13px;
}

/* Payment methods */
.payment-methods {
  margin-bottom: 24px;
}

.payment-methods h4 {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin: 0 0 12px;
}

.payment-radio-group {
  width: 100%;
  display: flex;
  gap: 12px;
}

.payment-radio-group :deep(.el-radio-button) {
  flex: 1;
}

.payment-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 10px !important;
  border: 1px solid #dcdfe6;
  box-shadow: none !important;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
}

/* QR Code view styles */
.payment-qr-view {
  text-align: center;
}

.qr-header {
  margin-bottom: 16px;
}

.qr-price {
  margin-bottom: 24px;
}

.qr-amount {
  font-size: 42px;
  font-weight: 700;
  color: #f56c6c;
}

.qr-code-container {
  margin: 0 auto 24px;
  width: 200px;
  height: 200px;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.qr-code-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-code-placeholder {
  text-align: center;
  color: #909399;
}

.qr-code-placeholder p {
  margin-top: 8px;
  font-size: 13px;
}

.qr-info {
  margin-bottom: 24px;
}

.qr-info p {
  font-size: 14px;
  color: #606266;
  margin: 0 0 8px;
}

.qr-order-no {
  font-size: 12px;
  color: #909399;
}

.paid-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.change-method-btn {
  width: 100%;
}

/* Step transition */
.step-fade-enter-active,
.step-fade-leave-active {
  transition: all 0.25s ease;
}

.step-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.step-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* ==================== */
/* Feature Modal        */
/* ==================== */
.modal-content {
  text-align: center;
}

.modal-image {
  max-width: 100%;
  border-radius: 8px;
}

.modal-video {
  max-width: 100%;
  border-radius: 8px;
}

.modal-text {
  font-size: 15px;
  line-height: 1.8;
  color: #374151;
  text-align: left;
  white-space: pre-wrap;
}

/* Skeleton Loading */
.hero-skeleton {
  background: linear-gradient(135deg, #b0b0b0 0%, #909090 100%) !important;
}

.skeleton-line {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  animation: skeletonPulse 1.5s ease-in-out infinite;
}

.skeleton-title {
  width: 320px;
  height: 48px;
  margin: 0 auto 20px;
}

.skeleton-subtitle {
  width: 240px;
  height: 20px;
  margin: 0 auto 40px;
}

.skeleton-btn {
  width: 140px;
  height: 48px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.3);
  margin: 0 auto;
  animation: skeletonPulse 1.5s ease-in-out infinite;
}

@keyframes skeletonPulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* Responsive */
@media (max-width: 768px) {
  .hero {
    padding: 80px 16px 60px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .features {
    flex-direction: column;
    align-items: center;
  }

  .feature-card {
    width: 100%;
    max-width: 320px;
  }

  .pay-dialog :deep(.el-dialog) {
    max-width: calc(100% - 24px);
    margin: 0 auto;
  }

  .step-title {
    font-size: 16px;
  }

  .price {
    font-size: 34px;
  }

  .captcha-img {
    width: 100px;
  }

  .qr-amount {
    font-size: 34px;
  }

  .qr-code-container {
    width: 180px;
    height: 180px;
  }
}
</style>

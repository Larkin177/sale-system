<template>
  <div class="home" v-loading="loading">
    <!-- ==================== 顶部导航栏 ==================== -->
    <nav class="top-nav" v-if="!loading">
      <div class="nav-inner">
        <span class="nav-brand">{{ settings.site_name || 'CC-Installer' }}</span>
        <div class="nav-links">
          <el-button text size="small" @click="$router.push('/orders')">📋 我的订单</el-button>
        </div>
      </div>
    </nav>

    <!-- ==================== Hero Section ==================== -->
    <div
      v-if="!loading"
      class="hero"
      :style="{
        '--hero-bg': settings.hero_bg_color || '#667eea',
        '--hero-bg-end': settings.hero_bg_color_end || '#764ba2'
      }"
    >
      <div class="hero-content">
        <h1 class="hero-title">{{ settings.hero_title || '专业软件工具' }}</h1>
        <p class="hero-subtitle">{{ settings.hero_subtitle || '高效、稳定、安全的解决方案' }}</p>
        <el-button type="primary" size="large" class="hero-btn" @click="openBuyDialog">
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

    <!-- ==================== 下半区：产品介绍 + 使用教程 ==================== -->
    <div class="bottom-section" id="products-section" v-if="!loading">
      <el-tabs v-model="mainTab" class="main-tabs">
        <!-- Tab 1: 产品介绍 -->
        <el-tab-pane name="products">
          <template #label>
            <span class="main-tab-label">
              <el-icon :size="18"><Present /></el-icon>
              产品介绍
            </span>
          </template>

          <div class="tab-content">
            <div class="features-area" v-if="featureList.length > 0">
              <div class="features-grid">
                <div
                  class="feature-card-inline"
                  v-for="(feat, idx) in featureList"
                  :key="idx"
                  @click="openFeatureModal(feat)"
                  :class="{ 'feature-clickable': feat.content_type && feat.content_type !== 'none' }"
                >
                  <div class="feature-icon-inline">
                    <img v-if="feat.icon_url" :src="feat.icon_url" class="feature-custom-icon" />
                    <el-icon v-else :size="24" color="#667eea">
                      <component :is="feat.icon" />
                    </el-icon>
                  </div>
                  <h3 class="feature-title-inline">{{ feat.title }}</h3>
                  <p class="feature-desc-inline">{{ feat.desc }}</p>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 使用教程 -->
        <el-tab-pane name="tutorials">
          <template #label>
            <span class="main-tab-label">
              <el-icon :size="18"><Notebook /></el-icon>
              使用教程
            </span>
          </template>

          <div class="tab-content">
            <div class="tutorials-area" v-if="tutorials.length > 0">
              <el-tabs v-model="tutorialCategory" class="tutorial-sub-tabs">
                <el-tab-pane label="📦 安装教程" name="installer" />
                <el-tab-pane label="🤖 Claude Code" name="claude-code" />
                <el-tab-pane label="⚡ Codex" name="codex" />
              </el-tabs>
              <div class="tutorials-grid">
                <div
                  class="tutorial-card"
                  v-for="t in filteredTutorials"
                  :key="t.id"
                  @click="openTutorial(t)"
                >
                  <div class="tutorial-thumb">
                    <img v-if="t.thumbnailUrl" :src="t.thumbnailUrl" alt="" />
                    <div v-else class="tutorial-thumb-placeholder">
                      <el-icon :size="36"><VideoPlay /></el-icon>
                    </div>
                    <div class="tutorial-badge">
                      <el-tag size="small" :type="t.type === 'video' ? 'danger' : t.type === 'image' ? 'warning' : ''" effect="dark">
                        {{ t.type === 'video' ? '🎬 视频' : t.type === 'image' ? '🖼 图片' : '📝 图文' }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="tutorial-info">
                    <h4>{{ t.title }}</h4>
                    <el-button type="primary" link size="small">
                      查看详情 <el-icon><ArrowRight /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
              <div v-if="filteredTutorials.length === 0" class="tutorials-empty">
                <el-empty description="暂无教程，敬请期待" />
              </div>
            </div>
            <div v-else class="tutorials-empty">
              <el-empty description="教程正在筹备中，敬请期待" />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ==================== Tutorial Detail Dialog ==================== -->
    <el-dialog v-model="tutorialVisible" :title="currentTutorial?.title" width="750px" :close-on-click-modal="true">
      <div v-if="currentTutorial" class="tutorial-detail-body">
        <div v-if="currentTutorial.type === 'video'" class="tutorial-video-wrap">
          <video v-if="currentTutorial.mediaUrl" :src="currentTutorial.mediaUrl" controls style="width:100%;max-height:450px;border-radius:12px;"></video>
          <div v-else class="tutorial-no-media"><el-empty description="视频资源暂未上传" /></div>
        </div>
        <div v-if="currentTutorial.type === 'image'" class="tutorial-image-wrap">
          <img v-if="currentTutorial.mediaUrl" :src="currentTutorial.mediaUrl" style="width:100%;border-radius:12px;" />
          <div v-else class="tutorial-no-media"><el-empty description="图片资源暂未上传" /></div>
        </div>
        <div v-if="currentTutorial.type === 'text'" class="tutorial-content" v-html="currentTutorial.content"></div>
      </div>
    </el-dialog>

    <!-- ==================== Feature Modal ==================== -->
    <el-dialog
      v-model="showModal"
      :title="modalFeature?.title || ''"
      width="600px"
      :close-on-click-modal="true"
      destroy-on-close
    >
      <div class="modal-content" v-if="modalFeature">
        <div v-if="modalFeature.content_type === 'image'" class="modal-media">
          <img :src="modalFeature.content_value" alt="" class="modal-image" />
        </div>
        <div v-else-if="modalFeature.content_type === 'video'" class="modal-media">
          <video :src="modalFeature.content_value" controls class="modal-video"></video>
        </div>
        <div v-else-if="modalFeature.content_type === 'text'" class="modal-text">
          {{ modalFeature.content_value }}
        </div>
        <div v-else-if="modalFeature.content_type === 'download'" class="modal-download">
          <p>点击下方按钮下载文件</p>
          <el-button type="primary" @click="downloadFile(modalFeature.content_value)">
            <el-icon><Download /></el-icon> 下载文件
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="showModal = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 购买流程多步弹窗 ==================== -->
    <el-dialog
      v-model="buyDialogVisible"
      :title="buyStepTitle"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <!-- Step 1: 邮箱 + 图形验证码 -->
      <div v-if="buyStep === 1" class="buy-step">
        <div class="step-indicator">
          <span class="step-dot active">1</span>
          <span class="step-line"></span>
          <span class="step-dot">2</span>
          <span class="step-line"></span>
          <span class="step-dot">3</span>
        </div>

        <h3 class="step-title">验证邮箱</h3>
        <p class="step-desc">请输入您的邮箱并完成图形验证</p>

        <el-input
          v-model="buyEmail"
          placeholder="your@email.com"
          size="large"
          class="buy-input"
        />
        <p class="email-reminder">📨 发货提醒将发送至该邮箱，请务必填写正确</p>

        <div class="captcha-row" v-if="buyEmail">
          <el-input
            v-model="captchaAnswer"
            placeholder="验证码"
            maxlength="4"
            size="large"
            class="captcha-input"
            @keyup.enter="goStep2"
          />
          <div class="captcha-img" @click="loadCaptcha" title="点击刷新">
            <img v-if="captchaImage" :src="captchaImage" alt="captcha" />
            <span v-else>加载中...</span>
          </div>
        </div>
      </div>

      <!-- Step 2: 选择平台 + 产品 -->
      <div v-if="buyStep === 2" class="buy-step">
        <div class="step-indicator">
          <span class="step-dot completed">✓</span>
          <span class="step-line completed"></span>
          <span class="step-dot active">2</span>
          <span class="step-line"></span>
          <span class="step-dot">3</span>
        </div>

        <h3 class="step-title">选择套餐</h3>
        <p class="step-desc">请先选择系统平台，再选择所需产品</p>

        <el-tabs v-model="selectedPlatform" class="platform-tabs">
          <el-tab-pane label="🍎 macOS" name="mac" />
          <el-tab-pane label="🪟 Windows" name="windows" />
        </el-tabs>

        <div class="pkg-grid">
          <div
            class="pkg-card"
            v-for="pkg in filteredPackages"
            :key="pkg.id"
            :class="{ selected: selectedPkg?.id === pkg.id }"
            @click="selectedPkg = pkg"
          >
            <h4>{{ pkg.name }}</h4>
            <div class="pkg-price">¥{{ pkg.price }}</div>
            <p v-if="pkg.description" class="pkg-desc">{{ pkg.description }}</p>
          </div>
        </div>
      </div>

      <!-- Step 3: 支付方式 + 二维码 -->
      <div v-if="buyStep === 3" class="buy-step">
        <div class="step-indicator">
          <span class="step-dot completed">✓</span>
          <span class="step-line completed"></span>
          <span class="step-dot completed">✓</span>
          <span class="step-line completed"></span>
          <span class="step-dot active">3</span>
        </div>

        <h3 class="step-title">选择支付方式</h3>
        <p class="step-desc">请选择支付方式并扫码付款</p>

        <el-tabs v-model="paymentMethod" class="pay-tabs">
          <el-tab-pane label="💚 微信支付" name="wechat" />
          <el-tab-pane label="💙 支付宝" name="alipay" />
        </el-tabs>

        <div class="qr-section">
          <div class="qr-box" v-loading="orderLoading">
            <img v-if="currentQr" :src="currentQr" class="qr-img" />
            <p v-else-if="!orderLoading">暂无二维码</p>
          </div>
          <p class="order-amount">应付金额：<b>¥{{ orderAmount }}</b></p>
          <p class="order-no" v-if="orderNo">订单号：{{ orderNo }}</p>
        </div>
      </div>

      <!-- 底部按钮（统一放在 dialog 的直接子级） -->
      <template #footer>
        <div class="dialog-footer">
          <template v-if="buyStep === 1">
            <el-button @click="closeBuyDialog">取消</el-button>
            <el-button type="primary" size="large" @click="goStep2" :disabled="!canGoStep2" :loading="stepLoading">
              下一步
            </el-button>
          </template>
          <template v-else-if="buyStep === 2">
            <el-button @click="buyStep = 1">上一步</el-button>
            <el-button type="primary" size="large" @click="goStep3" :disabled="!selectedPkg">
              下一步
            </el-button>
          </template>
          <template v-else-if="buyStep === 3">
            <el-button @click="buyStep = 2" :disabled="orderLoading">上一步</el-button>
            <el-button v-if="isStaticMode" type="success" size="large" @click="markPaid" :loading="submitting">
              我已支付
            </el-button>
          </template>
        </div>
      </template>
    </el-dialog>

    <!-- 支付提交成功弹窗 -->
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
            <p>确认后我们会向 <b>{{ buyEmail }}</b> 发送通知邮件</p>
          </div>
        </div>
        <div class="paid-step">
          <div class="paid-step-num">3</div>
          <div class="paid-step-text">
            <strong>回到本站获取授权码</strong>
            <p>访问 <a href="/orders" target="_blank">我的订单</a> 查看授权码并下载</p>
          </div>
        </div>
      </div>
      <div class="paid-countdown"><span>{{ countdown }} 秒后自动关闭</span></div>
      <template #footer>
        <el-button type="primary" @click="closePaidDialog" :disabled="countdown > 0">
          {{ countdown > 0 ? '请等待 ' + countdown + 's' : '我知道了' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Trophy, Star, Service, Coin, Position, Download, Present, Notebook, VideoPlay, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()

// ==================== 页面基础数据 ====================
const loading = ref(true)
const showModal = ref(false)
const modalFeature = ref(null)

const settings = ref({
  hero_title: '专业软件工具',
  hero_subtitle: '高效、稳定、安全的解决方案',
  hero_bg_color: '#667eea',
  hero_bg_color_end: '#764ba2',
  site_name: '',
  feature_1_icon: 'Trophy',
  feature_1_title: '品质保证',
  feature_1_desc: '经过严格测试，稳定可靠',
  feature_2_icon: 'Star',
  feature_2_title: '持续更新',
  feature_3_icon: 'Service',
  feature_2_desc: '定期更新，功能不断增强',
  feature_3_title: '技术支持',
  feature_3_desc: '专业团队提供技术支持',
  feature_1_icon_url: '',
  feature_1_content_type: 'none',
  feature_1_content_value: '',
  feature_2_icon_url: '',
  feature_2_content_type: 'none',
  feature_2_content_value: '',
  feature_3_icon_url: '',
  feature_3_content_type: 'none',
  feature_3_content_value: ''
})

const iconMap = { trophy: Trophy, star: Star, service: Service, coin: Coin, position: Position, Trophy, Star, Service, Coin, Position }

const featureList = computed(() => [
  {
    icon: iconMap[settings.value.feature_1_icon] || iconMap[settings.value.feature_1_icon?.toLowerCase()] || Trophy,
    icon_url: settings.value.feature_1_icon_url || '',
    title: settings.value.feature_1_title || '品质保证',
    desc: settings.value.feature_1_desc || '经过严格测试，稳定可靠',
    content_type: settings.value.feature_1_content_type || 'none',
    content_value: settings.value.feature_1_content_value || ''
  },
  {
    icon: iconMap[settings.value.feature_2_icon] || iconMap[settings.value.feature_2_icon?.toLowerCase()] || Star,
    icon_url: settings.value.feature_2_icon_url || '',
    title: settings.value.feature_2_title || '持续更新',
    desc: settings.value.feature_2_desc || '定期更新，功能不断增强',
    content_type: settings.value.feature_2_content_type || 'none',
    content_value: settings.value.feature_2_content_value || ''
  },
  {
    icon: iconMap[settings.value.feature_3_icon] || iconMap[settings.value.feature_3_icon?.toLowerCase()] || Service,
    icon_url: settings.value.feature_3_icon_url || '',
    title: settings.value.feature_3_title || '技术支持',
    desc: settings.value.feature_3_desc || '专业团队提供技术支持',
    content_type: settings.value.feature_3_content_type || 'none',
    content_value: settings.value.feature_3_content_value || ''
  }
])

const openFeatureModal = (item) => {
  if (!item.content_type || item.content_type === 'none') return
  modalFeature.value = item
  showModal.value = true
}

const downloadFile = (url) => {
  window.open(url, '_blank')
}

// ==================== 下半区 Tab ====================
const mainTab = ref('products')
const tutorials = ref([])
const tutorialCategory = ref('installer')
const tutorialVisible = ref(false)
const currentTutorial = ref(null)

const filteredTutorials = computed(() => {
  return tutorials.value.filter(t => t.category === tutorialCategory.value)
})

async function loadTutorials() {
  try {
    const res = await request.get('/tutorials')
    tutorials.value = res.data || []
  } catch (e) { /* ignore */ }
}

function openTutorial(t) {
  currentTutorial.value = t
  tutorialVisible.value = true
}

// ==================== 购买弹窗流程 ====================
const buyDialogVisible = ref(false)
const buyStep = ref(1)
const stepLoading = ref(false)
const buyEmail = ref(localStorage.getItem('customer_email') || '')

// Captcha
const captchaImage = ref('')
const captchaId = ref('')
const captchaAnswer = ref('')

// Packages
const packages = ref([])
const selectedPlatform = ref('mac')
const selectedPkg = ref(null)

// Payment
const paymentMethod = ref('wechat')
const orderLoading = ref(false)
const orderNo = ref('')
const orderAmount = ref('')
const qrWechat = ref('')
const qrAlipay = ref('')
const qrAlipayDyn = ref('')
const wechatMode = ref('static')
const alipayMode = ref('static')
const submitting = ref(false)
const paidDialogVisible = ref(false)
const countdown = ref(10)
let countdownTimer = null

const buyStepTitle = computed(() => {
  if (buyStep.value === 1) return '验证邮箱'
  if (buyStep.value === 2) return '选择套餐'
  return '支付'
})

const filteredPackages = computed(() => {
  return packages.value.filter(p => p.platform === selectedPlatform.value)
})

const canGoStep2 = computed(() => {
  return buyEmail.value && captchaAnswer.value
})

const currentQr = computed(() => {
  if (paymentMethod.value === 'wechat') return qrWechat.value
  if (alipayMode.value === 'api' && qrAlipayDyn.value) return qrAlipayDyn.value
  return qrAlipay.value
})

const isStaticMode = computed(() => {
  return paymentMethod.value === 'wechat'
    ? wechatMode.value === 'static'
    : alipayMode.value === 'static'
})

async function loadCaptcha() {
  try {
    const res = await request.get('/captcha/generate')
    captchaId.value = res.data?.captchaId || res.data?.id || ''
    captchaImage.value = res.data?.image ? `data:image/png;base64,${res.data.image}` : ''
  } catch (e) {
    console.error('captcha load failed', e)
  }
}

watch(buyEmail, (val) => {
  if (val && !captchaImage.value) loadCaptcha()
})

function openBuyDialog() {
  buyStep.value = 1
  selectedPkg.value = null
  orderNo.value = ''
  orderAmount.value = ''
  qrWechat.value = ''
  qrAlipay.value = ''
  qrAlipayDyn.value = ''
  paymentMethod.value = 'wechat'
  buyDialogVisible.value = true

  // 自动检测平台
  const ua = navigator.userAgent
  selectedPlatform.value = ua.includes('Mac') ? 'mac' : 'windows'

  // 如果已有邮箱则自动加载验证码
  if (buyEmail.value) loadCaptcha()
}

function closeBuyDialog() {
  buyDialogVisible.value = false
}

async function goStep2() {
  if (!buyEmail.value) {
    ElMessage.warning('请输入邮箱')
    return
  }
  if (!captchaAnswer.value) {
    ElMessage.warning('请输入验证码')
    return
  }

  stepLoading.value = true
  try {
    const res = await request.post('/captcha/verify', {
      id: captchaId.value,
      answer: captchaAnswer.value
    })
    if (res.code === 200) {
      localStorage.setItem('customer_email', buyEmail.value)
      buyStep.value = 2
    } else {
      ElMessage.error(res.message || '验证码错误')
      loadCaptcha()
      captchaAnswer.value = ''
    }
  } catch (e) {
    ElMessage.error('验证失败，请重试')
    loadCaptcha()
    captchaAnswer.value = ''
  } finally {
    stepLoading.value = false
  }
}

async function goStep3() {
  if (!selectedPkg.value) {
    ElMessage.warning('请选择一个套餐')
    return
  }

  buyStep.value = 3
  orderLoading.value = true

  try {
    const res = await request.post('/pay/create', {
      packageId: selectedPkg.value.id,
      email: buyEmail.value,
      salesCode: route.query.s || ''
    })
    orderNo.value = res.data.orderNo
    orderAmount.value = res.data.amount
    qrWechat.value = res.data.wechatQrcode || ''
    qrAlipay.value = res.data.alipayQrcode || ''
    qrAlipayDyn.value = res.data.alipayQrCode || ''
    wechatMode.value = res.data.wechatMode || 'static'
    alipayMode.value = res.data.alipayMode || 'static'
  } catch (e) {
    ElMessage.error('创建订单失败，请重试')
    buyStep.value = 2
  } finally {
    orderLoading.value = false
  }
}

async function markPaid() {
  submitting.value = true
  try {
    await request.post('/pay/mark-paid', { orderNo: orderNo.value })
    paidDialogVisible.value = true
    countdown.value = 10
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(countdownTimer)
    }, 1000)
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

function closePaidDialog() {
  if (countdownTimer) clearInterval(countdownTimer)
  paidDialogVisible.value = false
  buyDialogVisible.value = false
}

// ==================== 加载数据 ====================
onMounted(async () => {
  try {
    // 并行加载站点配置、套餐列表、教程
    const [siteRes, pkgRes] = await Promise.all([
      request.get('/site-settings').catch(() => ({ data: {} })),
      request.get('/pay/packages').catch(() => ({ data: [] })),
      loadTutorials()
    ])
    if (siteRes?.data) {
      settings.value = { ...settings.value, ...siteRes.data }
    }
    if (pkgRes?.data) {
      packages.value = pkgRes.data
    }
  } catch (e) {
    console.error('Failed to load page data', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home {
  min-height: 100vh;
}

/* ==================== Hero Section ==================== */
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

/* ==================== 购买弹窗 ==================== */
.buy-step {
  min-height: 200px;
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.step-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  border: 2px solid #d1d5db;
  color: #9ca3af;
  background: white;
}

.step-dot.active {
  border-color: #667eea;
  background: #667eea;
  color: white;
}

.step-dot.completed {
  border-color: #22c55e;
  background: #22c55e;
  color: white;
  font-size: 16px;
}

.step-line {
  width: 60px;
  height: 2px;
  background: #d1d5db;
  margin: 0 8px;
}

.step-line.completed {
  background: #22c55e;
}

.step-title {
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 6px;
}

.step-desc {
  text-align: center;
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 20px;
}

.buy-input {
  margin-bottom: 4px;
}

.email-reminder {
  font-size: 13px;
  color: #f59e0b;
  margin: 6px 0 16px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 130px;
  height: 44px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #d1d5db;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-img span {
  font-size: 12px;
  color: #9ca3af;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
}

/* ==================== 套餐选择 ==================== */
.platform-tabs {
  margin-bottom: 16px;
}

.platform-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.pkg-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.pkg-card {
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.pkg-card:hover {
  border-color: #667eea;
}

.pkg-card.selected {
  border-color: #667eea;
  background: #eef2ff;
}

.pkg-card h4 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.pkg-price {
  font-size: 28px;
  font-weight: 700;
  color: #667eea;
}

.pkg-desc {
  font-size: 12px;
  color: #6b7280;
  margin: 6px 0 0;
}

/* ==================== 支付 ==================== */
.qr-section {
  text-align: center;
  padding: 16px 0;
}

.qr-box {
  width: 220px;
  height: 220px;
  margin: 0 auto 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.qr-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.order-amount {
  font-size: 18px;
  color: #1f2937;
  margin: 0 0 4px;
}

.order-amount b {
  color: #667eea;
  font-size: 22px;
}

.order-no {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

/* ==================== 支付成功弹窗 ==================== */
.paid-dialog-body { padding: 8px 0; }

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

.paid-step-text strong { display: block; font-size: 15px; margin-bottom: 2px; }
.paid-step-text p { margin: 0; font-size: 13px; color: #666; }
.paid-countdown { text-align: center; font-size: 13px; color: #999; margin-top: 8px; }

/* ==================== Top Nav ==================== */
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
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ==================== Bottom Combined Section ==================== */
.bottom-section {
  max-width: 1200px;
  margin: -30px auto 60px;
  padding: 0 24px;
  position: relative;
  z-index: 2;
}

.main-tabs {
  background: white;
  border-radius: 20px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.06);
  overflow: hidden;
}

.main-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 32px;
  background: linear-gradient(135deg, #f8faff 0%, #f5f3ff 100%);
  border-bottom: 1px solid #eef0f6;
}

.main-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }

.main-tabs :deep(.el-tabs__item) {
  height: 60px;
  line-height: 60px;
  font-size: 16px;
  font-weight: 600;
  padding: 0 28px;
  color: #6b7280;
  transition: all 0.25s;
}

.main-tabs :deep(.el-tabs__item.is-active) { color: #667eea; }

.main-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px;
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.main-tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.main-tabs :deep(.el-tabs__content) { padding: 40px 32px; }

.tab-content { min-height: 200px; }

/* Features Grid (inline in tab) */
.features-area { margin-bottom: 16px; }

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card-inline {
  background: #f8faff;
  border: 2px solid #eef0f6;
  border-radius: 16px;
  padding: 32px 24px;
  text-align: center;
  transition: all 0.3s ease;
}

.feature-card-inline:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(102,126,234,0.1);
  border-color: #e0e7ff;
}

.feature-card-inline.feature-clickable { cursor: pointer; }
.feature-card-inline.feature-clickable:hover { border-color: #667eea; }

.feature-icon-inline {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.feature-title-inline {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px;
}

.feature-desc-inline {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
  line-height: 1.6;
}

/* Tutorials */
.tutorial-sub-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 28px;
}

.tutorial-sub-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }

.tutorial-sub-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  padding: 0 20px;
  height: 40px;
  line-height: 40px;
}

.tutorials-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.tutorial-card {
  background: white;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #f3f4f6;
  cursor: pointer;
  transition: all 0.25s;
}

.tutorial-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.tutorial-thumb {
  height: 170px;
  background: linear-gradient(135deg, #eef0f6 0%, #e8ecf4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.tutorial-thumb img { width: 100%; height: 100%; object-fit: cover; }
.tutorial-thumb-placeholder { color: #9ca3af; }
.tutorial-badge { position: absolute; top: 10px; right: 10px; }

.tutorial-info {
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tutorial-info h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tutorials-empty { padding: 48px 0; }

.tutorial-detail-body { line-height: 1.8; }
.tutorial-no-media { padding: 40px 0; text-align: center; }
.tutorial-content { font-size: 15px; color: #374151; line-height: 1.9; }
.tutorial-content :deep(img) { max-width: 100%; border-radius: 8px; margin: 12px 0; }

/* ==================== Skeleton Loading ==================== */
.hero-skeleton {
  background: linear-gradient(135deg, #b0b0b0 0%, #909090 100%) !important;
}

.skeleton-line {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  animation: skeletonPulse 1.5s ease-in-out infinite;
}

.skeleton-title { width: 320px; height: 48px; margin: 0 auto 20px; }
.skeleton-subtitle { width: 240px; height: 20px; margin: 0 auto 40px; }
.skeleton-btn { width: 140px; height: 48px; border-radius: 8px; background: rgba(255, 255, 255, 0.3); margin: 0 auto; animation: skeletonPulse 1.5s ease-in-out infinite; }

@keyframes skeletonPulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* ==================== Responsive ==================== */
@media (max-width: 768px) {
  .hero { padding: 80px 16px 60px; }
  .hero-title { font-size: 32px; }
  .hero-subtitle { font-size: 16px; }

  .bottom-section { margin: -20px 16px 40px; padding: 0; }
  .main-tabs :deep(.el-tabs__content) { padding: 24px 16px; }
  .main-tabs :deep(.el-tabs__header) { padding: 0 16px; }
  .main-tabs :deep(.el-tabs__item) { padding: 0 16px; font-size: 14px; }
  .features-grid { grid-template-columns: 1fr; }
  .tutorials-grid { grid-template-columns: 1fr; }

  .step-line { width: 30px; }
  .pkg-grid { grid-template-columns: 1fr; }
}
</style>

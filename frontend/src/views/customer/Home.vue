<template>
  <div class="home" v-loading="loading">
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
        <el-button type="primary" size="large" class="hero-btn" @click="scrollToProducts">
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

    <!-- ==================== 合并区：产品介绍 + 使用教程 ==================== -->
    <div class="combined-section" id="products-section">
      <el-tabs v-model="mainTab" class="main-tabs">
        <!-- ===== Tab 1: 产品介绍 ===== -->
        <el-tab-pane name="products">
          <template #label>
            <span class="main-tab-label">
              <el-icon :size="20"><Present /></el-icon>
              产品介绍
            </span>
          </template>

          <!-- 功能特性卡片 -->
          <div class="features-area" v-if="featureList.length > 0">
            <div class="features-grid">
              <div
                class="feature-card"
                v-for="(feat, idx) in featureList"
                :key="idx"
                @click="openFeatureModal(feat)"
                :class="{ 'feature-clickable': feat.content_type && feat.content_type !== 'none' }"
              >
                <div class="feature-icon">
                  <img v-if="feat.icon_url" :src="feat.icon_url" class="feature-custom-icon" />
                  <el-icon v-else :size="24" color="#667eea">
                    <component :is="feat.icon" />
                  </el-icon>
                </div>
                <h3 class="feature-title">{{ feat.title }}</h3>
                <p class="feature-desc">{{ feat.desc }}</p>
              </div>
            </div>
          </div>

          <!-- 套餐定价卡片 -->
          <div class="packages-area">
            <h2 class="area-title">选择您的套餐</h2>
            <p class="area-subtitle">支持 macOS 和 Windows 双平台</p>
            <div class="packages-grid">
              <div
                class="package-card"
                :class="{ 'package-featured': pkg.featured }"
                v-for="pkg in groupedPackages"
                :key="pkg.name"
              >
                <div v-if="pkg.featured" class="package-ribbon">推荐</div>
                <div class="package-header">
                  <h3>{{ pkg.name }}</h3>
                  <p class="package-desc">{{ pkg.description }}</p>
                </div>
                <div class="package-price">
                  <span class="price-symbol">¥</span>
                  <span class="price-value">{{ pkg.basePrice }}</span>
                  <span class="price-range">/ {{ pkg.priceRange }}</span>
                </div>
                <div class="package-platforms">
                  <el-tag
                    v-for="plat in pkg.platforms"
                    :key="plat.name"
                    :type="plat.name === 'mac' ? '' : 'success'"
                    effect="plain"
                    size="small"
                  >
                    {{ plat.name === 'mac' ? '🍎 macOS' : '🪟 Windows' }}
                    <span class="plat-price">¥{{ plat.price }}</span>
                  </el-tag>
                </div>
                <el-button
                  type="primary"
                  size="large"
                  class="package-buy-btn"
                  @click="buyPackage(pkg)"
                >
                  立即购买
                </el-button>
              </div>
            </div>
          </div>

          </el-tab-pane>

        <!-- ===== Tab 2: 使用教程 ===== -->
        <el-tab-pane name="tutorials">
          <template #label>
            <span class="main-tab-label">
              <el-icon :size="20"><Notebook /></el-icon>
              使用教程
            </span>
          </template>

          <div class="tutorials-area" v-if="tutorials.length > 0">
            <h2 class="area-title">快速上手</h2>
            <p class="area-subtitle">从安装到精通，一步到位</p>
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
        </el-tab-pane>
      </el-tabs>
    </div>

        <!-- ==================== 购买弹窗 ==================== -->
    <el-dialog
      v-model="buyDialogVisible"
      title="确认邮箱"
      width="420px"
      :close-on-click-modal="true"
    >
      <div class="buy-dialog" v-if="selectedPackage">
        <div class="buy-dialog-header">
          <h3>{{ selectedPackage.name }}</h3>
          <p>{{ selectedPackage.description }}</p>
        </div>
        <div class="buy-dialog-email">
          <label class="buy-dialog-label">请输入您的邮箱</label>
          <el-input
            v-model="buyEmail"
            placeholder="your@email.com"
            size="large"
            class="buy-email-input"
          />
          <p class="buy-email-reminder">📨 发货提醒将发送至该邮箱，请务必填写正确</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="buyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEmail" :disabled="!buyEmail">
          下一步
        </el-button>
      </template>
    </el-dialog>

<!-- ==================== Feature 弹窗 ==================== -->
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

    <!-- ==================== Tutorial 弹窗 ==================== -->
    <el-dialog v-model="tutorialVisible" :title="currentTutorial?.title" width="750px" :close-on-click-modal="true">
      <div v-if="currentTutorial" class="tutorial-detail-body">
        <div v-if="currentTutorial.type === 'video'" class="tutorial-video-wrap">
          <video v-if="currentTutorial.mediaUrl" :src="currentTutorial.mediaUrl" controls style="width: 100%; max-height: 450px; border-radius: 12px;"></video>
          <div v-else class="tutorial-no-media">
            <el-empty description="视频资源暂未上传" />
          </div>
        </div>
        <div v-if="currentTutorial.type === 'image'" class="tutorial-image-wrap">
          <img v-if="currentTutorial.mediaUrl" :src="currentTutorial.mediaUrl" style="width: 100%; border-radius: 12px;" />
          <div v-else class="tutorial-no-media">
            <el-empty description="图片资源暂未上传" />
          </div>
        </div>
        <div v-if="currentTutorial.type === 'text'" class="tutorial-content" v-html="currentTutorial.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Trophy, Star, Service, Coin, Position, Download, VideoPlay, Notebook, ArrowRight, Present } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(true)
const mainTab = ref('products')
const showModal = ref(false)
const modalFeature = ref(null)
const buyDialogVisible = ref(false)
const selectedPackage = ref(null)
const buyEmail = ref(localStorage.getItem('customer_email') || '')
const selectedPlatform = ref('')

// Tutorials
const tutorials = ref([])
const tutorialCategory = ref('installer')
const tutorialVisible = ref(false)
const currentTutorial = ref(null)

// Packages
const packages = ref([])

const settings = ref({
  hero_title: '专业软件工具',
  hero_subtitle: '高效、稳定、安全的解决方案',
  hero_bg_color: '#667eea',
  hero_bg_color_end: '#764ba2',
  feature_1_icon: 'Trophy',
  feature_1_title: '品质保证',
  feature_1_desc: '经过严格测试，稳定可靠',
  feature_1_icon_url: '',
  feature_1_content_type: 'none',
  feature_1_content_value: '',
  feature_2_icon: 'Star',
  feature_2_title: '持续更新',
  feature_2_desc: '定期更新，功能不断增强',
  feature_2_icon_url: '',
  feature_2_content_type: 'none',
  feature_2_content_value: '',
  feature_3_icon: 'Service',
  feature_3_title: '技术支持',
  feature_3_desc: '专业团队提供技术支持',
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

// Group packages by product
const groupedPackages = computed(() => {
  // Group by name prefix (strip platform suffix)
  const groups = {}
  for (const pkg of packages.value) {
    // Extract base name: "Claude Code Mac" -> "Claude Code"
    const baseName = pkg.name.replace(/\s+(Mac|Windows)$/i, '')
    if (!groups[baseName]) {
      groups[baseName] = {
        name: baseName,
        description: pkg.description || '',
        basePrice: pkg.price,
        minPrice: pkg.minPrice,
        maxPrice: pkg.maxPrice,
        platforms: [],
        productId: pkg.productId
      }
    }
    groups[baseName].platforms.push({
      name: pkg.platform,
      price: pkg.price,
      packageId: pkg.id,
      minPrice: pkg.minPrice,
      maxPrice: pkg.maxPrice
    })
    // Keep the lowest base price for display
    if (pkg.price < groups[baseName].basePrice) {
      groups[baseName].basePrice = pkg.price
    }
  }
  const result = Object.values(groups)
  // Mark the bundle as featured
  for (const g of result) {
    if (g.name.includes('+')) {
      g.featured = true
      g.priceRange = '¥' + g.minPrice + ' - ¥' + g.maxPrice
    } else {
      g.priceRange = '¥' + g.minPrice + ' - ¥' + g.maxPrice
    }
  }
  return result
})

const filteredTutorials = computed(() => {
  return tutorials.value.filter(t => t.category === tutorialCategory.value)
})

const openFeatureModal = (item) => {
  if (!item.content_type || item.content_type === 'none') return
  modalFeature.value = item
  showModal.value = true
}

const downloadFile = (url) => {
  window.open(url, '_blank')
}

const scrollToProducts = () => {
  mainTab.value = 'products'
  document.getElementById('products-section')?.scrollIntoView({ behavior: 'smooth' })
}

const buyPackage = (pkg) => {
  selectedPackage.value = pkg
  buyDialogVisible.value = true
}

const loadTutorials = async () => {
  try {
    const res = await request.get('/tutorials')
    tutorials.value = res.data || []
  } catch (e) { /* ignore */ }
}

const loadPackages = async () => {
  try {
    const res = await request.get('/pay/packages')
    packages.value = res.data || []
  } catch (e) { /* ignore */ }
}

const openTutorial = (t) => {
  currentTutorial.value = t
  tutorialVisible.value = true
}

const confirmBuy = () => {
  if (!selectedPlatform.value || !selectedPackage.value) return
  const plat = selectedPackage.value.platforms.find(p => p.name === selectedPlatform.value)
  buyDialogVisible.value = false
  router.push({
    path: '/pay',
    query: {
      auto: '1',
      packageId: plat?.packageId,
      productName: selectedPackage.value.name,
      platform: selectedPlatform.value
    }
  })
}

onMounted(async () => {
  loadTutorials()
  loadPackages()
  try {
    const res = await request.get('/site-settings')
    if (res?.data) {
      settings.value = { ...settings.value, ...res.data }
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
  background: #fafbfc;
}

/* ==================== Hero ==================== */
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

/* ==================== Combined Section ==================== */
.combined-section {
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

.main-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.main-tabs :deep(.el-tabs__item) {
  height: 60px;
  line-height: 60px;
  font-size: 16px;
  font-weight: 600;
  padding: 0 28px;
  color: #6b7280;
  transition: all 0.25s;
}

.main-tabs :deep(.el-tabs__item.is-active) {
  color: #667eea;
}

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

.main-tabs :deep(.el-tabs__content) {
  padding: 40px 32px;
}

/* ==================== Features ==================== */
.features-area {
  margin-bottom: 48px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  background: #f8faff;
  border: 2px solid #eef0f6;
  border-radius: 16px;
  padding: 32px 24px;
  text-align: center;
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(102,126,234,0.1);
  border-color: #e0e7ff;
}

.feature-clickable {
  cursor: pointer;
}

.feature-clickable:hover {
  border-color: #667eea;
}

.feature-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.feature-custom-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.feature-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px;
}

.feature-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .features-grid {
    grid-template-columns: 1fr;
  }
  .feature-card {
    max-width: 320px;
    margin: 0 auto;
  }
}

/* ==================== Packages ==================== */
.area-title {
  text-align: center;
  font-size: 28px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.area-subtitle {
  text-align: center;
  font-size: 15px;
  color: #9ca3af;
  margin: 0 0 36px;
}

.packages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 48px;
}

.package-card {
  background: #fafbfc;
  border-radius: 16px;
  padding: 32px 24px;
  text-align: center;
  border: 2px solid #eef0f6;
  transition: all 0.3s;
  position: relative;
}

.package-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(102,126,234,0.12);
  border-color: #667eea;
}

.package-featured {
  background: linear-gradient(135deg, #f8f6ff 0%, #f0edff 100%);
  border-color: #a78bfa;
}

.package-ribbon {
  position: absolute;
  top: 12px;
  right: -8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 16px;
  border-radius: 4px 0 0 4px;
}

.package-header h3 {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 6px;
}

.package-desc {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

.package-price {
  margin: 20px 0 16px;
}

.price-symbol {
  font-size: 18px;
  font-weight: 500;
  color: #667eea;
  vertical-align: top;
}

.price-value {
  font-size: 42px;
  font-weight: 800;
  color: #1a1a2e;
  line-height: 1;
}

.price-range {
  font-size: 13px;
  color: #9ca3af;
}

.package-platforms {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.plat-price {
  font-weight: 600;
  margin-left: 4px;
}

.package-buy-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
}


/* ==================== Buy Dialog ==================== */
.buy-dialog-header {
  text-align: center;
  margin-bottom: 24px;
}

.buy-dialog-header h3 {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 6px;
}

.buy-dialog-header p {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

.buy-dialog-platforms {
  display: flex;
  gap: 12px;
}

.buy-platform-option {
  flex: 1;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.buy-platform-option:hover {
  border-color: #a5b4fc;
}

.buy-platform-option.active {
  border-color: #667eea;
  background: #f5f3ff;
}

.plat-label {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.plat-price {
  display: block;
  font-size: 22px;
  font-weight: 800;
  color: #667eea;
}

/* ==================== Tutorials ==================== */
.tutorial-sub-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 28px;
}

.tutorial-sub-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

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

.tutorial-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.tutorial-thumb-placeholder {
  color: #9ca3af;
}

.tutorial-badge {
  position: absolute;
  top: 10px;
  right: 10px;
}

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

.tutorials-empty {
  padding: 48px 0;
}

/* ==================== Modals ==================== */
.modal-media { text-align: center; }
.modal-image { max-width: 100%; border-radius: 8px; }
.modal-video { max-width: 100%; border-radius: 8px; }
.modal-text { font-size: 15px; line-height: 1.8; color: #374151; white-space: pre-wrap; }
.modal-download { text-align: center; padding: 20px 0; }
.modal-download p { color: #6b7280; margin-bottom: 16px; }

.tutorial-detail-body { line-height: 1.8; }
.tutorial-no-media { padding: 40px 0; text-align: center; }
.tutorial-content { font-size: 15px; color: #374151; line-height: 1.9; }
.tutorial-content :deep(img) { max-width: 100%; border-radius: 8px; margin: 12px 0; }

/* ==================== Skeleton ==================== */
.hero-skeleton {
  background: linear-gradient(135deg, #b0b0b0 0%, #909090 100%) !important;
}

.skeleton-line {
  background: rgba(255,255,255,0.3);
  border-radius: 8px;
  animation: skeletonPulse 1.5s ease-in-out infinite;
}

.skeleton-title { width: 320px; height: 48px; margin: 0 auto 20px; }
.skeleton-subtitle { width: 240px; height: 20px; margin: 0 auto 40px; }
.skeleton-btn { width: 140px; height: 48px; border-radius: 8px; background: rgba(255,255,255,0.3); margin: 0 auto; animation: skeletonPulse 1.5s ease-in-out infinite; }

@keyframes skeletonPulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* ==================== Responsive ==================== */
@media (max-width: 768px) {
  .hero { padding: 80px 16px 60px; }
  .hero-title { font-size: 32px; }
  .hero-subtitle { font-size: 16px; }

  .combined-section { margin: -20px 16px 40px; padding: 0; }
  .main-tabs :deep(.el-tabs__content) { padding: 24px 16px; }
  .main-tabs :deep(.el-tabs__header) { padding: 0 16px; }
  .main-tabs :deep(.el-tabs__item) { padding: 0 16px; font-size: 14px; }

  .packages-grid { grid-template-columns: 1fr; }
  .features-grid { flex-direction: column; align-items: center; }
  .feature-card { width: 100%; max-width: 320px; }
  .tutorials-grid { grid-template-columns: 1fr; }
}
</style>

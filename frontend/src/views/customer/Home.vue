<template>
  <div class="home" v-loading="loading">
    <!-- Hero Section -->
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
        <el-button type="primary" size="large" class="hero-btn" @click="$router.push('/pay?auto=1')">
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
    <div class="features-section">
      <div class="features">
        <div
          class="feature-card"
          :class="{ 'feature-clickable': item.content_type !== 'none' }"
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

    <!-- Feature Modal -->
    <el-dialog
      v-model="showModal"
      :title="modalFeature?.title || ''"
      width="600px"
      :close-on-click-modal="true"
      destroy-on-close
    >
      <div class="modal-content" v-if="modalFeature">
        <!-- Image -->
        <div v-if="modalFeature.content_type === 'image'" class="modal-media">
          <img :src="modalFeature.content_value" alt="" class="modal-image" />
        </div>
        <!-- Video -->
        <div v-else-if="modalFeature.content_type === 'video'" class="modal-media">
          <video
            :src="modalFeature.content_value"
            controls
            class="modal-video"
          ></video>
        </div>
        <!-- Text -->
        <div v-else-if="modalFeature.content_type === 'text'" class="modal-text">
          {{ modalFeature.content_value }}
        </div>
        <!-- Download -->
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Trophy, Star, Service, Coin, Position, Download } from '@element-plus/icons-vue'
import request from '@/utils/request'

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

onMounted(async () => {
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
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.feature-clickable {
  cursor: pointer;
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

/* Modal Content */
.modal-media {
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
  white-space: pre-wrap;
}

.modal-download {
  text-align: center;
  padding: 20px 0;
}

.modal-download p {
  color: #6b7280;
  margin-bottom: 16px;
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
}
</style>

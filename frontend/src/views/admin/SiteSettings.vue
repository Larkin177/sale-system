<template>
  <AdminLayout>
    <div class="site-settings-page">
      <div class="settings-content">
        <h2 class="page-title">站点设置</h2>

        <!-- Basic Info -->
        <el-card class="settings-card" shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon><InfoFilled /></el-icon>
              <span>基本信息</span>
            </div>
          </template>
          <el-form :model="settings" label-width="100px" label-position="left">
            <el-form-item label="站点名称">
              <el-input v-model="settings.site_name" placeholder="输入站点名称" />
            </el-form-item>
            <el-form-item label="站点副标题">
              <el-input v-model="settings.site_subtitle" placeholder="输入站点副标题" />
            </el-form-item>
            <el-form-item label="页脚文字">
              <el-input v-model="settings.footer_text" placeholder="输入页脚文字" />
            </el-form-item>
          </el-form>
        </el-card>

        <!-- Hero Section -->
        <el-card class="settings-card" shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon><Picture /></el-icon>
              <span>首页横幅区</span>
            </div>
          </template>
          <el-form :model="settings" label-width="100px" label-position="left">
            <el-form-item label="主标题">
              <el-input v-model="settings.hero_title" placeholder="输入主标题" />
            </el-form-item>
            <el-form-item label="副标题">
              <el-input v-model="settings.hero_subtitle" placeholder="输入副标题" />
            </el-form-item>
            <el-form-item label="渐变起始色">
              <div class="color-picker-row">
                <el-color-picker v-model="settings.hero_bg_color" show-alpha />
                <el-input v-model="settings.hero_bg_color" placeholder="#667eea" class="color-input" />
              </div>
            </el-form-item>
            <el-form-item label="渐变结束色">
              <div class="color-picker-row">
                <el-color-picker v-model="settings.hero_bg_color_end" show-alpha />
                <el-input v-model="settings.hero_bg_color_end" placeholder="#764ba2" class="color-input" />
              </div>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- Features -->
        <el-card class="settings-card" shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon><Grid /></el-icon>
              <span>功能特性</span>
            </div>
          </template>
          <div v-for="(feat, idx) in [1, 2, 3]" :key="feat" class="feature-block">
            <h4 class="feature-label">特性 {{ feat }}</h4>
            <el-form :model="settings" label-width="100px" label-position="left">
              <el-form-item label="图标">
                <el-select v-model="settings[`feature_${feat}_icon`]" style="width: 100%">
                  <el-option label="Trophy (奖杯)" value="Trophy" />
                  <el-option label="Star (星星)" value="Star" />
                  <el-option label="Service (客服)" value="Service" />
                  <el-option label="Coin (金币)" value="Coin" />
                  <el-option label="Position (定位)" value="Position" />
                </el-select>
              </el-form-item>
              <el-form-item label="自定义图标">
                <el-input v-model="settings[`feature_${feat}_icon_url`]" placeholder="输入图标图片URL（可选，设置后覆盖上方图标）" />
              </el-form-item>
              <el-form-item label="标题">
                <el-input v-model="settings[`feature_${feat}_title`]" :placeholder="`特性 ${feat} 标题`" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="settings[`feature_${feat}_desc`]" type="textarea" :rows="2" :placeholder="`特性 ${feat} 描述`" />
              </el-form-item>
              <el-form-item label="内容类型">
                <el-select v-model="settings[`feature_${feat}_content_type`]" style="width: 100%">
                  <el-option label="无内容" value="none" />
                  <el-option label="图片" value="image" />
                  <el-option label="视频" value="video" />
                  <el-option label="文本" value="text" />
                  <el-option label="下载链接" value="download" />
                </el-select>
              </el-form-item>
              <el-form-item label="内容值" v-if="settings[`feature_${feat}_content_type`] && settings[`feature_${feat}_content_type`] !== 'none'">
                <el-input
                  v-model="settings[`feature_${feat}_content_value`]"
                  :placeholder="getContentPlaceholder(settings[`feature_${feat}_content_type`])"
                />
              </el-form-item>
            </el-form>
            <el-divider v-if="feat < 3" />
          </div>
        </el-card>

        <!-- Page Titles -->
        <el-card class="settings-card" shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon><Document /></el-icon>
              <span>页面标题</span>
            </div>
          </template>
          <el-form :model="settings" label-width="100px" label-position="left">
            <el-form-item label="购买页标题">
              <el-input v-model="settings.pay_title" placeholder="购买软件工具" />
            </el-form-item>
            <el-form-item label="购买页副标题">
              <el-input v-model="settings.pay_subtitle" placeholder="输入购买页副标题" />
            </el-form-item>
            <el-form-item label="下载页标题">
              <el-input v-model="settings.download_title" placeholder="下载软件" />
            </el-form-item>
            <el-form-item label="下载页副标题">
              <el-input v-model="settings.download_subtitle" placeholder="输入下载页副标题" />
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 支付二维码 -->
        <el-card class="settings-card">
          <template #header>
            <div class="card-header">
              <el-icon><Picture /></el-icon>
              <span>支付二维码</span>
            </div>
          </template>
          <el-form label-position="top">
            <el-form-item label="微信收款码">
              <el-input v-model="settings.wechat_qrcode" placeholder="输入微信收款码图片URL" />
              <div class="field-hint">客户扫码后付款到您的微信账户</div>
            </el-form-item>
            <el-form-item label="支付宝收款码">
              <el-input v-model="settings.alipay_qrcode" placeholder="输入支付宝收款码图片URL" />
              <div class="field-hint">客户扫码后付款到您的支付宝账户</div>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- Save Button -->
        <div class="save-bar">
          <el-button type="primary" size="large" @click="saveSettings" :loading="saving">
            <el-icon><Check /></el-icon> 保存设置
          </el-button>
        </div>
      </div>

      <!-- Live Preview Panel -->
      <div class="preview-panel">
        <h3 class="preview-title">实时预览</h3>
        <div class="preview-frame">
          <div
            class="preview-hero"
            :style="{
              background: `linear-gradient(135deg, ${settings.hero_bg_color || '#667eea'} 0%, ${settings.hero_bg_color_end || '#764ba2'} 100%)`
            }"
          >
            <h1 class="preview-hero-title">{{ settings.hero_title || '专业软件工具' }}</h1>
            <p class="preview-hero-subtitle">{{ settings.hero_subtitle || '高效、稳定、安全的解决方案' }}</p>
          </div>
          <div class="preview-features">
            <div class="preview-feature" v-for="i in 3" :key="i">
              <div class="preview-feature-icon">
                <el-icon :size="20" color="#667eea">
                  <component :is="iconMap[settings[`feature_${i}_icon`]] || Trophy" />
                </el-icon>
              </div>
              <div class="preview-feature-text">
                <strong>{{ settings[`feature_${i}_title`] || `特性 ${i}` }}</strong>
                <p>{{ settings[`feature_${i}_desc`] || '特性描述' }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { ElMessage } from 'element-plus'
import { Trophy, Star, Service, Coin, Position, InfoFilled, Picture, Grid, Document, Check } from '@element-plus/icons-vue'
import { getAdminSiteSettings, updateSiteSettings } from '@/api/config'
import { refreshSiteName } from '@/router/index.js'

const iconMap = { Trophy, Star, Service, Coin, Position }
const saving = ref(false)

const settings = ref({
  site_name: '',
  site_subtitle: '',
  footer_text: '',
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
  feature_3_content_value: '',
  pay_title: '购买软件工具',
  pay_subtitle: '',
  download_title: '下载软件',
  download_subtitle: '',
  wechat_qrcode: '',
  alipay_qrcode: ''
})

const getContentPlaceholder = (type) => {
  const map = {
    image: '输入图片URL，如 https://example.com/image.png',
    video: '输入视频URL，如 https://example.com/video.mp4',
    text: '输入显示的文本内容',
    download: '输入下载链接URL，如 https://example.com/file.zip'
  }
  return map[type] || ''
}

onMounted(async () => {
  try {
    const res = await getAdminSiteSettings()
    if (res.data) {
      settings.value = { ...settings.value, ...res.data }
    }
  } catch (e) {
    console.error('Failed to load site settings', e)
  }
})

const saveSettings = async () => {
  saving.value = true
  try {
    await updateSiteSettings(settings.value)
    await refreshSiteName()
    ElMessage.success('站点设置保存成功')
  } catch (e) {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.site-settings-page {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.settings-content {
  flex: 1;
  min-width: 0;
}

.preview-panel {
  width: 340px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 24px;
}

.settings-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  font-size: 15px;
  color: #374151;
}

.field-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.feature-block {
  margin-bottom: 0;
}

.feature-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin: 0 0 12px;
}

.color-picker-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-input {
  width: 140px;
}

.save-bar {
  padding: 20px 0;
}

/* Preview Panel */
.preview-title {
  font-size: 15px;
  font-weight: 500;
  color: #374151;
  margin: 0 0 12px;
  text-align: center;
}

.preview-frame {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #ebeef5;
}

.preview-hero {
  padding: 32px 20px;
  text-align: center;
}

.preview-hero-title {
  font-size: 18px;
  font-weight: 600;
  color: white;
  margin: 0 0 6px;
}

.preview-hero-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.preview-features {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-feature {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 8px;
  background: #f8f9fc;
}

.preview-feature-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.preview-feature-text strong {
  font-size: 12px;
  color: #1a1a2e;
  display: block;
}

.preview-feature-text p {
  font-size: 10px;
  color: #6b7280;
  margin: 2px 0 0;
}

@media (max-width: 1024px) {
  .site-settings-page {
    flex-direction: column;
  }

  .preview-panel {
    width: 100%;
    position: static;
  }
}
</style>

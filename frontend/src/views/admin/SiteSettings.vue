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
                <el-select v-model="settings[`feature_${feat}_icon`]" style="width: 100%" filterable>
                  <el-option label="MagicStick (魔法棒)" value="MagicStick" />
                  <el-option label="Aim (瞄准)" value="Aim" />
                  <el-option label="Mouse (鼠标)" value="Mouse" />
                  <el-option label="Connection (连接)" value="Connection" />
                  <el-option label="Headset (耳机)" value="Headset" />
                  <el-option label="Trophy (奖杯)" value="Trophy" />
                  <el-option label="Star (星星)" value="Star" />
                  <el-option label="Service (客服)" value="Service" />
                  <el-option label="Coin (金币)" value="Coin" />
                  <el-option label="Position (定位)" value="Position" />
                  <el-option label="Key (钥匙)" value="Key" />
                  <el-option label="Medal (奖牌)" value="Medal" />
                  <el-option label="Promotion (推广)" value="Promotion" />
                  <el-option label="Reading (阅读)" value="Reading" />
                  <el-option label="School (学校)" value="School" />
                  <el-option label="SetUp (设置)" value="SetUp" />
                  <el-option label="ShoppingCart (购物车)" value="ShoppingCart" />
                  <el-option label="Tools (工具)" value="Tools" />
                  <el-option label="TrendCharts (趋势)" value="TrendCharts" />
                  <el-option label="User (用户)" value="User" />
                  <el-option label="Wallet (钱包)" value="Wallet" />
                  <el-option label="ChatLineSquare (聊天)" value="ChatLineSquare" />
                  <el-option label="DataBoard (数据)" value="DataBoard" />
                  <el-option label="Discount (折扣)" value="Discount" />
                  <el-option label="SwitchFilled (开关)" value="SwitchFilled" />
                  <el-option label="Sunny (晴天)" value="Sunny" />
                  <el-option label="Collection (收藏)" value="Collection" />
                  <el-option label="Flag (旗帜)" value="Flag" />
                  <el-option label="Opportunity (机会)" value="Opportunity" />
                  <el-option label="Tickets (票券)" value="Tickets" />
                </el-select>
              </el-form-item>
              <el-form-item label="自定义图标">
                <div class="icon-upload-row">
                  <el-upload
                    class="icon-uploader"
                    action="/api/admin/upload"
                    :show-file-list="false"
                    :on-success="(res) => handleIconUpload(feat, res)"
                    :before-upload="beforeImageUpload"
                    accept="image/*"
                  >
                    <img v-if="settings[`feature_${feat}_icon_url`]" :src="settings[`feature_${feat}_icon_url`]" class="icon-preview" />
                    <el-icon v-else class="icon-uploader-icon"><Plus /></el-icon>
                  </el-upload>
                  <el-input v-model="settings[`feature_${feat}_icon_url`]" placeholder="或输入图片URL" class="icon-url-input" />
                </div>
                <div class="field-hint">可选，设置后覆盖上方系统图标</div>
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
              <el-form-item label="内容值" v-if="settings[`feature_${feat}_content_type`] && settings[`feature_${feat}_content_type`] !== 'none' && settings[`feature_${feat}_content_type`] !== 'text'">
                <!-- 图片上传 -->
                <div v-if="settings[`feature_${feat}_content_type`] === 'image'" class="content-upload-row">
                  <el-upload
                    class="content-uploader"
                    action="/api/admin/upload"
                    :show-file-list="false"
                    :on-success="(res) => handleContentUpload(feat, res)"
                    :before-upload="beforeImageUpload"
                    accept="image/*"
                  >
                    <img v-if="settings[`feature_${feat}_content_value`]" :src="settings[`feature_${feat}_content_value`]" class="content-preview" />
                    <el-icon v-else class="content-uploader-icon"><Plus /></el-icon>
                  </el-upload>
                  <el-input v-model="settings[`feature_${feat}_content_value`]" placeholder="或输入图片URL" class="content-url-input" />
                </div>
                <!-- 视频上传 -->
                <div v-else-if="settings[`feature_${feat}_content_type`] === 'video'" class="content-upload-row">
                  <el-upload
                    class="content-uploader"
                    action="/api/admin/upload"
                    :show-file-list="false"
                    :on-success="(res) => handleContentUpload(feat, res)"
                    :before-upload="beforeVideoUpload"
                    accept="video/*"
                  >
                    <video v-if="settings[`feature_${feat}_content_value`]" :src="settings[`feature_${feat}_content_value`]" class="content-preview" controls />
                    <el-icon v-else class="content-uploader-icon"><Plus /></el-icon>
                  </el-upload>
                  <el-input v-model="settings[`feature_${feat}_content_value`]" placeholder="或输入视频URL" class="content-url-input" />
                </div>
                <!-- 下载文件上传 -->
                <div v-else-if="settings[`feature_${feat}_content_type`] === 'download'" class="content-upload-row">
                  <el-upload
                    class="content-uploader"
                    action="/api/admin/upload"
                    :show-file-list="false"
                    :on-success="(res) => handleContentUpload(feat, res)"
                    :before-upload="beforeFileUpload"
                  >
                    <div v-if="settings[`feature_${feat}_content_value`]" class="file-info">
                      <el-icon><Document /></el-icon>
                      <span>{{ getFileName(settings[`feature_${feat}_content_value`]) }}</span>
                    </div>
                    <el-icon v-else class="content-uploader-icon"><Plus /></el-icon>
                  </el-upload>
                  <el-input v-model="settings[`feature_${feat}_content_value`]" placeholder="或输入下载链接" class="content-url-input" />
                </div>
              </el-form-item>
              <el-form-item label="内容值" v-if="settings[`feature_${feat}_content_type`] === 'text'">
                <el-input
                  v-model="settings[`feature_${feat}_content_value`]"
                  type="textarea"
                  :rows="3"
                  placeholder="输入显示的文本内容"
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
import { Trophy, Star, Service, Coin, Position, InfoFilled, Picture, Grid, Document, Check, Plus,
  MagicStick, Aim, Mouse, Connection, Headset, Key, Medal, Promotion, Reading, School,
  SetUp, ShoppingCart, Tools, TrendCharts, User, Wallet, ChatLineSquare, DataBoard,
  Discount, SwitchFilled, Sunny, Collection, Flag, Opportunity, Tickets
} from '@element-plus/icons-vue'
import { getAdminSiteSettings, updateSiteSettings } from '@/api/config'
import { refreshSiteName } from '@/router/index.js'

const iconMap = { Trophy, Star, Service, Coin, Position,
  MagicStick, Aim, Mouse, Connection, Headset,
  Key, Medal, Promotion, Reading, School,
  SetUp, ShoppingCart, Tools, TrendCharts, User,
  Wallet, ChatLineSquare, DataBoard, Discount, SwitchFilled,
  Sunny, Collection, Flag, Opportunity, Tickets
}
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

// 上传后自动保存到数据库（确保缩略图可用）
const autoSaveAfterUpload = async (field, value) => {
  settings.value[field] = value
  try {
    await updateSiteSettings(settings.value)
  } catch (e) {
    console.error('自动保存失败', e)
  }
}

// 二维码上传相关
const handleQrcodeUpload = (field, response) => {
  if (response.code === 200) {
    autoSaveAfterUpload(field, response.data.url)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 图标上传
const handleIconUpload = (feat, response) => {
  if (response.code === 200) {
    autoSaveAfterUpload(`feature_${feat}_icon_url`, response.data.url)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 内容上传
const handleContentUpload = (feat, response) => {
  if (response.code === 200) {
    autoSaveAfterUpload(`feature_${feat}_content_value`, response.data.url)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 获取文件名
const getFileName = (url) => {
  if (!url) return ''
  return url.split('/').pop() || url
}

// 图片上传验证
const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

// 视频上传验证
const beforeVideoUpload = (file) => {
  const isVideo = file.type.startsWith('video/')
  const isLt100M = file.size / 1024 / 1024 < 100

  if (!isVideo) {
    ElMessage.error('只能上传视频文件')
    return false
  }
  if (!isLt100M) {
    ElMessage.error('视频大小不能超过 100MB')
    return false
  }
  return true
}

// 文件上传验证
const beforeFileUpload = (file) => {
  const isLt50M = file.size / 1024 / 1024 < 50

  if (!isLt50M) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
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

/* 图标上传样式 */
.icon-upload-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.icon-uploader {
  flex-shrink: 0;
}

.icon-uploader :deep(.el-upload) {
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.icon-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}

.icon-preview {
  width: 60px;
  height: 60px;
  display: block;
  object-fit: contain;
}

.icon-uploader-icon {
  font-size: 20px;
  color: #8c939d;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-url-input {
  flex: 1;
}

/* 内容上传样式 */
.content-upload-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  width: 100%;
}

.content-uploader {
  flex-shrink: 0;
}

.content-uploader :deep(.el-upload) {
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.content-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}

.content-preview {
  width: 120px;
  height: 120px;
  display: block;
  object-fit: contain;
}

.content-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content-url-input {
  flex: 1;
}

.file-info {
  width: 120px;
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #606266;
  font-size: 12px;
  background: #f5f7fa;
  border-radius: 4px;
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

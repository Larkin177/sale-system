<template>
  <div class="download-page" v-loading="loading">
    <div class="download-container">
      <div class="download-header">
        <h2>{{ pageSettings.download_title || '下载软件' }}</h2>
        <p class="download-subtitle" v-if="pageSettings.download_subtitle">{{ pageSettings.download_subtitle }}</p>
      </div>

      <el-card class="download-card" shadow="hover">
        <el-result
          v-if="downloadInfo"
          icon="success"
          title="支付成功"
          sub-title="感谢您的购买，请点击下方按钮下载软件"
        >
          <template #extra>
            <el-button type="primary" size="large" @click="download" class="download-btn">
              <el-icon><Download /></el-icon> 下载软件
            </el-button>
            <div class="download-meta">
              <p class="tip">下载链接有效期：{{ downloadInfo.expireAt }}</p>
              <p class="tip">已下载次数：{{ downloadInfo.downloadCount }}</p>
            </div>
          </template>
        </el-result>

        <el-result
          v-else
          icon="error"
          title="链接无效或已过期"
          sub-title="请重新支付获取下载链接"
        >
          <template #extra>
            <el-button type="primary" size="large" @click="$router.push('/pay')" class="download-btn">
              重新购买
            </el-button>
          </template>
        </el-result>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Download } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const downloadInfo = ref(null)
const loading = ref(true)

const pageSettings = ref({
  download_title: '下载软件',
  download_subtitle: ''
})

onMounted(async () => {
  const token = route.query.token

  const promises = [
    request.get('/site-settings').catch(() => ({ data: {} }))
  ]

  if (token) {
    promises.push(
      request.get(`/download/info?token=${token}`).catch(() => ({ data: null }))
    )
  }

  const results = await Promise.all(promises)

  const settingsRes = results[0]
  if (settingsRes?.data) {
    pageSettings.value = { ...pageSettings.value, ...settingsRes.data }
  }

  if (token && results[1]?.data) {
    downloadInfo.value = results[1].data
  }

  loading.value = false
})

const download = () => {
  const token = route.query.token
  window.open(`/api/download/file?token=${token}`, '_blank')
}
</script>

<style scoped>
.download-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e9f2 100%);
  padding: 40px 16px;
}

.download-container {
  width: 100%;
  max-width: 520px;
}

.download-header {
  text-align: center;
  margin-bottom: 32px;
}

.download-header h2 {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.download-subtitle {
  font-size: 15px;
  color: #6b7280;
  margin: 0;
}

.download-card {
  border-radius: 16px;
  overflow: hidden;
}

.download-btn {
  min-width: 160px;
  height: 44px;
  font-size: 15px;
  border-radius: 8px;
}

.download-meta {
  margin-top: 20px;
}

.tip {
  color: #9ca3af;
  font-size: 13px;
  margin: 6px 0;
}

@media (max-width: 520px) {
  .download-page {
    padding: 24px 12px;
  }

  .download-header h2 {
    font-size: 22px;
  }
}
</style>

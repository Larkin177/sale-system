<template>
  <div class="download-page">
    <el-card class="download-card">
      <el-result
        v-if="downloadInfo"
        icon="success"
        title="支付成功"
        sub-title="感谢您的购买，请点击下方按钮下载软件"
      >
        <template #extra>
          <el-button type="primary" size="large" @click="download">
            <el-icon><Download /></el-icon> 下载软件
          </el-button>
          <p class="tip">下载链接有效期：{{ downloadInfo.expireAt }}</p>
          <p class="tip">已下载次数：{{ downloadInfo.downloadCount }}</p>
        </template>
      </el-result>

      <el-result
        v-else
        icon="error"
        title="链接无效或已过期"
        sub-title="请重新支付获取下载链接"
      >
        <template #extra>
          <el-button type="primary" @click="$router.push('/pay')">重新购买</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Download } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const downloadInfo = ref(null)

onMounted(async () => {
  const token = route.query.token
  if (!token) return

  try {
    const res = await request.get(`/download/info?token=${token}`)
    downloadInfo.value = res.data
  } catch (e) {
    console.error('获取下载信息失败')
  }
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
  background: #f5f7fa;
}
.download-card {
  width: 500px;
  padding: 20px;
}
.tip {
  color: #999;
  font-size: 14px;
  margin-top: 10px;
}
</style>

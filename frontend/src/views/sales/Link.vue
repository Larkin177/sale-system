<template>
  <SalesLayout>
    <h2>我的推广链接</h2>

    <el-card class="link-card">
      <el-form label-width="100px">
        <el-form-item label="我的推广码">
          <el-input v-model="salesCode" readonly>
            <template #append>
              <el-button @click="copyCode">复制</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="推广链接">
          <el-input v-model="link" readonly>
            <template #append>
              <el-button @click="copyLink">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <div class="qrcode-wrapper">
        <div class="qrcode-box">
          <canvas ref="qrcodeCanvas"></canvas>
          <p class="qrcode-tip">扫码或复制链接分享给客户</p>
        </div>
      </div>
    </el-card>
  </SalesLayout>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import SalesLayout from '@/components/SalesLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import request from '@/utils/request'

const authStore = useAuthStore()
const qrcodeCanvas = ref(null)
const logoUrl = ref('')

const salesCode = ref('')

const link = computed(() => {
  const base = window.location.origin
  if (!salesCode.value) return ''
  return `${base}/?s=${salesCode.value}`
})

async function drawLogoOnCanvas(canvas) {
  if (!logoUrl.value) return
  const img = new Image()
  img.crossOrigin = 'anonymous'
  await new Promise((resolve, reject) => {
    img.onload = resolve
    img.onerror = reject
    img.src = logoUrl.value
  })
  const ctx = canvas.getContext('2d')
  const size = canvas.width
  const logoSize = size * 0.24
  const x = (size - logoSize) / 2
  const y = (size - logoSize) / 2

  // 白色背景 + 阴影
  ctx.shadowColor = 'rgba(0,0,0,0.12)'
  ctx.shadowBlur = 8
  ctx.shadowOffsetX = 0
  ctx.shadowOffsetY = 2
  ctx.fillStyle = '#ffffff'
  const radius = 8
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.lineTo(x + logoSize - radius, y)
  ctx.quadraticCurveTo(x + logoSize, y, x + logoSize, y + radius)
  ctx.lineTo(x + logoSize, y + logoSize - radius)
  ctx.quadraticCurveTo(x + logoSize, y + logoSize, x + logoSize - radius, y + logoSize)
  ctx.lineTo(x + radius, y + logoSize)
  ctx.quadraticCurveTo(x, y + logoSize, x, y + logoSize - radius)
  ctx.lineTo(x, y + radius)
  ctx.quadraticCurveTo(x, y, x + radius, y)
  ctx.closePath()
  ctx.fill()

  // 画图标（留内边距）
  ctx.shadowColor = 'transparent'
  const pad = logoSize * 0.12
  ctx.drawImage(img, x + pad, y + pad, logoSize - pad * 2, logoSize - pad * 2)
}

async function drawQRCode() {
  if (!qrcodeCanvas.value || !link.value) return
  await QRCode.toCanvas(qrcodeCanvas.value, link.value, {
    width: 240,
    margin: 2,
    color: { dark: '#1a1a2e', light: '#ffffff' }
  })
  if (logoUrl.value) {
    await drawLogoOnCanvas(qrcodeCanvas.value).catch(() => {})
  }
}

onMounted(async () => {
  salesCode.value = authStore.userInfo?.code || ''
  try {
    const res = await request.get('/config')
    if (res.data?.qrcode_logo) {
      logoUrl.value = res.data.qrcode_logo
    }
  } catch(e) {}
  await nextTick()
  await drawQRCode()
})

watch(link, async () => {
  await nextTick()
  drawQRCode()
})

const copyCode = () => {
  navigator.clipboard.writeText(salesCode.value)
  ElMessage.success('推广码已复制')
}

const copyLink = () => {
  navigator.clipboard.writeText(link.value)
  ElMessage.success('推广链接已复制')
}
</script>

<style scoped>
.link-card {
  max-width: 600px;
}
.qrcode-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}
.qrcode-box {
  text-align: center;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  display: inline-block;
}
.qrcode-box canvas {
  display: block;
  margin: 0 auto;
  border-radius: 8px;
}
.qrcode-tip {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}
</style>

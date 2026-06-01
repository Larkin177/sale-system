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

        <el-form-item label="自定义价格">
          <el-input-number
            v-model="customPrice"
            :min="minPrice"
            :max="maxPrice"
            :step="1"
          />
          <span class="price-range">
            (可选范围：¥{{ minPrice }} - ¥{{ maxPrice }})
          </span>
        </el-form-item>

        <el-form-item label="推广链接">
          <el-input v-model="link" readonly>
            <template #append>
              <el-button @click="copyLink">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <div class="qrcode-section">
        <p>推广链接二维码：</p>
        <canvas ref="qrcodeCanvas"></canvas>
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

const salesCode = ref('')
const customPrice = ref(99)
const minPrice = ref(80)
const maxPrice = ref(150)

const link = computed(() => {
  const base = window.location.origin
  return `${base}/pay?s=${salesCode.value}&p=${customPrice.value}`
})

onMounted(async () => {
  salesCode.value = authStore.userInfo?.code || ''

  try {
    const res = await request.get('/config')
    minPrice.value = parseInt(res.data.min_price) || 80
    maxPrice.value = parseInt(res.data.max_price) || 150
    customPrice.value = parseInt(res.data.base_price) || 99
  } catch (e) {
    console.error('获取配置失败')
  }
})

watch(link, async (val) => {
  await nextTick()
  if (qrcodeCanvas.value) {
    QRCode.toCanvas(qrcodeCanvas.value, val, {
      width: 200,
      margin: 2
    })
  }
}, { immediate: true })

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
.price-range {
  margin-left: 10px;
  color: #999;
  font-size: 14px;
}
.qrcode-section {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}
.qrcode-section p {
  margin-bottom: 15px;
  color: #666;
}
</style>

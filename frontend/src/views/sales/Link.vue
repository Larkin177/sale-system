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

        <el-form-item label="选择平台">
          <el-radio-group v-model="selectedPlatform" @change="onPlatformChange">
            <el-radio-button v-for="p in platforms" :key="p" :value="p">
              {{ p === 'mac' ? 'Mac' : p === 'windows' ? 'Windows' : '全平台' }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="选择套餐" v-if="packages.length > 0">
          <el-select v-model="selectedPackageId" placeholder="选择套餐" @change="onPackageChange" style="width: 100%;">
            <el-option
              v-for="pkg in packages"
              :key="pkg.id"
              :label="`${pkg.name} - ¥${pkg.price}`"
              :value="pkg.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="自定义价格" v-if="selectedPackage">
          <el-input-number
            v-model="customPrice"
            :min="selectedPackage.minPrice || selectedPackage.price"
            :max="selectedPackage.maxPrice || selectedPackage.price"
            :step="1"
          />
          <span class="price-range">
            (可选范围：¥{{ selectedPackage.minPrice || selectedPackage.price }} - ¥{{ selectedPackage.maxPrice || selectedPackage.price }})
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
const platforms = ref([])
const packages = ref([])
const selectedPlatform = ref('')
const selectedPackageId = ref(null)
const selectedPackage = ref(null)
const customPrice = ref(99)

const link = computed(() => {
  const base = window.location.origin
  if (!selectedPackageId.value || !salesCode.value) return ''
  return `${base}/pay?s=${salesCode.value}&pkg=${selectedPackageId.value}&p=${customPrice.value}`
})

const loadPlatforms = async () => {
  try {
    const res = await request.get('/products/1/platforms')
    platforms.value = res.data || []
    if (platforms.value.length > 0) {
      selectedPlatform.value = platforms.value[0]
      await loadPackages()
    }
  } catch (e) {
    console.error('获取平台失败')
  }
}

const loadPackages = async () => {
  if (!selectedPlatform.value) return
  try {
    const res = await request.get('/products/1/packages', { params: { platform: selectedPlatform.value } })
    packages.value = res.data || []
    // 自动选中第一个套餐
    if (packages.value.length > 0) {
      selectedPackageId.value = packages.value[0].id
      onPackageChange()
    }
  } catch (e) {
    console.error('获取套餐失败')
  }
}

const onPlatformChange = () => {
  selectedPackageId.value = null
  selectedPackage.value = null
  loadPackages()
}

const onPackageChange = () => {
  const pkg = packages.value.find(p => p.id === selectedPackageId.value)
  selectedPackage.value = pkg || null
  if (pkg) {
    customPrice.value = pkg.price
  }
}

onMounted(async () => {
  salesCode.value = authStore.userInfo?.code || ''
  await loadPlatforms()
})

watch(link, async (val) => {
  await nextTick()
  if (qrcodeCanvas.value && val) {
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
  if (!link.value) {
    ElMessage.warning('请先选择套餐')
    return
  }
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

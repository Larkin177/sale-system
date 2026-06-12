<template>
  <SalesLayout>
    <h2>我的推广</h2>

    <div class="promotion-page">
      <div class="promo-card">
        <!-- ========== QR Code + Brand ========== -->
        <div class="promo-body">
          <div class="qr-section">
            <!-- QR code with integrated brand bar -->
            <div ref="qrFrameRef" class="qr-frame">
              <div class="qr-stage">
                <div ref="qrContainer" class="qr-code-svg"></div>
                <!-- Logo badge: rounded square with logo only -->
                <div v-if="logoUrl" class="qr-logo-badge">
                  <div class="qr-logo-badge-inner">
                    <img :src="logoUrl" alt="logo" class="qr-logo-img" />
                  </div>
                </div>
              </div>
              <!-- Brand area: full-width line + brand name -->
              <div class="qr-brand">
                <div class="qr-brand-line"></div>
                <span class="qr-brand-text">CC-INSTALLER</span>
              </div>
            </div>
            <p class="qr-hint">
              <el-icon><Iphone /></el-icon>
              扫码或分享链接给客户
            </p>
            <el-button class="save-qr-btn" size="small" plain @click="saveQR">
              <el-icon><Download /></el-icon> 保存二维码
            </el-button>
          </div>

          <div class="promo-divider"></div>

          <!-- Sales Info -->
          <div class="info-section">
            <div class="info-row">
              <span class="info-label">推广码</span>
              <div class="info-value">
                <code class="info-code">{{ salesCode }}</code>
                <el-button size="small" type="primary" plain @click="copyCode">复制</el-button>
              </div>
            </div>
            <div class="info-row">
              <span class="info-label">推广链接</span>
              <div class="info-value">
                <span class="info-link">{{ link }}</span>
                <el-button size="small" type="primary" plain @click="copyLink">复制</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </SalesLayout>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import SalesLayout from '@/components/SalesLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { Iphone, Download } from '@element-plus/icons-vue'
import QRCodeStyling from 'qr-code-styling'
import request from '@/utils/request'
import html2canvas from 'html2canvas'

const authStore = useAuthStore()
const qrContainer = ref(null)
const qrFrameRef = ref(null)
const logoUrl = ref('')
const salesCode = ref('')
let qrInstance = null

const link = computed(() => {
  const base = window.location.origin
  if (!salesCode.value) return ''
  return `${base}/?s=${salesCode.value}`
})

// Solid QR dot color — clean, flat design
const QRDotColor = '#1a1a2e'

function createQR() {
  if (!qrContainer.value || !link.value) return

  if (qrInstance) {
    qrInstance.update({
      data: link.value,
    })
    return
  }

  qrInstance = new QRCodeStyling({
    width: 260,
    height: 260,
    type: "svg",
    data: link.value,
    qrOptions: { errorCorrectionLevel: "H" },
    dotsOptions: {
      type: "rounded",
      color: QRDotColor,
    },
    cornersSquareOptions: {
      type: "extra-rounded",
      color: QRDotColor,
    },
    cornersDotOptions: {
      type: "dot",
      color: QRDotColor,
    },
    backgroundOptions: {
      color: "#ffffff",
      round: 6,
    },
  })
  qrInstance.append(qrContainer.value)
}

onMounted(async () => {
  salesCode.value = authStore.userInfo?.code || ''

  try {
    const res = await request.get('/config')
    if (res.data?.qrcode_logo) {
      logoUrl.value = res.data.qrcode_logo
    }
  } catch (e) {
    // use defaults
  }

  await nextTick()
  createQR()
})

watch(link, async () => {
  await nextTick()
  createQR()
})

onUnmounted(() => {
  qrInstance = null
})

const copyCode = () => {
  navigator.clipboard.writeText(salesCode.value)
  ElMessage.success('推广码已复制')
}

const copyLink = () => {
  navigator.clipboard.writeText(link.value)
  ElMessage.success('推广链接已复制')
}

const saveQR = async () => {
  if (!qrFrameRef.value) return
  try {
    const canvas = await html2canvas(qrFrameRef.value, {
      backgroundColor: '#fff',
      scale: 2,
      useCORS: true,
    })
    const link = document.createElement('a')
    link.download = 'cc-installer-promo-qr.png'
    link.href = canvas.toDataURL('image/png')
    link.click()
    ElMessage.success('二维码已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}
</script>

<style scoped>
.promotion-page {
  max-width: 500px;
  margin: 0 auto;
}

/* ===== Main Card ===== */
.promo-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f5;
}

/* ===== Body ===== */
.promo-body {
  padding: 32px;
}

/* ===== QR Section ===== */
.qr-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-frame {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #eef0f6;
}

.qr-stage {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  line-height: 0;
}

.qr-code-svg {
  display: flex;
}

.qr-code-svg :deep(svg) {
  display: block;
  border-radius: 4px;
}

/* ===== Logo badge — matching admin preview ===== */
.qr-logo-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 2;
}

.qr-logo-badge-inner {
  width: 92px;
  height: 92px;
  background: #fff;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-logo-img {
  width: 74px;
  height: 74px;
  object-fit: contain;
}

/* ===== Brand area — full-width line + brand name ===== */
.qr-brand {
  text-align: center;
  padding-top: 0;
  position: relative;
}

.qr-brand-line {
  height: 1px;
  background: #e5e7eb;
  margin: 0 -16px 12px;
}

.qr-brand-text {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: 4px;
}

/* ===== Hint ===== */
.qr-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 18px 0 12px;
  font-size: 14px;
  color: #909399;
}

/* ===== Save QR button ===== */
.save-qr-btn {
  width: 100%;
}

/* ===== Divider ===== */
.promo-divider {
  height: 1px;
  background: #eef0f6;
  margin: 20px 0;
}

/* ===== Info Section ===== */
.info-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.info-label {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  white-space: nowrap;
  padding-top: 8px;
  min-width: 56px;
}

.info-value {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.info-code {
  flex: 1;
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  background: #f3f4f6;
  padding: 8px 14px;
  border-radius: 8px;
  letter-spacing: 1px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.info-link {
  flex: 1;
  font-size: 13px;
  color: #6b7280;
  background: #f9fafb;
  padding: 8px 14px;
  border-radius: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border: 1px solid #f0f0f5;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .promo-body {
    padding: 20px;
  }

  .qr-logo-badge-inner {
    width: 74px;
    height: 74px;
  }

  .qr-logo-img {
    width: 58px;
    height: 58px;
  }

  .qr-brand-line {
    margin: 0 -12px 10px;
  }

  .qr-brand-text {
    font-size: 12px;
    letter-spacing: 3px;
  }

  .qr-brand {
    padding-top: 0;
  }

  .info-code {
    font-size: 14px;
    padding: 6px 12px;
  }

  .info-link {
    font-size: 12px;
    padding: 6px 12px;
  }
}
</style>

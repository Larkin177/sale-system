<template>
  <AdminLayout>
    <h2>系统配置</h2>

    <el-card style="max-width: 700px; margin-bottom: 20px;">
      <template #header>支付模式</template>
      <el-form label-width="140px">
        <el-form-item label="微信支付">
          <el-radio-group v-model="payMode.wechat_pay_mode">
            <el-radio value="static">静态二维码（上传个人收款码，管理员手动确认）</el-radio>
            <el-radio value="api">官方API（微信支付官方接口，自动回调）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付宝">
          <el-radio-group v-model="payMode.alipay_pay_mode">
            <el-radio value="static">静态二维码（上传个人收款码，管理员手动确认）</el-radio>
            <el-radio value="api">官方API（支付宝官方接口，自动回调）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="max-width: 700px; margin-bottom: 20px;">
      <template #header>收款二维码</template>
      <el-form label-width="120px">
        <el-form-item label="微信收款码">
          <div>
            <el-input v-model="qrCodes.wechat_qrcode" placeholder="输入图片URL或上传" style="margin-bottom: 8px" />
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :on-success="(res) => qrCodes.wechat_qrcode = res.data?.url || ''"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small">上传图片</el-button>
            </el-upload>
          </div>
          <img v-if="qrCodes.wechat_qrcode" :src="qrCodes.wechat_qrcode" style="max-width: 200px; max-height: 200px; margin-top: 8px; border-radius: 8px;" />
        </el-form-item>
        <el-form-item label="支付宝收款码">
          <div>
            <el-input v-model="qrCodes.alipay_qrcode" placeholder="输入图片URL或上传" style="margin-bottom: 8px" />
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :on-success="(res) => qrCodes.alipay_qrcode = res.data?.url || ''"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small">上传图片</el-button>
            </el-upload>
          </div>
          <img v-if="qrCodes.alipay_qrcode" :src="qrCodes.alipay_qrcode" style="max-width: 200px; max-height: 200px; margin-top: 8px; border-radius: 8px;" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="max-width: 700px; margin-bottom: 20px;">
      <template #header>佣金设置</template>
      <el-form label-width="120px">
        <el-form-item label="默认分润比例">
          <el-input-number v-model="config.default_commission_rate" :min="0" :max="100" :step="1" />
          <span style="margin-left: 10px;">%</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="max-width: 700px; margin-bottom: 20px;">
      <template #header>推广二维码图标</template>
      <el-form label-width="120px">
        <el-form-item label="二维码中心图标">
          <div>
            <el-input v-model="qrcodeLogo" placeholder="输入图片URL或上传" style="margin-bottom: 8px" />
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :on-success="(res) => qrcodeLogo = res.data?.url || ''"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small">上传图片</el-button>
            </el-upload>
          </div>
          <div class="form-tip">上传后，销售推广链接二维码中间将显示此图标（建议 200x200px 方形PNG）</div>
        </el-form-item>
        <el-form-item label="预览效果">
          <div class="qrcode-preview-box">
            <canvas ref="qrcodePreviewCanvas" style="display:none;"></canvas>
            <img v-if="qrcodePreviewDataUrl" :src="qrcodePreviewDataUrl" style="width:180px;height:180px;border-radius:8px;border:1px solid #e5e7eb;" />
            <div v-else style="width:180px;height:180px;border-radius:8px;border:1px dashed #dcdfe6;display:flex;align-items:center;justify-content:center;color:#c0c4cc;font-size:13px;">预览</div>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-button type="primary" @click="saveAll" :loading="loading" style="margin-top: 10px;">
      保存全部配置
    </el-button>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, watch, onMounted, nextTick } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import QRCode from 'qrcode'

const loading = ref(false)
const uploadUrl = '/api/admin/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const config = ref({ default_commission_rate: 10 })
const payMode = reactive({ wechat_pay_mode: 'static', alipay_pay_mode: 'static' })
const qrCodes = reactive({ wechat_qrcode: '', alipay_qrcode: '' })
const qrcodeLogo = ref('')
const qrcodePreviewCanvas = ref(null)
const qrcodePreviewDataUrl = ref('')

// 监听图标变化，生成预览二维码
watch(qrcodeLogo, async (val) => {
  await nextTick()
  if (!val || !qrcodePreviewCanvas.value) {
    qrcodePreviewDataUrl.value = ''
    return
  }
  try {
    const canvas = qrcodePreviewCanvas.value
    await QRCode.toCanvas(canvas, 'https://example.com/?s=DEMO', {
      width: 180, margin: 2,
      color: { dark: '#1a1a2e', light: '#ffffff' }
    })
    // 叠加图标
    const img = new Image()
    img.crossOrigin = 'anonymous'
    await new Promise((resolve, reject) => {
      img.onload = resolve; img.onerror = reject; img.src = val
    })
    const ctx = canvas.getContext('2d')
    const size = canvas.width
    const logoSize = size * 0.24
    const x = (size - logoSize) / 2
    const y = (size - logoSize) / 2
    ctx.shadowColor = 'rgba(0,0,0,0.12)'
    ctx.shadowBlur = 6
    ctx.shadowOffsetX = 0
    ctx.shadowOffsetY = 2
    ctx.fillStyle = '#ffffff'
    const radius = 6
    ctx.beginPath()
    ctx.moveTo(x + radius, y); ctx.lineTo(x + logoSize - radius, y)
    ctx.quadraticCurveTo(x + logoSize, y, x + logoSize, y + radius)
    ctx.lineTo(x + logoSize, y + logoSize - radius)
    ctx.quadraticCurveTo(x + logoSize, y + logoSize, x + logoSize - radius, y + logoSize)
    ctx.lineTo(x + radius, y + logoSize)
    ctx.quadraticCurveTo(x, y + logoSize, x, y + logoSize - radius)
    ctx.lineTo(x, y + radius)
    ctx.quadraticCurveTo(x, y, x + radius, y)
    ctx.closePath(); ctx.fill()
    ctx.shadowColor = 'transparent'
    const pad = logoSize * 0.12
    ctx.drawImage(img, x + pad, y + pad, logoSize - pad * 2, logoSize - pad * 2)
    qrcodePreviewDataUrl.value = canvas.toDataURL()
  } catch(e) {
    qrcodePreviewDataUrl.value = ''
  }
})

onMounted(async () => {
  try {
    const res = await request.get('/admin/config')
    if (res.data) {
      config.value.default_commission_rate = parseInt(res.data.default_commission_rate) || 10
      payMode.wechat_pay_mode = res.data.wechat_pay_mode || 'static'
      payMode.alipay_pay_mode = res.data.alipay_pay_mode || 'static'
      qrCodes.wechat_qrcode = res.data.wechat_qrcode || ''
      qrCodes.alipay_qrcode = res.data.alipay_qrcode || ''
      qrcodeLogo.value = res.data.qrcode_logo || ''

      }
  } catch (e) { console.error('获取配置失败') }
})

const saveAll = async () => {
  loading.value = true
  try {
    const allConfigs = {
      // 佣金
      default_commission_rate: String(config.value.default_commission_rate),
      // 支付模式
      ...Object.fromEntries(Object.entries(payMode).map(([k, v]) => [k, String(v)])),
      // 收款码
      ...Object.fromEntries(Object.entries(qrCodes).map(([k, v]) => [k, String(v)])),
      // 推广二维码图标
      qrcode_logo: qrcodeLogo.value
    }
    for (const [key, value] of Object.entries(allConfigs)) {
      await request.put(`/admin/config/${key}`, { value })
    }
    ElMessage.success('配置保存成功')
  } catch (e) {
    ElMessage.error('保存配置失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.el-card { max-width: 700px; }
.el-radio { display: block; margin-bottom: 8px; height: auto; line-height: 22px; padding: 4px 0; }
.form-tip { font-size: 12px; color: #9ca3af; margin-top: 4px; display: block; }
.qrcode-preview-box { display: flex; align-items: center; justify-content: center; min-height: 180px; }
</style>

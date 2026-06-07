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

    <el-button type="primary" @click="saveAll" :loading="loading" style="margin-top: 10px;">
      保存全部配置
    </el-button>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const uploadUrl = '/api/admin/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const config = ref({ default_commission_rate: 10 })

const payMode = reactive({ wechat_pay_mode: 'static', alipay_pay_mode: 'static' })
const qrCodes = reactive({ wechat_qrcode: '', alipay_qrcode: '' })

onMounted(async () => {
  try {
    const res = await request.get('/admin/config')
    if (res.data) {
      config.value.default_commission_rate = parseInt(res.data.default_commission_rate) || 10
      payMode.wechat_pay_mode = res.data.wechat_pay_mode || 'static'
      payMode.alipay_pay_mode = res.data.alipay_pay_mode || 'static'
      qrCodes.wechat_qrcode = res.data.wechat_qrcode || ''
      qrCodes.alipay_qrcode = res.data.alipay_qrcode || ''
    }
  } catch (e) { console.error('获取配置失败') }
})

const saveAll = async () => {
  loading.value = true
  try {
    // Commission rate
    await request.put('/admin/config/default_commission_rate', { value: String(config.value.default_commission_rate) })
    // Payment modes
    for (const [key, value] of Object.entries(payMode)) {
      await request.put(`/admin/config/${key}`, { value: String(value) })
    }
    // QR codes
    for (const [key, value] of Object.entries(qrCodes)) {
      await request.put(`/admin/config/${key}`, { value: String(value) })
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
</style>

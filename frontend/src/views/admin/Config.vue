<template>
  <AdminLayout>
    <h2>系统配置</h2>

    <el-card style="max-width: 700px; margin-bottom: 20px;">
      <template #header>价格配置</template>
      <el-form :model="config" label-width="120px">
        <el-form-item label="基础价格">
          <el-input-number v-model="config.base_price" :min="0" :step="1" />
          <span style="margin-left: 10px;">元</span>
        </el-form-item>
        <el-form-item label="最低定价">
          <el-input-number v-model="config.min_price" :min="0" :step="1" />
          <span style="margin-left: 10px;">元</span>
        </el-form-item>
        <el-form-item label="最高定价">
          <el-input-number v-model="config.max_price" :min="0" :step="1" />
          <span style="margin-left: 10px;">元</span>
        </el-form-item>
        <el-form-item label="默认分润比例">
          <el-input-number v-model="config.default_commission_rate" :min="0" :max="100" :step="1" />
          <span style="margin-left: 10px;">%</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="max-width: 700px; margin-bottom: 20px;">
      <template #header>支付模式</template>
      <el-form label-width="140px">
        <el-form-item label="微信支付">
          <el-radio-group v-model="payMode.wechat_pay_mode">
            <el-radio value="static">
              🟢 静态二维码
              <span style="color: #999; font-size: 12px; margin-left: 4px;">（上传个人收款码，管理员手动确认）</span>
            </el-radio>
            <el-radio value="api">
              🔵 官方API
              <span style="color: #999; font-size: 12px; margin-left: 4px;">（微信支付官方接口，自动回调）</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付宝">
          <el-radio-group v-model="payMode.alipay_pay_mode">
            <el-radio value="static">
              🟢 静态二维码
              <span style="color: #999; font-size: 12px; margin-left: 4px;">（上传个人收款码，管理员手动确认）</span>
            </el-radio>
            <el-radio value="api">
              🔵 官方API
              <span style="color: #999; font-size: 12px; margin-left: 4px;">（支付宝官方接口，自动回调）</span>
            </el-radio>
          </el-radio-group>
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

const config = ref({
  base_price: 99,
  min_price: 80,
  max_price: 150,
  default_commission_rate: 10
})

const payMode = reactive({
  wechat_pay_mode: 'static',
  alipay_pay_mode: 'static'
})

onMounted(async () => {
  try {
    const res = await request.get('/admin/config')
    if (res.data) {
      config.value = {
        base_price: parseInt(res.data.base_price) || 99,
        min_price: parseInt(res.data.min_price) || 80,
        max_price: parseInt(res.data.max_price) || 150,
        default_commission_rate: parseInt(res.data.default_commission_rate) || 10
      }
      payMode.wechat_pay_mode = res.data.wechat_pay_mode || 'static'
      payMode.alipay_pay_mode = res.data.alipay_pay_mode || 'static'
    }
  } catch (e) {
    console.error('获取配置失败')
  }
})

const saveAll = async () => {
  loading.value = true
  try {
    // Save price configs
    for (const [key, value] of Object.entries(config.value)) {
      await request.put(`/admin/config/${key}`, { value: String(value) })
    }
    // Save payment mode configs
    for (const [key, value] of Object.entries(payMode)) {
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
.el-card {
  max-width: 700px;
}

.el-radio {
  display: block;
  margin-bottom: 8px;
  height: auto;
  line-height: 22px;
  padding: 4px 0;
}
</style>

<template>
  <SalesLayout>
    <h2>收款码管理</h2>
    <p style="color:#6b7280;margin-bottom:20px;">上传您的微信/支付宝收款码，用于每月结算</p>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>微信收款码</template>
          <div style="text-align:center;">
            <img v-if="wechatUrl" :src="wechatUrl" style="max-width:250px;max-height:250px;border-radius:8px;border:1px solid #e5e7eb;" />
            <div v-else style="padding:40px 0;color:#999;">未上传</div>
            <div style="margin-top:12px;">
              <el-upload :action="uploadUrl" :headers="uploadHeaders" :on-success="(r) => saveCode('wechat', r.data?.url)" :show-file-list="false" accept="image/*">
                <el-button type="primary" size="small">{{ wechatUrl ? '更换' : '上传' }}微信收款码</el-button>
              </el-upload>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>支付宝收款码</template>
          <div style="text-align:center;">
            <img v-if="alipayUrl" :src="alipayUrl" style="max-width:250px;max-height:250px;border-radius:8px;border:1px solid #e5e7eb;" />
            <div v-else style="padding:40px 0;color:#999;">未上传</div>
            <div style="margin-top:12px;">
              <el-upload :action="uploadUrl" :headers="uploadHeaders" :on-success="(r) => saveCode('alipay', r.data?.url)" :show-file-list="false" accept="image/*">
                <el-button type="primary" size="small">{{ alipayUrl ? '更换' : '上传' }}支付宝收款码</el-button>
              </el-upload>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </SalesLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import SalesLayout from '@/components/SalesLayout.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const authStore = useAuthStore()
const wechatUrl = ref('')
const alipayUrl = ref('')
const uploadUrl = '/api/admin/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

async function loadCodes() {
  try {
    const res = await request.get('/sales/payment-code?salesId=' + authStore.userInfo?.id)
    const codes = res.data || []
    const w = codes.find(c => c.codeType === 'wechat')
    const a = codes.find(c => c.codeType === 'alipay')
    if (w) wechatUrl.value = w.codeUrl
    if (a) alipayUrl.value = a.codeUrl
  } catch(e) {}
}

async function saveCode(type, url) {
  if (!url) return
  try {
    await request.post('/sales/payment-code', {
      salesId: authStore.userInfo?.id,
      codeType: type,
      codeUrl: url,
    })
    ElMessage.success('收款码已保存')
    loadCodes()
  } catch(e) { ElMessage.error('保存失败') }
}

onMounted(loadCodes)
</script>

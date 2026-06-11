<template>
  <div class="forgot-page">
    <el-card class="card">
      <h2>找回密码</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="请输入注册邮箱" prefix-icon="Message" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn" @click="handleReset" :loading="loading">
            {{ sent ? '邮件已发送' : '发送重置邮件' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div style="text-align:center;margin-top:16px;">
        <router-link to="/s/login" style="color:#667eea;text-decoration:none;">返回登录</router-link>
      </div>
    </el-card>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
const form = ref({ email: '' })
const loading = ref(false)
const sent = ref(false)
const rules = { email: [{ required: true, message: '请输入注册邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }] }
async function handleReset() {
  loading.value = true
  try {
    await request.post('/sales/forgot-password', { email: form.value.email })
    sent.value = true
    ElMessage.success('新密码已发送到您的邮箱')
  } catch(e) {
    ElMessage.error(e.response?.data?.message || '发送失败')
  } finally { loading.value = false }
}
</script>
<style scoped>
.forgot-page { min-height:100vh; display:flex; align-items:center; justify-content:center; background:linear-gradient(135deg,#667eea,#764ba2); }
.card { width:400px; padding:20px; }
h2 { text-align:center; margin-bottom:24px; color:#333; }
.btn { width:100%; }
</style>
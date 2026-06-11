<template>
  <AdminLayout>
    <h2>邮件管理</h2>

    <!-- 邮件服务器配置 -->
    <el-card style="max-width: 700px; margin-bottom: 20px;">
      <template #header>📧 邮件服务器配置</template>
      <el-form label-width="140px">
        <el-form-item label="SMTP 服务器">
          <el-input v-model="form.mail_host" placeholder="smtp.qq.com" />
        </el-form-item>
        <el-form-item label="SMTP 端口">
          <el-input v-model="form.mail_port" placeholder="587" />
        </el-form-item>
        <el-form-item label="发件邮箱">
          <el-input v-model="form.mail_username" placeholder="your@qq.com" />
        </el-form-item>
        <el-form-item label="邮箱授权码">
          <el-input v-model="form.mail_password" type="password" placeholder="QQ邮箱授权码(非QQ密码)" show-password />
        </el-form-item>
        <el-form-item label="发件人名称">
          <el-input v-model="form.mail_from_name" placeholder="CC-Installer" />
        </el-form-item>
        <el-form-item label="站点地址">
          <el-input v-model="form.site_url" placeholder="https://www.wonderhow.store" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveMailConfig" :loading="saving">保存邮件配置</el-button>
        </el-form-item>
        <el-form-item label="测试收件人">
          <div style="display:flex;gap:8px;width:100%;">
            <el-input v-model="testEmail" placeholder="输入测试收件邮箱，然后点发送测试" style="flex:1;" />
            <el-button @click="testMail" :loading="testing">发送测试</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 发货邮件模板 -->
    <el-card style="max-width: 700px; margin-bottom: 20px;">
      <template #header>📦 发货邮件模板</template>
      <p style="font-size:12px;color:#909399;margin-bottom:12px;">
        可用变量: <code>{site_name}</code> <code>{order_no}</code> <code>{auth_code}</code> <code>{product_name}</code> <code>{download_url}</code> <code>{hours}</code>
      </p>
      <el-input v-model="form.delivery_template" type="textarea" :rows="8" />
      <div style="margin-top:8px;">
        <el-button type="primary" @click="saveTemplate('delivery_template', form.delivery_template)" :loading="saving">保存发货模板</el-button>
      </div>
    </el-card>

    <!-- 拒绝通知邮件模板 -->
    <el-card style="max-width: 700px; margin-bottom: 20px;">
      <template #header>❌ 拒绝通知邮件模板</template>
      <p style="font-size:12px;color:#909399;margin-bottom:12px;">
        可用变量: <code>{site_name}</code> <code>{order_no}</code> <code>{reason}</code> <code>{email}</code>
      </p>
      <el-input v-model="form.reject_template" type="textarea" :rows="6" placeholder="&lt;div&gt;&lt;h2&gt;订单已被拒绝&lt;/h2&gt;&lt;p&gt;您的订单 {order_no} 已被管理员拒绝。&lt;/p&gt;&lt;p&gt;原因: {reason}&lt;/p&gt;&lt;/div&gt;" />
      <div style="margin-top:8px;">
        <el-button type="primary" @click="saveTemplate('reject_template', form.reject_template)" :loading="saving">保存拒绝模板</el-button>
      </div>
    </el-card>

  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/components/AdminLayout.vue'
import request from '@/utils/request'

const saving = ref(false)
const testing = ref(false)
const testEmail = ref('')
const form = ref({
  mail_host: '',
  mail_port: '',
  mail_username: '',
  mail_password: '',
  mail_from_name: '',
  site_url: '',
  delivery_template: '',
  reject_template: '',
})

async function loadConfig() {
  try {
    const res = await request.get('/admin/config')
    const data = res.data || {}
    form.value = {
      mail_host: data.mail_host || '',
      mail_port: data.mail_port || '587',
      mail_username: data.mail_username || '',
      mail_password: data.mail_password || '',
      mail_from_name: data.mail_from_name || '',
      site_url: data.site_url || 'http://localhost:3000',
      delivery_template: data.delivery_template || '',
      reject_template: data.reject_template || '',
    }
  } catch (e) {
    ElMessage.error('加载配置失败')
  }
}

async function saveMailConfig() {
  saving.value = true
  try {
    const keys = ['mail_host', 'mail_port', 'mail_username', 'mail_password', 'mail_from_name', 'site_url']
    for (const key of keys) {
      await request.put('/admin/config/' + key, { value: String(form.value[key] || '') })
    }
    ElMessage.success('邮件配置已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function saveTemplate(key, value) {
  saving.value = true
  try {
    await request.put('/admin/config/' + key, { value })
    ElMessage.success('模板已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function testMail() {
  testing.value = true
  try {
    await request.post('/admin/email/send', {
      to: testEmail.value,
      subject: '【测试邮件】CC-Installer 邮件配置测试',
      content: '<h2>邮件配置测试成功</h2><p>如果您收到此邮件，说明邮件服务器配置正确。</p>',
    })
    ElMessage.success('测试邮件已发送，请检查收件箱')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '发送失败')
  } finally {
    testing.value = false
  }
}

onMounted(loadConfig)
</script>

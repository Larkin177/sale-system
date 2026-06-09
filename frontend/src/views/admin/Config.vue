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
      <template #header>📧 邮件服务器</template>
      <el-form label-width="140px" :model="mailConfig">
        <el-form-item label="SMTP 服务器">
          <el-input v-model="mailConfig.mail_host" placeholder="smtp.qq.com" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model="mailConfig.mail_port" placeholder="587" />
        </el-form-item>
        <el-form-item label="发件邮箱">
          <el-input v-model="mailConfig.mail_username" placeholder="your@qq.com" />
        </el-form-item>
        <el-form-item label="邮箱密码/授权码">
          <el-input v-model="mailConfig.mail_password" type="password" placeholder="SMTP授权码" show-password />
        </el-form-item>
        <el-form-item label="发件人名称">
          <el-input v-model="mailConfig.mail_from_name" placeholder="CC-Installer" />
        </el-form-item>
        <el-form-item label="站点地址">
          <el-input v-model="mailConfig.site_url" placeholder="http://localhost:3000" />
          <span class="form-tip">用于邮件中的链接生成</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="max-width: 700px; margin-bottom: 20px;">
      <template #header>📨 发货邮件模板</template>
      <el-form label-width="120px">
        <el-form-item label="邮件内容">
          <el-input
            v-model="config.delivery_template"
            type="textarea"
            :rows="10"
            placeholder="输入邮件模板内容，支持HTML"
          />
          <div class="template-vars">
            <span class="vars-label">可用变量：</span>
            <el-tag size="small" v-for="v in templateVars" :key="v.key" class="var-tag">
              <code>{{ v.key }}</code> — {{ v.desc }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="预览">
          <div class="preview-box" v-html="previewContent"></div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-button type="primary" @click="saveAll" :loading="loading" style="margin-top: 10px;">
      保存全部配置
    </el-button>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const uploadUrl = '/api/admin/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const config = ref({ default_commission_rate: 10, delivery_template: '' })
const payMode = reactive({ wechat_pay_mode: 'static', alipay_pay_mode: 'static' })
const qrCodes = reactive({ wechat_qrcode: '', alipay_qrcode: '' })
const mailConfig = reactive({
  mail_host: '',
  mail_port: '587',
  mail_username: '',
  mail_password: '',
  mail_from_name: '',
  site_url: 'http://localhost:3000'
})

const templateVars = [
  { key: '{site_name}', desc: '站点名称' },
  { key: '{site_url}', desc: '站点地址' },
  { key: '{order_no}', desc: '订单号' },
  { key: '{amount}', desc: '订单金额' },
  { key: '{email}', desc: '客户邮箱' },
  { key: '{download_url}', desc: '下载链接' },
  { key: '{order_url}', desc: '订单查询链接' },
  { key: '{auth_code}', desc: '授权码' },
  { key: '{package_name}', desc: '套餐名称' },
  { key: '{product_name}', desc: '产品名称' },
  { key: '{hours}', desc: '授权有效期(小时)' }
]

const previewContent = computed(() => {
  let tpl = config.value.delivery_template
  if (!tpl) return '<p style="color:#999;">暂无模板内容</p>'
  tpl = tpl
    .replace(/{site_name}/g, mailConfig.mail_from_name || 'CC-Installer')
    .replace(/{site_url}/g, mailConfig.site_url || 'http://localhost:3000')
    .replace(/{order_no}/g, 'ORD' + Date.now())
    .replace(/{amount}/g, '99.00')
    .replace(/{email}/g, mailConfig.mail_username || 'customer@email.com')
    .replace(/{download_url}/g, (mailConfig.site_url || 'http://localhost:3000') + '/orders?orderNo=ORD-demo')
    .replace(/{order_url}/g, (mailConfig.site_url || 'http://localhost:3000') + '/orders?orderNo=ORD-demo')
    .replace(/{auth_code}/g, 'AIC-DEMO-XXXX.XXXX')
    .replace(/{package_name}/g, 'Claude Code')
    .replace(/{product_name}/g, 'CC-Installer')
    .replace(/{hours}/g, '72')
  return tpl
})

onMounted(async () => {
  try {
    const res = await request.get('/admin/config')
    if (res.data) {
      config.value.default_commission_rate = parseInt(res.data.default_commission_rate) || 10
      config.value.delivery_template = res.data.delivery_template || ''
      payMode.wechat_pay_mode = res.data.wechat_pay_mode || 'static'
      payMode.alipay_pay_mode = res.data.alipay_pay_mode || 'static'
      qrCodes.wechat_qrcode = res.data.wechat_qrcode || ''
      qrCodes.alipay_qrcode = res.data.alipay_qrcode || ''
      mailConfig.mail_host = res.data.mail_host || ''
      mailConfig.mail_port = res.data.mail_port || '587'
      mailConfig.mail_username = res.data.mail_username || ''
      mailConfig.mail_password = res.data.mail_password || ''
      mailConfig.mail_from_name = res.data.mail_from_name || ''
      mailConfig.site_url = res.data.site_url || 'http://localhost:3000'
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
      // 邮件服务器
      ...Object.fromEntries(Object.entries(mailConfig).map(([k, v]) => [k, String(v)])),
      // 发货模板
      delivery_template: config.value.delivery_template
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
.template-vars { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.vars-label { font-size: 12px; color: #6b7280; margin-right: 4px; }
.var-tag { margin: 2px; }
.var-tag code { font-size: 11px; }
.preview-box {
  width: 100%;
  min-height: 100px;
  padding: 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.8;
}
</style>

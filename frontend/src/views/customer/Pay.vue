<template>
  <div class="pay-page">
    <el-card class="pay-card">
      <h2>购买软件工具</h2>

      <div class="price-display">
        <span class="label">价格</span>
        <span class="price">¥{{ price }}</span>
      </div>

      <div class="sales-info" v-if="salesCode">
        <el-tag type="success">销售专属链接</el-tag>
      </div>

      <el-form :model="form" label-width="80px">
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="用于接收下载链接" />
        </el-form-item>
      </el-form>

      <div class="payment-methods">
        <h4>选择支付方式</h4>
        <el-radio-group v-model="paymentMethod">
          <el-radio-button value="wechat">
            <el-icon><ChatDotRound /></el-icon> 微信支付
          </el-radio-button>
          <el-radio-button value="alipay">
            <el-icon><Wallet /></el-icon> 支付宝
          </el-radio-button>
        </el-radio-group>
      </div>

      <el-button type="primary" size="large" class="pay-btn" @click="handlePay" :loading="loading">
        立即支付
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, Wallet } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const salesCode = ref(route.query.s || '')
const customPrice = ref(route.query.p ? parseInt(route.query.p) : null)
const price = ref(99)
const minPrice = ref(80)
const maxPrice = ref(150)
const paymentMethod = ref('wechat')
const loading = ref(false)

const form = ref({
  phone: ''
})

onMounted(async () => {
  // 获取配置
  try {
    const res = await request.get('/config')
    minPrice.value = parseInt(res.data.min_price) || 80
    maxPrice.value = parseInt(res.data.max_price) || 150

    // 如果有自定义价格参数，使用自定义价格
    if (customPrice.value !== null) {
      // 验证价格是否在允许范围内
      if (customPrice.value >= minPrice.value && customPrice.value <= maxPrice.value) {
        price.value = customPrice.value
      } else {
        // 价格超出范围，使用基础价格
        price.value = parseInt(res.data.base_price) || 99
        ElMessage.warning('价格超出范围，已使用默认价格')
      }
    } else {
      // 没有自定义价格，使用基础价格
      price.value = parseInt(res.data.base_price) || 99
    }
  } catch (e) {
    console.error('获取配置失败')
  }
})

const handlePay = async () => {
  if (!form.value.phone) {
    ElMessage.warning('请输入手机号')
    return
  }

  loading.value = true
  try {
    const res = await request.post('/pay/create', {
      amount: price.value,
      salesCode: salesCode.value,
      phone: form.value.phone,
      paymentMethod: paymentMethod.value
    })

    // 跳转到支付页面
    if (res.data.paymentUrl) {
      window.location.href = res.data.paymentUrl
    } else {
      ElMessage.success('订单创建成功，请完成支付')
    }
  } catch (e) {
    ElMessage.error('创建订单失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.pay-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.pay-card {
  width: 450px;
  padding: 20px;
}
h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
.price-display {
  text-align: center;
  margin-bottom: 30px;
}
.price-display .label {
  color: #666;
  margin-right: 10px;
}
.price-display .price {
  font-size: 36px;
  font-weight: bold;
  color: #f56c6c;
}
.sales-info {
  text-align: center;
  margin-bottom: 20px;
}
.payment-methods {
  margin-bottom: 30px;
}
.payment-methods h4 {
  margin-bottom: 15px;
  color: #666;
}
.pay-btn {
  width: 100%;
}
</style>

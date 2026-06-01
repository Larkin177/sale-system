<template>
  <SalesLayout>
    <h2>认领订单</h2>

    <el-card class="claim-card">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="客户手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入客户手机号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleClaim" :loading="loading">
            认领订单
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="unclaimed-card" style="margin-top: 20px;">
      <template #header>
        <span>待认领订单</span>
      </template>
      <el-table :data="unclaimedOrders" stripe>
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column prop="amount" label="金额">
          <template #default="{ row }">
            ¥{{ row.amount?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="fillOrder(row.orderNo)">
              认领
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </SalesLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import SalesLayout from '@/components/SalesLayout.vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const formRef = ref(null)
const loading = ref(false)
const unclaimedOrders = ref([])

const form = ref({
  orderNo: '',
  phone: ''
})

const rules = {
  orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const loadUnclaimed = async () => {
  try {
    const res = await request.get('/orders/unclaimed')
    unclaimedOrders.value = res.data || []
  } catch (e) {
    console.error('获取待认领订单失败')
  }
}

const fillOrder = (orderNo) => {
  form.value.orderNo = orderNo
}

const handleClaim = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await request.post('/orders/claim', form.value)
    ElMessage.success('认领成功')
    form.value = { orderNo: '', phone: '' }
    loadUnclaimed()
  } catch (e) {
    console.error('认领失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadUnclaimed)
</script>

<style scoped>
.claim-card {
  max-width: 500px;
}
</style>

<template>
  <SalesLayout>
    <h2>我的订单</h2>

    <el-table :data="orders" stripe>
      <el-table-column prop="orderNo" label="订单号" />
      <el-table-column prop="amount" label="金额">
        <template #default="{ row }">
          ¥{{ row.amount?.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="paymentMethod" label="支付方式">
        <template #default="{ row }">
          {{ row.paymentMethod === 'wechat' ? '微信' : '支付宝' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" />
      <el-table-column prop="paidAt" label="支付时间" />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="10"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="loadOrders"
    />
  </SalesLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import SalesLayout from '@/components/SalesLayout.vue'
import request from '@/utils/request'

const orders = ref([])
const page = ref(1)
const total = ref(0)

const loadOrders = async () => {
  try {
    const res = await request.get(`/orders/my?page=${page.value}&size=10`)
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取订单失败')
  }
}

const getStatusType = (status) => {
  const map = { pending: 'info', paid: 'success', settled: '' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待支付', paid: '已支付', settled: '已结算' }
  return map[status] || status
}

onMounted(loadOrders)
</script>

<style scoped>
.el-pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>

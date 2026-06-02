<template>
  <AdminLayout>
    <h2>订单管理</h2>

    <el-table :data="orders" stripe>
      <el-table-column prop="orderNo" label="订单号" />
      <el-table-column prop="amount" label="金额">
        <template #default="{ row }">
          ¥{{ row.amount?.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="salesId" label="绑定销售" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="paymentMethod" label="支付方式">
        <template #default="{ row }">
          {{ row.paymentMethod === 'wechat' ? '微信' : row.paymentMethod === 'alipay' ? '支付宝' : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" />
      <el-table-column prop="paidAt" label="支付时间" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'pending'"
            type="success"
            size="small"
            @click="handleSimulatePay(row)"
          >
            模拟支付
          </el-button>
          <el-tag v-else-if="row.status === 'delivered'" type="success" size="small">
            已自动发货
          </el-tag>
          <el-tag v-else-if="row.status === 'redeemed'" type="info" size="small">
            已核销
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="10"
      :total="total"
      layout="prev, pager, next"
      @current-change="loadOrders"
    />

  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/components/AdminLayout.vue'
import request from '@/utils/request'
import { simulatePayment } from '@/api/order'

const orders = ref([])
const page = ref(1)
const total = ref(0)

const loadOrders = async () => {
  try {
    const res = await request.get(`/admin/orders?page=${page.value}&size=10`)
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取订单失败')
  }
}

const getStatusType = (status) => {
  const map = { pending: 'info', paid: 'success', delivered: 'success', redeemed: '', bound: 'warning', settled: '' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待支付', paid: '已支付', delivered: '已发货', redeemed: '已核销', bound: '已绑定', settled: '已结算' }
  return map[status] || status
}

// 模拟支付
const handleSimulatePay = async (row) => {
  try {
    await ElMessageBox.confirm(`确认模拟支付订单 ${row.orderNo}？`, '模拟支付', { type: 'warning' })
    await simulatePayment(row.id)
    ElMessage.success('支付模拟成功')
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.el-pagination {
  margin-top: 20px;
  justify-content: center;
}

.delivery-info p {
  margin: 8px 0;
}

.message-preview {
  margin-top: 12px;
}

.preview-text {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 13px;
  line-height: 1.6;
  margin-top: 6px;
}

@media (max-width: 768px) {
  .el-dialog {
    width: 90% !important;
  }
}
</style>

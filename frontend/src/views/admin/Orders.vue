<template>
  <AdminLayout>
    <h2>订单管理</h2>

    <!-- Tabs for order status filtering -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="order-tabs">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane name="pending">
        <template #label>
          待支付 <el-badge :value="counts.pending" :hidden="!counts.pending" />
        </template>
      </el-tab-pane>
      <el-tab-pane name="pending_verify">
        <template #label>
          <span style="color: #e6a23c;">待审核</span>
          <el-badge :value="counts.pending_verify" :hidden="!counts.pending_verify" style="margin-left: 4px;" />
        </template>
      </el-tab-pane>
      <el-tab-pane label="已支付" name="paid" />
      <el-tab-pane label="已发货" name="delivered" />
    </el-tabs>

    <div class="batch-actions" v-if="selectedOrders.length > 0">
      <el-button type="primary" @click="batchDeliver">批量发货</el-button>
      <el-button type="success" @click="batchSettle">批量结算</el-button>
      <span class="selected-count">已选择 {{ selectedOrders.length }} 个订单</span>
    </div>

    <el-table :data="orders" stripe @selection-change="handleSelectionChange" ref="tableRef">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="orderNo" label="订单号" min-width="160" />
      <el-table-column prop="amount" label="金额">
        <template #default="{ row }">
          ¥{{ row.amount?.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="salesId" label="绑定销售" />
      <el-table-column prop="customerPhone" label="客户手机" />
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
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <!-- 待审核：确认收款 / 拒绝 -->
          <template v-if="row.status === 'pending_verify'">
            <el-button type="success" size="small" @click="handleConfirmPayment(row)">
              确认收款
            </el-button>
            <el-button type="danger" size="small" @click="handleRejectPayment(row)">
              拒绝
            </el-button>
          </template>
          <!-- 待支付：模拟支付（测试用） -->
          <el-button
            v-else-if="row.status === 'pending'"
            type="warning"
            size="small"
            @click="handleSimulatePay(row)"
          >
            模拟支付
          </el-button>
          <el-tag v-else-if="row.status === 'delivered'" type="success" size="small">
            已自动发货
          </el-tag>
          <span v-else>-</span>
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

    <!-- Confirm Payment Dialog -->
    <el-dialog v-model="confirmDialog.visible" title="确认收款" width="400px">
      <p>确认已收到客户 <strong>{{ confirmDialog.phone }}</strong> 的付款？</p>
      <p style="color: #999;">订单号: {{ confirmDialog.orderNo }}</p>
      <p style="color: #999;">金额: ¥{{ confirmDialog.amount }}</p>
      <el-input
        v-model="confirmDialog.note"
        placeholder="审核备注（可选）"
        style="margin-top: 12px;"
      />
      <template #footer>
        <el-button @click="confirmDialog.visible = false">取消</el-button>
        <el-button type="success" @click="doConfirmPayment" :loading="confirmDialog.loading">
          确认收款并发货
        </el-button>
      </template>
    </el-dialog>

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectDialog.visible" title="拒绝收款" width="400px">
      <p>确认拒绝订单 <strong>{{ rejectDialog.orderNo }}</strong>？</p>
      <el-input
        v-model="rejectDialog.note"
        placeholder="拒绝原因（必填）"
        style="margin-top: 12px;"
      />
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" @click="doRejectPayment" :loading="rejectDialog.loading">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>

  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/components/AdminLayout.vue'
import request from '@/utils/request'
import { simulatePayment } from '@/api/order'

const orders = ref([])
const page = ref(1)
const total = ref(0)
const selectedOrders = ref([])
const activeTab = ref('all')
const counts = reactive({ pending: 0, pending_verify: 0 })

const confirmDialog = reactive({
  visible: false,
  orderId: null,
  orderNo: '',
  phone: '',
  amount: '',
  note: '',
  loading: false
})

const rejectDialog = reactive({
  visible: false,
  orderId: null,
  orderNo: '',
  note: '',
  loading: false
})

const handleSelectionChange = (selection) => {
  selectedOrders.value = selection
}

const handleTabChange = () => {
  page.value = 1
  loadOrders()
}

const batchDeliver = async () => {
  try {
    await ElMessageBox.confirm(`确认批量发货 ${selectedOrders.value.length} 个订单？`, '批量发货', { type: 'warning' })
    for (const order of selectedOrders.value) {
      if (order.status === 'paid') {
        await simulatePayment(order.id)
      }
    }
    ElMessage.success('批量发货成功')
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const batchSettle = async () => {
  try {
    await ElMessageBox.confirm(`确认批量结算 ${selectedOrders.value.length} 个订单？`, '批量结算', { type: 'warning' })
    for (const order of selectedOrders.value) {
      if (order.status === 'paid' || order.status === 'delivered') {
        await simulatePayment(order.id)
      }
    }
    ElMessage.success('批量结算成功')
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const loadOrders = async () => {
  try {
    const params = `page=${page.value}&size=10`
    const statusParam = activeTab.value !== 'all' ? `&status=${activeTab.value}` : ''
    const res = await request.get(`/admin/orders?${params}${statusParam}`)
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取订单失败')
  }
}

// Also load counts for pending_verify
const loadCounts = async () => {
  try {
    const res = await request.get('/admin/orders?page=1&size=1&status=pending')
    if (res.data?.total !== undefined) counts.pending = res.data.total
    const res2 = await request.get('/admin/orders?page=1&size=1&status=pending_verify')
    if (res2.data?.total !== undefined) counts.pending_verify = res2.data.total
  } catch (e) {
    // ignore
  }
}

// Confirm payment (static mode)
const handleConfirmPayment = (row) => {
  confirmDialog.orderId = row.id
  confirmDialog.orderNo = row.orderNo
  confirmDialog.phone = row.customerPhone || '未知'
  confirmDialog.amount = row.amount?.toFixed(2) || '0'
  confirmDialog.note = ''
  confirmDialog.visible = true
}

const doConfirmPayment = async () => {
  confirmDialog.loading = true
  try {
    const adminId = localStorage.getItem('adminId')
    await request.post(`/admin/orders/${confirmDialog.orderId}/confirm-payment`, {
      adminId: adminId,
      note: confirmDialog.note
    })
    ElMessage.success('确认收款成功，已自动发货')
    confirmDialog.visible = false
    loadOrders()
    loadCounts()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    confirmDialog.loading = false
  }
}

// Reject payment (static mode)
const handleRejectPayment = (row) => {
  rejectDialog.orderId = row.id
  rejectDialog.orderNo = row.orderNo
  rejectDialog.note = ''
  rejectDialog.visible = true
}

const doRejectPayment = async () => {
  if (!rejectDialog.note.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  rejectDialog.loading = true
  try {
    const adminId = localStorage.getItem('adminId')
    await request.post(`/admin/orders/${rejectDialog.orderId}/reject-payment`, {
      adminId: adminId,
      note: rejectDialog.note
    })
    ElMessage.success('已拒绝该订单')
    rejectDialog.visible = false
    loadOrders()
    loadCounts()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    rejectDialog.loading = false
  }
}

const getStatusType = (status) => {
  const map = { pending: 'info', pending_verify: 'warning', paid: 'success', delivered: 'success', redeemed: '', bound: 'warning', settled: '' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待支付', pending_verify: '待审核', paid: '已支付', delivered: '已发货', redeemed: '已核销', bound: '已绑定', settled: '已结算' }
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

onMounted(() => {
  loadOrders()
  loadCounts()
})
</script>

<style scoped>
.order-tabs {
  margin-bottom: 16px;
}

.batch-actions {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.selected-count {
  color: #666;
  font-size: 14px;
}

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

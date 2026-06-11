<template>
  <AdminLayout>
    <div style="padding: 0 20px;">
      <h2 style="margin-bottom: 20px;">分润结算</h2>

      <!-- Sales Summary -->
      <el-table :data="paginatedSummary" stripe v-loading="loading" style="width: 100%;">
        <el-table-column prop="salesName" label="销售姓名" width="120" />
        <el-table-column prop="salesCode" label="推广码" width="110" />
        <el-table-column label="总销售额" width="130">
          <template #default="{ row }">¥{{ (row.totalSales || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="总佣金" width="130">
          <template #default="{ row }">¥{{ (row.totalCommission || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="已结算" width="130">
          <template #default="{ row }">
            <span style="color:#22c55e;">¥{{ (row.settledCommission || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="当前分润比例" width="120">
          <template #default="{ row }">{{ row.commissionRate }}%</template>
        </el-table-column>
        <el-table-column label="待结算" width="130">
          <template #default="{ row }">
            <span style="color:#f59e0b;">¥{{ (row.pendingCommission || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">明细</el-button>
            <el-button size="small" type="success" @click="openSettleDialog(row)" :disabled="!(row.pendingCommission > 0)">结算</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="summaryPage"
        :page-size="summaryPageSize"
        :total="summaryTotal"
        layout="total, prev, pager, next"
        style="margin-top:16px;"
      />

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="1000px" top="5vh">
      <div v-if="detailData" style="margin-bottom:16px;">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="总销售额">¥{{ detailData.totalSales?.toFixed(2) || '0.00' }}</el-descriptions-item>
          <el-descriptions-item label="总佣金">¥{{ detailData.totalCommission?.toFixed(2) || '0.00' }}</el-descriptions-item>
          <el-descriptions-item label="已结算"><span style="color:#22c55e;">¥{{ detailData.settledCommission?.toFixed(2) || '0.00' }}</span></el-descriptions-item>
          <el-descriptions-item label="当前分润比例">{{ detailData.commissionRate }}%</el-descriptions-item>
          <el-descriptions-item label="待结算"><span style="color:#f59e0b;">¥{{ detailData.pendingCommission?.toFixed(2) || '0.00' }}</span></el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- Trend Chart -->
      <div v-if="detailData?.trend?.length" style="margin-bottom:20px;">
        <h4 style="margin-bottom:8px;font-weight:500;">月度分润趋势</h4>
        <div style="display:flex;align-items:end;gap:8px;padding:12px 0;border-bottom:2px solid #e5e7eb;">
          <div v-for="t in detailData.trend" :key="t.month" style="display:flex;flex-direction:column;align-items:center;flex:1;">
            <span style="font-size:11px;color:#6b7280;margin-bottom:4px;">¥{{ t.amount.toFixed(0) }}</span>
            <div :style="{height: Math.max(20, t.amount / maxTrend * 100) + 'px', width:'100%', background:'linear-gradient(180deg,#667eea,#764ba2)', borderRadius:'4px 4px 0 0'}"></div>
            <span style="font-size:10px;color:#9ca3af;margin-top:4px;">{{ t.month }}</span>
          </div>
        </div>
      </div>

      <!-- Settlement Records -->
      <div v-if="detailData?.settlementRecords?.length" style="margin-bottom:16px;">
        <h4 style="margin-bottom:8px;font-weight:500;">结算记录</h4>
        <el-table :data="detailData.settlementRecords" stripe size="small">
          <el-table-column label="结算金额" width="120">
            <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="凭证" width="120">
            <template #default="{ row }">
              <el-image v-if="row.proofUrl" :src="row.proofUrl" style="width:60px;height:60px;border-radius:4px;" fit="cover" />
              <span v-else style="color:#999;">无</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" width="200">
            <template #default="{ row }">{{ row.adminNote || '-' }}</template>
          </el-table-column>
          <el-table-column label="结算时间" width="170">
            <template #default="{ row }">{{ row.settledAt ? formatDate(row.settledAt) : '-' }}</template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Commission List -->
      <h4 style="margin-bottom:8px;font-weight:500;">分润明细</h4>
      <el-table :data="paginatedDetailCommissions" stripe size="small" max-height="400" style="width:100%;">
        <el-table-column type="index" label="序号" width="60" :index="(i) => i + 1" />
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column label="订单金额" width="120">
          <template #default="{ row }">¥{{ row.orderAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="分润比例" width="100">
          <template #default="{ row }">{{ row.rate }}%</template>
        </el-table-column>
        <el-table-column label="分润金额" width="120">
          <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.orderStatus)" size="small">{{ orderStatusLabel(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单创建时间" width="170">
          <template #default="{ row }">{{ row.orderCreatedAt ? formatDate(row.orderCreatedAt) : (row.createdAt ? formatDate(row.createdAt) : '-') }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="detailCommPage"
        :page-size="detailCommPageSize"
        :total="detailCommTotal"
        layout="total, prev, pager, next"
        style="margin-top:12px;"
      />
    </el-dialog>

    <!-- Settle Dialog -->
    <el-dialog v-model="settleVisible" title="结算" width="500px">
      <p>销售: <strong>{{ settleTarget?.salesName }}</strong></p>
      <p>待结算金额: <strong style="color:#f59e0b;">¥{{ settleTarget?.pendingCommission?.toFixed(2) }}</strong></p>
      <el-form label-width="100px" style="margin-top:16px;">
        <el-form-item label="结算金额">
          <el-input-number v-model="settleAmount" :min="0" :max="settleTarget?.pendingCommission || 0" :precision="2" style="width:200px;" />
        </el-form-item>

        <!-- Sales payment code -->
        <el-form-item label="收款码">
          <div v-if="settlePaymentCode">
            <img :src="settlePaymentCode" style="max-width:200px;max-height:200px;border-radius:8px;border:1px solid #e5e7eb;" />
          </div>
          <span v-else style="color:#999;font-size:12px;">销售未上传收款码</span>
        </el-form-item>

        <el-form-item label="转账凭证">
          <el-upload :action="uploadUrl" :headers="uploadHeaders" :on-success="handleProofUploaded" :show-file-list="false" accept="image/*">
            <el-button size="small" type="primary">上传凭证截图</el-button>
          </el-upload>
          <div v-if="settleProofUrl" style="margin-top:8px;">
            <img :src="settleProofUrl" style="max-width:200px;max-height:200px;border-radius:8px;border:1px solid #e5e7eb;" />
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="settleNote" type="textarea" :rows="2" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settleVisible = false">取消</el-button>
        <el-button type="success" @click="doSettle" :loading="settling">确认结算</el-button>
      </template>
    </el-dialog>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/components/AdminLayout.vue'
import request from '@/utils/request'

const loading = ref(false)
const summary = ref([])
// 汇总表分页
const summaryPage = ref(1)
const summaryPageSize = ref(10)
const summaryTotal = computed(() => summary.value.length)
const paginatedSummary = computed(() => {
  const start = (summaryPage.value - 1) * summaryPageSize.value
  return summary.value.slice(start, start + summaryPageSize.value)
})
// 明细弹窗分页
const detailCommPage = ref(1)
const detailCommPageSize = ref(10)
const detailCommTotal = computed(() => detailData.value?.commissions?.length || 0)
const paginatedDetailCommissions = computed(() => {
  const all = detailData.value?.commissions || []
  const start = (detailCommPage.value - 1) * detailCommPageSize.value
  return all.slice(start, start + detailCommPageSize.value)
})
const detailVisible = ref(false)
const detailData = ref(null)
const detailTitle = ref('')
const settleVisible = ref(false)
const settleTarget = ref(null)
const settleAmount = ref(0)
const settlePaymentCode = ref('')
const settleProofUrl = ref('')
const settleNote = ref('')
const settling = ref(false)
const uploadUrl = '/api/admin/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const maxTrend = computed(() => {
  if (!detailData.value?.trend?.length) return 1
  return Math.max(...detailData.value.trend.map(t => t.amount), 1)
})

const STATUS_MAP = {
  pending: '待支付',
  paid: '已支付',
  delivered: '已发货',
  settled: '已结算',
  rejected: '已拒绝'
}

const STATUS_TYPE_MAP = {
  pending: 'info',
  paid: 'warning',
  delivered: 'success',
  settled: 'success',
  rejected: 'danger'
}

function orderStatusLabel(status) {
  return STATUS_MAP[status] || status
}
function orderStatusType(status) {
  return STATUS_TYPE_MAP[status] || 'info'
}

async function loadSummary() {
  loading.value = true
  try {
    const res = await request.get('/admin/commissions/summary')
    summary.value = res.data || []
    summaryPage.value = 1
  } catch(e) { console.error(e) }
  finally { loading.value = false }
}

async function showDetail(row) {
  detailTitle.value = row.salesName + ' - 分润明细'
  try {
    const res = await request.get('/admin/commissions/sales/' + row.salesId)
    detailData.value = res.data
    detailCommPage.value = 1
    detailVisible.value = true
  } catch(e) { ElMessage.error('获取明细失败') }
}

async function openSettleDialog(row) {
  settleTarget.value = row
  settleAmount.value = row.pendingCommission || 0
  settlePaymentCode.value = ''
  settleProofUrl.value = ''
  settleNote.value = ''
  try {
    const res = await request.get('/sales/payment-code?salesId=' + row.salesId)
    const codes = res.data || []
    const wechat = codes.find(c => c.codeType === 'wechat')
    if (wechat) settlePaymentCode.value = wechat.codeUrl
  } catch(e) {}
  settleVisible.value = true
}

function handleProofUploaded(res) {
  if (res.data?.url) settleProofUrl.value = res.data.url
}

async function doSettle() {
  if (!settleAmount.value || settleAmount.value <= 0) {
    ElMessage.warning('请输入结算金额')
    return
  }
  settling.value = true
  try {
    const adminId = localStorage.getItem('adminId')
    const res = await request.post('/admin/commissions/settle', {
      salesId: settleTarget.value.salesId,
      amount: String(settleAmount.value),
      adminId: adminId,
    })
    // Upload proof if available
    const settleId = res.data?.id
    if (settleProofUrl.value && settleId) {
      await request.put('/admin/commissions/settle/' + settleId + '/proof', {
        proofUrl: settleProofUrl.value,
        note: settleNote.value,
      })
    }
    ElMessage.success('结算成功')
    settleVisible.value = false
    loadSummary()
  } catch(e) {
    ElMessage.error(e.response?.data?.message || '结算失败')
  } finally { settling.value = false }
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN')
}

onMounted(loadSummary)
</script>
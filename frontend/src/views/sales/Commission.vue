<template>
  <SalesLayout>
    <h2>我的分润</h2>

    <!-- Stats -->
    <el-row :gutter="16" style="margin-bottom:20px;">
      <el-col :span="6">
        <el-card><div class="stat-card"><div class="stat-label">总佣金</div><div class="stat-value">¥{{ (data.totalCommission || 0).toFixed(2) }}</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="stat-card"><div class="stat-label">已结算</div><div class="stat-value" style="color:#22c55e;">¥{{ (data.settledCommission || 0).toFixed(2) }}</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="stat-card"><div class="stat-label">待结算</div><div class="stat-value" style="color:#f59e0b;">¥{{ (data.pendingCommission || 0).toFixed(2) }}</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="stat-card"><div class="stat-label">当前分润比例</div><div class="stat-value" style="color:#667eea;">{{ data.commissionRate || 0 }}%</div></div></el-card>
      </el-col>
    </el-row>

    <!-- Settlement Records + 分润变化 -->
    <el-row :gutter="16" style="margin-bottom:20px;">
      <el-col :span="14" v-if="data.settlementRecords?.length">
        <el-card>
          <template #header>结算记录</template>
          <el-table :data="data.settlementRecords" stripe size="small">
            <el-table-column label="结算金额" width="120">
              <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="凭证" width="100">
              <template #default="{ row }">
                <el-image v-if="row.proofUrl" :src="row.proofUrl" style="width:50px;height:50px;border-radius:4px;" fit="cover" />
                <span v-else style="color:#999;">无</span>
              </template>
            </el-table-column>
            <el-table-column label="备注" width="150">
              <template #default="{ row }">{{ row.adminNote || '-' }}</template>
            </el-table-column>
            <el-table-column label="结算时间" width="160">
              <template #default="{ row }">{{ row.settledAt ? formatDate(row.settledAt) : '-' }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="data.settlementRecords?.length ? 10 : 24">
        <el-card>
          <template #header>分润比例变更记录</template>
          <el-table :data="rateLogs" stripe size="small" v-if="rateLogs.length">
            <el-table-column label="变更前" width="90">
              <template #default="{ row }">{{ row.oldRate }}%</template>
            </el-table-column>
            <el-table-column label="变更后" width="90">
              <template #default="{ row }"><span style="color:#e6a23c;">{{ row.newRate }}%</span></template>
            </el-table-column>
            <el-table-column label="变更时间" width="160">
              <template #default="{ row }">{{ row.createdAt ? formatDate(row.createdAt) : '-' }}</template>
            </el-table-column>
          </el-table>
          <div v-else style="text-align:center;color:#909399;padding:20px 0;">暂无比例变更记录</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Commission List -->
    <el-card>
      <template #header>分润明细</template>
      <el-table :data="paginatedCommissions" stripe size="small" style="width:100%;">
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
        v-model:current-page="commissionPage"
        :page-size="commissionPageSize"
        :total="commissionTotal"
        layout="total, prev, pager, next"
        style="margin-top:12px;"
      />
    </el-card>
  </SalesLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import SalesLayout from '@/components/SalesLayout.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const authStore = useAuthStore()
const data = ref({})
const rateLogs = ref([])
const commissionPage = ref(1)
const commissionPageSize = ref(10)
const commissionTotal = computed(() => data.value.commissions?.length || 0)
const paginatedCommissions = computed(() => {
  const all = data.value.commissions || []
  const start = (commissionPage.value - 1) * commissionPageSize.value
  return all.slice(start, start + commissionPageSize.value)
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
function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN')
}

onMounted(async () => {
  const salesId = authStore.userInfo?.id
  if (!salesId) return
  try {
    const [commRes, logsRes] = await Promise.all([
      request.get('/sales/commissions?salesId=' + salesId),
      request.get('/sales/rate-logs?salesId=' + salesId)
    ])
    data.value = commRes.data || {}
    rateLogs.value = logsRes.data || []
    commissionPage.value = 1
  } catch(e) {
    console.error(e)
  }
})
</script>

<style scoped>
.stat-card { text-align:center; padding:8px 0; }
.stat-label { font-size:13px; color:#6b7280; margin-bottom:8px; }
.stat-value { font-size:28px; font-weight:700; color:#1f2937; }
</style>
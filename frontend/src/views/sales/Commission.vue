<template>
  <SalesLayout>
    <h2>我的分润</h2>

    <el-row :gutter="16" style="margin-bottom:20px;">
      <el-col :span="8">
        <el-card><div class="stat-card"><div class="stat-label">总佣金</div><div class="stat-value">¥{{ (data.totalCommission || 0).toFixed(2) }}</div></div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="stat-card"><div class="stat-label">已结算</div><div class="stat-value" style="color:#22c55e;">¥{{ (data.settledCommission || 0).toFixed(2) }}</div></div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="stat-card"><div class="stat-label">待结算</div><div class="stat-value" style="color:#f59e0b;">¥{{ (data.pendingCommission || 0).toFixed(2) }}</div></div></el-card>
      </el-col>
    </el-row>

    <el-card style="margin-bottom:20px;">
      <template #header>当前分润比例</template>
      <div style="font-size:24px;font-weight:700;color:#667eea;">{{ data.commissionRate || 0 }}%</div>
    </el-card>

    <el-table :data="data.commissions || []" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="orderId" label="订单" width="80" />
      <el-table-column prop="amount" label="分润" width="100">
        <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="rate" label="比例" width="80">{{ row.rate }}%</el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'settled' ? 'success' : 'info'" size="small">
            {{ row.status === 'settled' ? '已结算' : '待结算' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="160" />
    </el-table>
  </SalesLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import SalesLayout from '@/components/SalesLayout.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const authStore = useAuthStore()
const data = ref({})

onMounted(async () => {
  try {
    const res = await request.get('/sales/commissions?salesId=' + authStore.userInfo?.id)
    data.value = res.data || {}
  } catch(e) {}
})
</script>

<style scoped>
.stat-card { text-align:center; padding:8px 0; }
.stat-label { font-size:13px; color:#6b7280; margin-bottom:8px; }
.stat-value { font-size:28px; font-weight:700; color:#1f2937; }
</style>

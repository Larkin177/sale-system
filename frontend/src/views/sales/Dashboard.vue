<template>
  <SalesLayout>
    <h2>我的业绩</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalOrders || 0 }}</div>
            <div class="stat-label">总成交单数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">¥{{ (stats.totalAmount || 0).toFixed(2) }}</div>
            <div class="stat-label">总成交额</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ stats.monthOrders || 0 }}</div>
            <div class="stat-label">本月成交单数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">¥{{ (stats.monthAmount || 0).toFixed(2) }}</div>
            <div class="stat-label">本月成交额</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="recent-orders">
      <template #header>
        <span>最近订单</span>
      </template>
      <el-table :data="recentOrders" stripe>
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column prop="amount" label="金额">
          <template #default="{ row }">
            ¥{{ row.amount?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'paid' ? 'success' : 'info'">
              {{ row.status === 'paid' ? '已支付' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" />
      </el-table>
    </el-card>
  </SalesLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import SalesLayout from '@/components/SalesLayout.vue'
import request from '@/utils/request'

const stats = ref({})
const recentOrders = ref([])

onMounted(async () => {
  try {
    const [statsRes, ordersRes] = await Promise.all([
      request.get('/orders/stats'),
      request.get('/orders/my?size=5')
    ])
    stats.value = statsRes.data
    recentOrders.value = ordersRes.data?.records || []
  } catch (e) {
    console.error('获取数据失败')
  }
})
</script>

<style scoped>
.stats-row {
  margin-bottom: 20px;
}
.stat-item {
  text-align: center;
  padding: 20px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 10px;
}
.stat-label {
  color: #666;
}
.recent-orders {
  margin-top: 20px;
}
</style>

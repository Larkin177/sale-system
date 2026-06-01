<template>
  <AdminLayout>
    <h2>数据总览</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">¥{{ stats.totalRevenue || 0 }}</div>
            <div class="stat-label">总营收</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">¥{{ stats.totalCommission || 0 }}</div>
            <div class="stat-label">总分润</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalOrders || 0 }}</div>
            <div class="stat-label">总订单数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ stats.activeSales || 0 }}</div>
            <div class="stat-label">活跃销售</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import request from '@/utils/request'

const stats = ref({})

onMounted(async () => {
  try {
    const res = await request.get('/admin/stats')
    stats.value = res.data || {}
  } catch (e) {
    console.error('获取统计数据失败')
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
</style>

<template>
  <AdminLayout>
    <h2>销售详情</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ salesInfo.name || '-' }}</div>
            <div class="stat-label">销售名称</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ salesInfo.phone || '-' }}</div>
            <div class="stat-label">手机号</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ salesInfo.code || '-' }}</div>
            <div class="stat-label">推广码</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ salesInfo.commissionRate || 0 }}%</div>
            <div class="stat-label">分润比例</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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
        <span>订单列表</span>
      </template>
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
            {{ row.paymentMethod === 'wechat' ? '微信' : row.paymentMethod === 'alipay' ? '支付宝' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column prop="paidAt" label="支付时间" />
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="10"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadOrders"
      />
    </el-card>

    <div class="back-btn">
      <el-button @click="$router.push('/admin/sales')">返回列表</el-button>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AdminLayout from '@/components/AdminLayout.vue'
import request from '@/utils/request'

const route = useRoute()
const salesId = route.params.id

const salesInfo = ref({})
const stats = ref({})
const orders = ref([])
const page = ref(1)
const total = ref(0)

const loadSalesInfo = async () => {
  try {
    const res = await request.get(`/admin/sales/${salesId}`)
    salesInfo.value = res.data || {}
  } catch (e) {
    console.error('获取销售信息失败')
  }
}

const loadStats = async () => {
  try {
    const res = await request.get(`/admin/sales/${salesId}/stats`)
    stats.value = res.data || {}
  } catch (e) {
    console.error('获取统计数据失败')
  }
}

const loadOrders = async () => {
  try {
    const res = await request.get(`/admin/sales/${salesId}/orders?page=${page.value}&size=10`)
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取订单失败')
  }
}

const getStatusType = (status) => {
  const map = { pending: 'info', paid: 'success', delivered: 'success', redeemed: '', settled: '' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待支付', paid: '已支付', delivered: '已发货', redeemed: '已核销', settled: '已结算' }
  return map[status] || status
}

onMounted(() => {
  loadSalesInfo()
  loadStats()
  loadOrders()
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
.back-btn {
  margin-top: 20px;
  text-align: center;
}
.el-pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>

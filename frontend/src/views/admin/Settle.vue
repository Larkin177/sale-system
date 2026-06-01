<template>
  <AdminLayout>
    <h2>分润结算</h2>

    <el-table :data="commissions" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="orderId" label="订单ID" />
      <el-table-column prop="salesId" label="销售ID" />
      <el-table-column prop="amount" label="销售分润">
        <template #default="{ row }">
          ¥{{ row.amount?.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="adminAmount" label="管理员抽成">
        <template #default="{ row }">
          ¥{{ row.adminAmount?.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="rate" label="分润比例">
        <template #default="{ row }">
          {{ row.rate }}%
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 'settled' ? 'success' : 'info'">
            {{ row.status === 'settled' ? '已结算' : '待结算' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="10"
      :total="total"
      layout="prev, pager, next"
      @current-change="loadCommissions"
    />
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import request from '@/utils/request'

const commissions = ref([])
const page = ref(1)
const total = ref(0)

const loadCommissions = async () => {
  try {
    const res = await request.get(`/admin/commissions?page=${page.value}&size=10`)
    commissions.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取分润记录失败')
  }
}

onMounted(loadCommissions)
</script>

<style scoped>
.el-pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>

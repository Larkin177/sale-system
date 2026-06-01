<template>
  <AdminLayout>
    <h2>系统配置</h2>

    <el-card>
      <el-form :model="config" label-width="120px">
        <el-form-item label="基础价格">
          <el-input-number v-model="config.base_price" :min="0" :step="1" />
          <span style="margin-left: 10px;">元</span>
        </el-form-item>
        <el-form-item label="最低定价">
          <el-input-number v-model="config.min_price" :min="0" :step="1" />
          <span style="margin-left: 10px;">元</span>
        </el-form-item>
        <el-form-item label="最高定价">
          <el-input-number v-model="config.max_price" :min="0" :step="1" />
          <span style="margin-left: 10px;">元</span>
        </el-form-item>
        <el-form-item label="默认分润比例">
          <el-input-number v-model="config.default_commission_rate" :min="0" :max="100" :step="1" />
          <span style="margin-left: 10px;">%</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveConfig" :loading="loading">
            保存配置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)

const config = ref({
  base_price: 99,
  min_price: 80,
  max_price: 150,
  default_commission_rate: 10
})

onMounted(async () => {
  try {
    const res = await request.get('/admin/config')
    if (res.data) {
      config.value = {
        base_price: parseInt(res.data.base_price) || 99,
        min_price: parseInt(res.data.min_price) || 80,
        max_price: parseInt(res.data.max_price) || 150,
        default_commission_rate: parseInt(res.data.default_commission_rate) || 10
      }
    }
  } catch (e) {
    console.error('获取配置失败')
  }
})

const saveConfig = async () => {
  loading.value = true
  try {
    for (const [key, value] of Object.entries(config.value)) {
      await request.put(`/admin/config/${key}`, { value: String(value) })
    }
    ElMessage.success('配置保存成功')
  } catch (e) {
    console.error('保存配置失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.el-card {
  max-width: 600px;
}
</style>

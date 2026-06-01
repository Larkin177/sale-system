<template>
  <AdminLayout>
    <div class="header">
      <h2>销售管理</h2>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon> 添加销售
      </el-button>
    </div>

    <el-table :data="salesList" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="code" label="推广码" />
      <el-table-column prop="commissionRate" label="分润比例">
        <template #default="{ row }">
          {{ row.commissionRate }}%
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
            {{ row.status === 'active' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="primary" link @click="showEditDialog(row)">编辑</el-button>
          <el-button :type="row.status === 'active' ? 'danger' : 'success'" link @click="toggleStatus(row)">
            {{ row.status === 'active' ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="10"
      :total="total"
      layout="prev, pager, next"
      @current-change="loadSales"
    />

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑销售' : '添加销售'" width="400px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input v-model="form.password" type="password" :placeholder="isEdit ? '留空不修改' : '请输入密码'" show-password />
        </el-form-item>
        <el-form-item label="分润比例" prop="commissionRate">
          <el-input-number v-model="form.commissionRate" :min="0" :max="100" :step="1" />
          <span style="margin-left: 10px;">%</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const salesList = ref([])
const page = ref(1)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)

const form = ref({
  name: '',
  phone: '',
  password: '',
  commissionRate: 10
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const loadSales = async () => {
  try {
    const res = await request.get(`/admin/sales?page=${page.value}&size=10`)
    salesList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取销售列表失败')
  }
}

const showAddDialog = () => {
  isEdit.value = false
  editId.value = null
  form.value = { name: '', phone: '', password: '', commissionRate: 10 }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.value = { name: row.name, phone: row.phone, password: '', commissionRate: row.commissionRate }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await request.put(`/admin/sales/${editId.value}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/admin/sales', form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadSales()
  } catch (e) {
    console.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

const toggleStatus = async (row) => {
  try {
    await request.put(`/admin/sales/${row.id}/toggle-status`)
    ElMessage.success('操作成功')
    loadSales()
  } catch (e) {
    console.error('操作失败')
  }
}

onMounted(loadSales)
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.el-pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>

<template>
  <AdminLayout>
    <div class="packages-page">
      <div class="page-header">
        <h2>套餐管理</h2>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon> 新增套餐
        </el-button>
      </div>

      <!-- 产品筛选 -->
      <el-card class="filter-card" shadow="never">
        <el-select v-model="filterProductId" placeholder="选择产品筛选" clearable @change="loadPackages" style="width: 240px;">
          <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-card>

      <!-- 套餐列表 -->
      <el-card shadow="never">
        <el-table :data="packages" stripe v-loading="loading">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="套餐名称" min-width="140" />
          <el-table-column prop="platform" label="平台" width="100">
            <template #default="{ row }">
              <el-tag :type="row.platform === 'mac' ? 'primary' : row.platform === 'windows' ? 'success' : 'info'" size="small">
                {{ row.platform === 'mac' ? 'Mac' : row.platform === 'windows' ? 'Windows' : '全平台' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="80" />
          <el-table-column prop="price" label="价格" width="90">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="60" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
                {{ row.status === 'active' ? '上架' : '下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="authEnabled" label="授权码" width="80">
            <template #default="{ row }">
              <el-tag :type="row.authEnabled ? 'success' : 'info'" size="small">
                {{ row.authEnabled ? '开启' : '关闭' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openDialog(row)">编辑</el-button>
              <el-button size="small" :type="row.status === 'active' ? 'warning' : 'success'" @click="toggleStatus(row)">
                {{ row.status === 'active' ? '下架' : '上架' }}
              </el-button>
              <el-popconfirm title="确认删除此套餐？" @confirm="deletePackage(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="page"
          :page-size="20"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadPackages"
          style="margin-top: 16px; justify-content: center;"
        />
      </el-card>

      <!-- 新增/编辑弹窗 -->
      <el-dialog v-model="dialogVisible" :title="editingId ? '编辑套餐' : '新增套餐'" width="560px" destroy-on-close>
        <el-form :model="form" label-width="90px" label-position="left">
          <el-form-item label="所属产品" required>
            <el-select v-model="form.productId" placeholder="选择产品" style="width: 100%;">
              <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="套餐名称" required>
            <el-input v-model="form.name" placeholder="如: Claude Code" />
          </el-form-item>
          <el-form-item label="平台" required>
            <el-select v-model="form.platform" placeholder="选择平台" style="width: 100%;">
              <el-option label="Mac" value="mac" />
              <el-option label="Windows" value="windows" />
              <el-option label="全平台" value="all" />
            </el-select>
          </el-form-item>
          <el-form-item label="版本号">
            <el-input v-model="form.version" placeholder="如: 1.0.0" />
          </el-form-item>
          <el-form-item label="套餐描述">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="套餐描述信息" />
          </el-form-item>
          <el-form-item label="价格(元)" required>
            <el-input-number v-model="form.price" :min="0.01" :precision="2" style="width: 200px;" />
          </el-form-item>
          <el-form-item label="销售价格范围">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-input-number v-model="form.minPrice" :min="0" :precision="2" size="small" style="width: 120px;" />
              <span>~</span>
              <el-input-number v-model="form.maxPrice" :min="0" :precision="2" size="small" style="width: 120px;" />
            </div>
            <div style="font-size: 12px; color: #909399; margin-top: 4px;">销售生成推广链接时的自定义价格范围</div>
          </el-form-item>
          <el-form-item label="下载链接">
            <div style="display: flex; gap: 8px;">
              <el-input v-model="form.downloadUrl" placeholder="软件下载地址URL" style="flex: 1" />
              <el-upload
                :action="uploadUrl"
                :headers="uploadHeaders"
                :on-success="handleDownloadUploaded"
                :show-file-list="false"
                accept=".zip,.rar,.7z,.exe,.msi,.dmg,.pkg,.tar.gz,.gz"
              >
                <el-button size="small" type="primary">上传文件</el-button>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item label="授权码">
            <el-switch v-model="form.authEnabled" />
            <span style="margin-left: 12px; color: #909399; font-size: 12px;">开启后发货时自动生成授权码</span>
          </el-form-item>
          <el-form-item label="授权有效期" v-if="form.authEnabled">
            <el-input-number v-model="form.authValidityHours" :min="1" :max="8760" />
            <span style="margin-left: 8px; color: #909399;">小时</span>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
            <span style="margin-left: 8px; color: #909399;">数字越小越靠前</span>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="savePackage" :loading="saving">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import AdminLayout from '@/components/AdminLayout.vue'
import request from '@/utils/request'
const uploadUrl = '/api/admin/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const packages = ref([])
const products = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const filterProductId = ref(null)

const form = reactive({
  productId: null,
  name: '',
  platform: 'mac',
  version: '',
  description: '',
  price: 99,
  minPrice: 80,
  maxPrice: 150,
  downloadUrl: '',
  authEnabled: true,
  authValidityHours: 72,
  sortOrder: 0
})

const loadProducts = async () => {
  try {
    const res = await request.get('/admin/products?page=1&size=100')
    products.value = res.data?.records || []
  } catch (e) {
    console.error('加载产品失败')
  }
}

const loadPackages = async () => {
  loading.value = true
  try {
    let url = `/admin/packages?page=${page.value}&size=20`
    if (filterProductId.value) url += `&productId=${filterProductId.value}`
    const res = await request.get(url)
    packages.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载套餐失败')
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  editingId.value = row ? row.id : null
  if (row) {
    Object.assign(form, {
      productId: row.productId,
      name: row.name,
      platform: row.platform,
      version: row.version || '',
      description: row.description || '',
      price: row.price,
      minPrice: row.minPrice || row.price,
      maxPrice: row.maxPrice || row.price,
      downloadUrl: row.downloadUrl || '',
      authEnabled: row.authEnabled !== false,
      authValidityHours: row.authValidityHours || 72,
      sortOrder: row.sortOrder || 0
    })
  } else {
    Object.assign(form, {
      productId: products.value.length > 0 ? products.value[0].id : null,
      name: '', platform: 'mac', version: '', description: '',
      price: 99, minPrice: 80, maxPrice: 150,
      downloadUrl: '', authEnabled: true, authValidityHours: 72, sortOrder: 0
    })
  }
  dialogVisible.value = true
}

const handleDownloadUploaded = (res) => {
  if (res.data?.url) {
    form.downloadUrl = res.data.url
    ElMessage.success('文件上传成功')
  }
}

const savePackage = async () => {
  if (!form.productId || !form.name || !form.platform || !form.price) {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/admin/packages/${editingId.value}`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/admin/packages', form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadPackages()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  try {
    await request.put(`/admin/packages/${row.id}/toggle-status`)
    ElMessage.success('状态已切换')
    loadPackages()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deletePackage = async (id) => {
  try {
    await request.delete(`/admin/packages/${id}`)
    ElMessage.success('删除成功')
    loadPackages()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadProducts()
  loadPackages()
})
</script>

<style scoped>
.packages-page {
  max-width: 1200px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
}
.filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
</style>

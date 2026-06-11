<template>
  <AdminLayout>
    <div class="tutorials-header">
      <h2>教程内容管理</h2>
      <el-button type="primary" @click="openDialog(null)">新增教程</el-button>
    </div>

    <el-table :data="paginatedTutorials" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="150" />
      <el-table-column prop="category" label="分类" width="130">
        <template #default="{ row }">
          <el-tag>{{ getCategoryLabel(row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 'video' ? 'danger' : row.type === 'image' ? 'warning' : 'info'" size="small">
            {{ row.type === 'video' ? '视频' : row.type === 'image' ? '图片' : '图文' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="60" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'">{{ row.status === 'active' ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top:16px;"
    />

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑教程' : '新增教程'" width="650px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="教程标题" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option v-for="cat in categories" :key="cat.slug" :label="cat.name" :value="cat.slug" />
            <el-option disabled style="border-top:1px solid #eee;margin-top:4px;padding-top:4px;">
              <span style="color:#909399;font-size:12px;">💡 在下方管理分类</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.type">
            <el-radio value="text">图文</el-radio>
            <el-radio value="image">图片</el-radio>
            <el-radio value="video">视频</el-radio>
          </el-radio-group>
        </el-form-item>
                <el-form-item v-if="form.type === 'text'" label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="支持HTML格式的教程内容..." />
          <div style="margin-top:6px;">
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :on-success="handleMdUploaded"
              :on-error="() => ElMessage.error('上传失败, 文件可能过大或格式不支持')"
              :show-file-list="false"
              accept=".md"
            >
              <el-button size="small" type="primary">上传 .md 文件</el-button>
            </el-upload>
            <span style="margin-left:8px;font-size:12px;color:#909399;">上传 Markdown 文件自动填入</span>
          </div>
        </el-form-item>
        <el-form-item v-if="form.type !== 'text'" label="媒体文件">
          <div>
            <el-input v-model="form.mediaUrl" placeholder="输入图片/视频URL，或点击上传" style="margin-bottom: 8px" />
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :on-success="handleMediaUploaded"
              :on-error="e => ElMessage.error(&#39;上传失败: &#39; + (e.message || &#39;未知错误&#39;))"
              :show-file-list="false"
              accept="image/*,video/*"
            >
              <el-button size="small" type="primary">上传文件</el-button>
            </el-upload>
          </div>
          <div v-if="form.mediaUrl" class="media-preview" style="margin-top: 8px;">
            <img v-if="form.type === 'image'" :src="form.mediaUrl" style="max-width: 300px; max-height: 200px; border-radius: 6px;" />
            <video v-if="form.type === 'video'" :src="form.mediaUrl" controls style="max-width: 300px; max-height: 200px;" />
          </div>
        </el-form-item>
        <el-form-item label="缩略图">
          <div style="display:flex;gap:8px;">
            <el-input v-model="form.thumbnailUrl" placeholder="缩略图URL（可选）" style="flex:1;" />
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :on-success="handleThumbUploaded"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small" type="primary">上传图片</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" active-value="active" inactive-value="disabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- Category Management -->
    <el-card style="max-width:600px;margin-top:20px;">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>分类管理</span>
          <el-button size="small" type="primary" @click="openCategoryDialog(null)">新增分类</el-button>
        </div>
      </template>
      <div v-for="cat in categories" :key="cat.id" style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid #f3f4f6;">
        <span>{{ cat.name }} <code style="font-size:11px;color:#909399;">({{ cat.slug }})</code></span>
        <div>
          <el-button size="small" text @click="openCategoryDialog(cat)">编辑</el-button>
          <el-popconfirm title="确定删除此分类？" @confirm="deleteCategory(cat.id)">
            <template #reference>
              <el-button size="small" text type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </el-card>

    <!-- Category Edit Dialog -->
    <el-dialog v-model="categoryDialog" :title="categoryForm.id ? '编辑分类' : '新增分类'" width="400px">
      <el-form label-width="60px">
        <el-form-item label="名称">
          <el-input v-model="categoryForm.name" placeholder="eg: 使用教程" />
        </el-form-item>
        <el-form-item label="标识">
          <el-input v-model="categoryForm.slug" placeholder="eg: tutorial" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const tutorials = ref([])
const categories = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = computed(() => tutorials.value.length)
const paginatedTutorials = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return tutorials.value.slice(start, start + pageSize.value)
})
const categoryDialog = ref(false)
const categoryForm = ref({ id: null, name: '', slug: '', sortOrder: 0 })
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const form = ref(getEmptyForm())
const uploadUrl = '/api/admin/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

function getEmptyForm() {
  return { id: null, title: '', type: 'text', content: '', mediaUrl: '', thumbnailUrl: '', category: 'claude-code', sortOrder: 0, status: 'active' }
}

function getCategoryLabel(c) {
  const map = { 'claude-code': 'Claude Code', codex: 'Codex', installer: '安装器' }
  return map[c] || c
}

function openDialog(row) {
  isEdit.value = !!row
  form.value = row ? { ...row } : getEmptyForm()
  dialogVisible.value = true
}

async function handleMdUploaded(res) {
  if (res.data?.url) {
    try {
      const r = await fetch(res.data.url)
      form.value.content = await r.text()
      ElMessage.success('.md 文件已加载')
    } catch(e) {
      ElMessage.error('读取文件失败')
    }
  }
}

function handleThumbUploaded(res) {
  if (res.data?.url) {
    form.value.thumbnailUrl = res.data.url
  }
}

function handleMediaUploaded(res) {
  if (res.data?.url) {
    form.value.mediaUrl = res.data.url
    // 视频上传时自动设置缩略图（后端生成的）
    if (res.data?.thumbnailUrl && !form.value.thumbnailUrl) {
      form.value.thumbnailUrl = res.data.thumbnailUrl
    }
  }
}

async function loadCategories() {
  try {
    const res = await request.get('/admin/categories')
    categories.value = res.data || []
  } catch(e) {}
}

function openCategoryDialog(cat) {
  categoryForm.value = cat ? { ...cat } : { id: null, name: '', slug: '', sortOrder: 0 }
  categoryDialog.value = true
}

async function saveCategory() {
  if (!categoryForm.value.name || !categoryForm.value.slug) {
    ElMessage.warning('请填写名称和标识')
    return
  }
  try {
    await request.post('/admin/categories', categoryForm.value)
    ElMessage.success('保存成功')
    categoryDialog.value = false
    loadCategories()
  } catch(e) {
    ElMessage.error('保存失败')
  }
}

async function deleteCategory(id) {
  try {
    await request.delete('/admin/categories/' + id)
    ElMessage.success('已删除')
    loadCategories()
  } catch(e) {
    ElMessage.error('删除失败')
  }
}

async function loadTutorials() {
  const res = await request.get('/admin/tutorials')
  tutorials.value = res.data || []
  page.value = 1
}

async function handleSave() {
  if (!form.value.title || !form.value.category) {
    ElMessage.warning('请填写标题和分类')
    return
  }
  saving.value = true
  try {
    await request.post('/admin/tutorials', form.value)
    ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
    dialogVisible.value = false
    loadTutorials()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  await request.delete(`/admin/tutorials/${id}`)
  ElMessage.success('已删除')
  loadTutorials()
}

onMounted(() => { loadTutorials(); loadCategories() })
</script>

<style scoped>
.tutorials-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.tutorials-header h2 { margin: 0; }
.media-preview img, .media-preview video { border: 1px solid #eee; }
</style>

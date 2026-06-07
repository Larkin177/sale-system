<template>
  <AdminLayout>
    <div class="tutorials-header">
      <h2>教程内容管理</h2>
      <el-button type="primary" @click="openDialog(null)">新增教程</el-button>
    </div>

    <el-table :data="tutorials" stripe>
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

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑教程' : '新增教程'" width="650px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="教程标题" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option label="Claude Code" value="claude-code" />
            <el-option label="Codex" value="codex" />
            <el-option label="安装器" value="installer" />
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
        </el-form-item>
        <el-form-item v-if="form.type !== 'text'" label="媒体文件">
          <div>
            <el-input v-model="form.mediaUrl" placeholder="输入图片/视频URL，或点击上传" style="margin-bottom: 8px" />
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :on-success="handleMediaUploaded"
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
          <el-input v-model="form.thumbnailUrl" placeholder="缩略图URL（可选）" />
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
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const tutorials = ref([])
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

function handleMediaUploaded(res) {
  if (res.data?.url) {
    form.value.mediaUrl = res.data.url
  }
}

async function loadTutorials() {
  const res = await request.get('/admin/tutorials')
  tutorials.value = res.data || []
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

onMounted(loadTutorials)
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

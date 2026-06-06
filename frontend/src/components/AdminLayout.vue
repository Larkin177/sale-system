<template>
  <div class="layout">
    <el-header class="header">
      <div class="header-left">
        <h1 class="logo">{{ siteName }} 管理端</h1>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ authStore.userInfo?.username || '管理员' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="main-container">
      <el-aside width="200px" class="aside">
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据总览</span>
          </el-menu-item>
          <el-menu-item index="/admin/sales">
            <el-icon><User /></el-icon>
            <span>销售管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/orders">
            <el-icon><List /></el-icon>
            <span>订单管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/packages">
            <el-icon><Goods /></el-icon>
            <span>套餐管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/settle">
            <el-icon><Money /></el-icon>
            <span>分润结算</span>
          </el-menu-item>
          <el-menu-item index="/admin/config">
            <el-icon><Setting /></el-icon>
            <span>系统配置</span>
          </el-menu-item>
          <el-menu-item index="/admin/site-settings">
            <el-icon><Setting /></el-icon>
            <span>站点设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <slot></slot>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getSiteSettings } from '@/api/config'
import { User, ArrowDown, SwitchButton, DataAnalysis, List, Money, Setting, Goods } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const siteName = ref('系统')

onMounted(async () => {
  try {
    const res = await getSiteSettings()
    if (res?.data?.site_name) {
      siteName.value = res.data.site_name
    }
  } catch (e) {
    // use default
  }
})

const handleCommand = (command) => {
  switch (command) {
    case 'logout':
      authStore.logout()
      ElMessage.success('已退出登录')
      router.push('/admin/login')
      break
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.header {
  background: linear-gradient(135deg, #30cfd3 0%, #330867 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.header-left .logo {
  color: white;
  font-size: 20px;
  margin: 0;
}
.header-right {
  display: flex;
  align-items: center;
}
.user-info {
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}
.user-info:hover {
  opacity: 0.9;
}
.main-container {
  height: calc(100vh - 60px);
}
.aside {
  background: white;
  box-shadow: 2px 0 8px rgba(0,0,0,0.05);
}
.main {
  background: #f5f7fa;
  padding: 20px;
}
</style>

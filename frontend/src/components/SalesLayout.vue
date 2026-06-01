<template>
  <div class="layout">
    <el-header class="header">
      <div class="header-left">
        <h1 class="logo">销售分润系统</h1>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ authStore.userInfo?.name || '销售' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon> 个人中心
              </el-dropdown-item>
              <el-dropdown-item command="link">
                <el-icon><Link /></el-icon> 推广链接
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
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
          <el-menu-item index="/s/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>业绩概览</span>
          </el-menu-item>
          <el-menu-item index="/s/orders">
            <el-icon><List /></el-icon>
            <span>我的订单</span>
          </el-menu-item>
          <el-menu-item index="/s/link">
            <el-icon><Link /></el-icon>
            <span>推广链接</span>
          </el-menu-item>
          <el-menu-item index="/s/claim">
            <el-icon><Claim /></el-icon>
            <span>认领订单</span>
          </el-menu-item>
          <el-menu-item index="/s/leaderboard">
            <el-icon><Trophy /></el-icon>
            <span>排行榜</span>
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
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, ArrowDown, Link, SwitchButton, DataAnalysis, List, Trophy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      // TODO: 打开个人中心弹窗
      ElMessage.info('个人中心功能开发中')
      break
    case 'link':
      router.push('/s/link')
      break
    case 'logout':
      authStore.logout()
      ElMessage.success('已退出登录')
      router.push('/s/login')
      break
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

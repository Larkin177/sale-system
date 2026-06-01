<template>
  <div class="home">
    <div class="hero">
      <h1>专业软件工具</h1>
      <p>高效、稳定、安全的解决方案</p>
      <el-button type="primary" size="large" @click="$router.push('/pay')">
        立即购买
      </el-button>
    </div>

    <div class="features">
      <el-card class="feature-card" v-for="item in features" :key="item.title">
        <el-icon :size="48" color="#409eff"><component :is="item.icon" /></el-icon>
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
      </el-card>
    </div>

    <!-- 排行榜 -->
    <div class="leaderboard" v-if="top3.length > 0">
      <h2>销售排行榜</h2>
      <el-table :data="top3" stripe>
        <el-table-column prop="rank" label="排名" width="80" />
        <el-table-column prop="name" label="销售" />
        <el-table-column prop="amount" label="成交额">
          <template #default="{ row }">
            ¥{{ row.amount?.toFixed(2) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Trophy, Star, Service } from '@element-plus/icons-vue'
import request from '@/utils/request'

const features = [
  { icon: 'Trophy', title: '品质保证', desc: '经过严格测试，稳定可靠' },
  { icon: 'Star', title: '持续更新', desc: '定期更新，功能不断增强' },
  { icon: 'Service', title: '技术支持', desc: '专业团队提供技术支持' }
]

const top3 = ref([])

onMounted(async () => {
  try {
    const res = await request.get('/leaderboard/top3')
    top3.value = res.data
  } catch (e) {
    console.error('获取排行榜失败', e)
  }
})
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.hero {
  text-align: center;
  padding: 100px 20px 60px;
  color: white;
}
.hero h1 {
  font-size: 48px;
  margin-bottom: 20px;
}
.hero p {
  font-size: 20px;
  margin-bottom: 40px;
  opacity: 0.9;
}
.features {
  display: flex;
  justify-content: center;
  gap: 30px;
  padding: 0 20px 60px;
  flex-wrap: wrap;
}
.feature-card {
  width: 280px;
  text-align: center;
  padding: 20px;
}
.feature-card h3 {
  margin: 20px 0 10px;
}
.feature-card p {
  color: #666;
}
.leaderboard {
  max-width: 600px;
  margin: 0 auto;
  padding: 40px 20px;
  background: white;
  border-radius: 12px;
}
.leaderboard h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}
</style>

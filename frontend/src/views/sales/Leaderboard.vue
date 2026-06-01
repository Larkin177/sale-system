<template>
  <SalesLayout>
    <h2>销售排行榜（本月）</h2>

    <el-card>
      <div class="podium" v-if="top3.length > 0">
        <!-- 第二名 -->
        <div class="podium-item second" v-if="top3[1]">
          <div class="avatar">2</div>
          <div class="name">{{ top3[1].name }}</div>
          <div class="amount">¥{{ top3[1].amount?.toFixed(2) }}</div>
          <div class="bar bar-2"></div>
        </div>

        <!-- 第一名 -->
        <div class="podium-item first" v-if="top3[0]">
          <div class="avatar">1</div>
          <div class="name">{{ top3[0].name }}</div>
          <div class="amount">¥{{ top3[0].amount?.toFixed(2) }}</div>
          <div class="bar bar-1"></div>
        </div>

        <!-- 第三名 -->
        <div class="podium-item third" v-if="top3[2]">
          <div class="avatar">3</div>
          <div class="name">{{ top3[2].name }}</div>
          <div class="amount">¥{{ top3[2].amount?.toFixed(2) }}</div>
          <div class="bar bar-3"></div>
        </div>
      </div>

      <el-empty v-else description="暂无数据" />
    </el-card>
  </SalesLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import SalesLayout from '@/components/SalesLayout.vue'
import request from '@/utils/request'

const top3 = ref([])

onMounted(async () => {
  try {
    const res = await request.get('/leaderboard/top3')
    top3.value = res.data || []
  } catch (e) {
    console.error('获取排行榜失败')
  }
})
</script>

<style scoped>
.podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  height: 400px;
  padding: 40px 20px 0;
}
.podium-item {
  text-align: center;
  margin: 0 20px;
}
.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  color: white;
  margin: 0 auto 10px;
}
.first .avatar { background: linear-gradient(135deg, #ffd700, #ffb800); }
.second .avatar { background: linear-gradient(135deg, #c0c0c0, #a0a0a0); }
.third .avatar { background: linear-gradient(135deg, #cd7f32, #b87333); }
.name {
  font-size: 16px;
  color: #333;
  margin-bottom: 5px;
}
.amount {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 10px;
}
.bar {
  width: 100px;
  border-radius: 8px 8px 0 0;
}
.bar-1 { height: 200px; background: linear-gradient(135deg, #ffd700, #ffb800); }
.bar-2 { height: 150px; background: linear-gradient(135deg, #c0c0c0, #a0a0a0); }
.bar-3 { height: 100px; background: linear-gradient(135deg, #cd7f32, #b87333); }
</style>

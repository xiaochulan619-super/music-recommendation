<template>
  <div>
    <h3>⭐ 智能推荐</h3>
    <el-card style="margin-bottom:20px">
      <div style="display:flex;align-items:center;justify-content:space-between">
        <div>
          <strong>训练状态：</strong>
          <el-tag v-if="trainStatus" :type="statusType">{{ statusText }}</el-tag>
          <span v-else style="color:#909399">暂无记录</span>
        </div>
        <el-button type="primary" @click="triggerTrain" :loading="training">
          🔄 开始训练
        </el-button>
      </div>
      <p v-if="trainStatus?.message" style="color:#909399;font-size:12px;margin-top:8px">
        {{ trainStatus.message?.substring(0, 200) }}
      </p>
    </el-card>

    <h3 v-if="recs.length">为你推荐</h3>
    <div class="song-grid">
      <el-card v-for="r in recs" :key="r.song_id" class="rec-card" shadow="hover"
        @click="$router.push(`/song/${r.song_id}`)">
        <img :src="r.cover_url || 'https://via.placeholder.com/150'" class="cover" />
        <div class="info">
          <div class="title">{{ r.title }}</div>
          <div class="artist">{{ r.artist_name || '未知' }}</div>
          <el-tag size="small" :type="r.source==='fusion'?'success':r.source==='ncf'?'primary':'warning'">
            {{ r.source }}
          </el-tag>
          <span style="font-size:12px;color:#909399;margin-left:6px">得分: {{ r.score?.toFixed(4) }}</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const trainStatus = ref(null)
const training = ref(false)
const recs = ref([])
let pollTimer = null

const statusText = computed(() => {
  const map = { pending: '等待中', running: '训练中...', completed: '已完成', failed: '失败' }
  return map[trainStatus.value?.status] || '未知'
})
const statusType = computed(() => {
  const map = { pending: 'info', running: 'warning', completed: 'success', failed: 'danger' }
  return map[trainStatus.value?.status] || 'info'
})

async function triggerTrain() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  training.value = true
  try {
    await request.post('/train/trigger')
    ElMessage.success('训练任务已启动')
    pollStatus()
  } catch (e) {} finally { training.value = false }
}

async function pollStatus() {
  try {
    const res = await request.get('/train/status')
    trainStatus.value = res.data
    if (trainStatus.value?.status === 'running' || trainStatus.value?.status === 'pending') {
      pollTimer = setTimeout(pollStatus, 3000)
    } else if (trainStatus.value?.status === 'completed') {
      loadRecs()
    }
  } catch (e) {}
}

async function loadRecs() {
  try {
    const res = await request.get('/recommendations', { params: { limit: 20 } })
    recs.value = res.data || []
  } catch (e) {}
}

onMounted(async () => {
  try {
    const res = await request.get('/train/status')
    trainStatus.value = res.data
  } catch (e) {}
  if (userStore.isLoggedIn) loadRecs()
})
</script>

<style scoped>
.song-grid { display: flex; flex-wrap: wrap; gap: 12px; }
.rec-card { cursor: pointer; width: 200px; }
.rec-card .cover { width: 100%; height: 150px; object-fit: cover; border-radius: 4px; }
.rec-card .info { margin-top: 8px; }
.rec-card .title { font-size: 14px; font-weight: bold; }
.rec-card .artist { font-size: 12px; color: #909399; margin: 4px 0; }
</style>

<template>
  <div>
    <h2>🏠 欢迎来到音乐推荐系统</h2>
    <p style="color:#909399;margin-bottom:30px">浏览歌曲、收藏喜欢的音乐，开启智能推荐之旅</p>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header><strong>🎵 热门推荐</strong><el-button text size="small" style="float:right" @click="$router.push('/recommend')">更多→</el-button></template>
          <div v-if="hotSongs.length" class="song-grid">
            <SongCard v-for="s in hotSongs.slice(0,4)" :key="s.id" :song="s" @click="goSong(s)" />
          </div>
          <p v-else style="color:#909399">暂无数据，<router-link to="/discover">去发现音乐</router-link></p>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><strong>📈 本周热榜</strong><el-button text size="small" style="float:right" @click="$router.push('/charts')">查看全部→</el-button></template>
          <div v-if="charts.length">
            <div v-for="(item, i) in charts.slice(0,5)" :key="i" class="chart-row">
              <span class="rank">{{ item.rank }}</span>
              <span class="name">{{ item.song?.title || '未知' }}</span>
              <span class="count">🔥 {{ item.playCount }}</span>
            </div>
          </div>
          <p v-else style="color:#909399">暂无数据</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import SongCard from '@/components/SongCard.vue'

const router = useRouter()
const hotSongs = ref([])
const charts = ref([])

function goSong(song) { router.push(`/song/${song.id}`) }

onMounted(async () => {
  try {
    const [songsRes, chartsRes] = await Promise.all([
      request.get('/songs', { params: { page: 1, size: 8 } }),
      request.get('/charts', { params: { type: 'weekly', limit: 5 } })
    ])
    hotSongs.value = songsRes.data.list
    charts.value = chartsRes.data || []
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.song-grid { display: flex; gap: 12px; flex-wrap: wrap; }
.chart-row { display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.rank { width: 28px; font-weight: bold; color: #e6a23c; }
.name { flex: 1; font-size: 14px; }
.count { font-size: 12px; color: #909399; }
</style>

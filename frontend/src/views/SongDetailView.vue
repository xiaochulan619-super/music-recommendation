<template>
  <div v-if="song">
    <el-row :gutter="20">
      <el-col :span="6">
        <img :src="song.coverUrl || 'https://via.placeholder.com/300'" style="width:100%;border-radius:8px" />
      </el-col>
      <el-col :span="18">
        <h2>{{ song.title }}</h2>
        <p>歌手：{{ song.artistName || '未知' }} | 专辑：{{ song.album || '未知' }} | 时长：{{ formatDuration(song.duration) }}</p>
        <div style="margin:16px 0">
          <el-button type="danger" @click="toggleFavorite" :icon="isFaved ? 'StarFilled' : 'Star'">
            {{ isFaved ? '已收藏' : '收藏' }}
          </el-button>
          <el-rate v-model="rating" @change="submitRating" style="display:inline-block;margin-left:12px" />
          <el-button @click="recordPlay" style="margin-left:12px">▶ 播放</el-button>
        </div>
      </el-col>
    </el-row>

    <el-divider />
    <h3>💬 评论 ({{ comments.length }})</h3>
    <div style="margin-bottom:16px">
      <el-input v-model="commentText" type="textarea" :rows="2" placeholder="写下你的评论..." />
      <el-button type="primary" @click="submitComment" style="margin-top:8px" :disabled="!commentText.trim()">发表评论</el-button>
    </div>
    <div v-for="c in comments" :key="c.id" style="padding:10px 0;border-bottom:1px solid #f0f0f0">
      <strong>{{ c.username }}</strong> <span style="color:#909399;font-size:12px">{{ c.createdAt }}</span>
      <p>{{ c.content }}</p>
      <div v-for="r in (c.replies||[])" :key="r.id" style="margin-left:30px;padding:6px 0">
        <strong>{{ r.username }}</strong>：{{ r.content }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const song = ref(null)
const isFaved = ref(false)
const rating = ref(0)
const comments = ref([])
const commentText = ref('')

function formatDuration(sec) {
  if (!sec) return '未知'
  return `${Math.floor(sec/60)}:${String(sec%60).padStart(2,'0')}`
}

async function loadSong() {
  const id = route.params.id
  try {
    const [sRes] = await Promise.all([
      request.get(`/songs/${id}`),
      loadComments()
    ])
    song.value = sRes.data
  } catch (e) {}
  if (userStore.isLoggedIn) {
    try {
      const fRes = await request.get(`/favorites/check/${id}`)
      isFaved.value = fRes.data
    } catch (e) {}
  }
}

async function loadComments() {
  try {
    const res = await request.get(`/comments/song/${route.params.id}`)
    comments.value = res.data || []
  } catch (e) {}
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  try {
    if (isFaved.value) {
      await request.delete(`/favorites/${song.value.id}`)
      isFaved.value = false
      ElMessage.success('取消收藏')
    } else {
      await request.post('/favorites', { songId: song.value.id })
      isFaved.value = true
      ElMessage.success('收藏成功')
    }
  } catch (e) {}
}

async function submitRating(val) {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  try {
    await request.post('/ratings', { songId: song.value.id, score: val })
    ElMessage.success(`评分 ${val} 星`)
  } catch (e) {}
}

async function recordPlay() {
  if (!userStore.isLoggedIn) return
  try { await request.post('/history', { songId: song.value.id }) } catch (e) {}
}

async function submitComment() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  try {
    await request.post('/comments', { songId: song.value.id, content: commentText.value })
    commentText.value = ''
    ElMessage.success('评论成功')
    loadComments()
  } catch (e) {}
}

onMounted(loadSong)
</script>

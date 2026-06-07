<template>
  <div>
    <h3>🔍 发现音乐</h3>
    <el-input v-model="keyword" placeholder="搜索歌曲名..." style="width:300px;margin-bottom:20px"
      @keyup.enter="fetchSongs" clearable />

    <div class="song-grid">
      <SongCard v-for="song in songs" :key="song.id" :song="song" @click="goSong" />
    </div>
    <p v-if="songs.length === 0" style="color:#909399;padding:40px 0;text-align:center">暂无歌曲数据</p>

    <div style="text-align:center;margin-top:20px" v-if="total > size">
      <el-pagination background layout="prev, pager, next"
        v-model:current-page="page" :page-size="size" :total="total" @current-change="fetchSongs" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import SongCard from '@/components/SongCard.vue'

const route = useRoute()
const router = useRouter()
const keyword = ref(route.query.keyword || '')
const songs = ref([])
const page = ref(1)
const size = 20
const total = ref(0)

function goSong(song) { router.push(`/song/${song.id}`) }

async function fetchSongs() {
  try {
    const res = await request.get('/songs', {
      params: { keyword: keyword.value, page: page.value, size }
    })
    songs.value = res.data.list
    total.value = res.data.total
  } catch (e) {}
}

onMounted(fetchSongs)
</script>

<style scoped>
.song-grid { display: flex; flex-wrap: wrap; gap: 12px; }
</style>

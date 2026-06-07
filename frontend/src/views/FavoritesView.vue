<template>
  <div>
    <h3>❤️ 我的收藏</h3>
    <div class="song-grid">
      <SongCard v-for="song in songs" :key="song.id" :song="song" @click="goSong" />
    </div>
    <p v-if="songs.length===0" style="color:#909399;padding:40px 0;text-align:center">还没有收藏歌曲，<router-link to="/discover">去发现</router-link></p>
    <div style="text-align:center;margin-top:20px" v-if="total > size">
      <el-pagination background layout="prev, pager, next"
        v-model:current-page="page" :page-size="size" :total="total" @current-change="loadFavs" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import SongCard from '@/components/SongCard.vue'

const router = useRouter()
const songs = ref([])
const page = ref(1), size = 20, total = ref(0)

function goSong(s) { router.push(`/song/${s.id}`) }

async function loadFavs() {
  try {
    const res = await request.get('/favorites', { params: { page: page.value, size } })
    songs.value = res.data.list
    total.value = res.data.total
  } catch (e) {}
}
onMounted(loadFavs)
</script>

<style scoped>
.song-grid { display: flex; flex-wrap: wrap; gap: 12px; }
</style>

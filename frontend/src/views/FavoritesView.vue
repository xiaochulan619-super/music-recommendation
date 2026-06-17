<template>
  <div class="page-wrap">
    <div class="page-hero"><h2 class="page-title">❤️ 我的收藏</h2><p class="page-desc">珍藏你喜欢的每一首歌</p></div>
    <div class="song-grid"><SongCard v-for="s in songs" :key="s.id" :song="s" @click="goSong" /></div>
    <div class="empty" v-if="!songs.length"><span class="empty-icon">💔</span><p>还没有收藏歌曲</p><router-link to="/discover" class="empty-link">去发现音乐 →</router-link></div>
    <div class="pagination-wrap" v-if="total>size"><el-pagination background layout="prev,pager,next" v-model:current-page="page" :page-size="size" :total="total" @current-change="loadFavs"/></div>
  </div>
</template>
<script setup>
import {ref,onMounted} from 'vue'; import {useRouter} from 'vue-router'; import request from '@/utils/request'; import SongCard from '@/components/SongCard.vue'
const router=useRouter(); const songs=ref([]); const page=ref(1),size=20,total=ref(0)
function goSong(s){router.push(`/song/${s.id}`)}
async function loadFavs(){try{const r=await request.get('/favorites',{params:{page:page.value,size}});songs.value=r.data.list||[];total.value=r.data.total||0}catch(e){}}
onMounted(loadFavs)
</script>
<style scoped>
.page-wrap{max-width:1280px;margin:0 auto;padding:0 20px 40px}
.page-hero{margin-bottom:20px}.page-title{font-size:26px;font-weight:800;color:var(--text-primary);margin:0}.page-desc{color:var(--text-muted);font-size:14px}
.song-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}
@media(max-width:1100px){.song-grid{grid-template-columns:repeat(3,1fr)}}@media(max-width:768px){.song-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:480px){.song-grid{grid-template-columns:1fr}}
.empty{text-align:center;padding:60px 0}.empty-icon{font-size:48px}.empty p{color:var(--text-muted)}.empty-link{color:var(--accent);text-decoration:none;font-weight:600}
.pagination-wrap{display:flex;justify-content:center;margin-top:28px}
</style>

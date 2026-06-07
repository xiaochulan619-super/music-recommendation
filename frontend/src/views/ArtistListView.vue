<template>
  <div>
    <h3>🎤 歌手</h3>
    <el-input v-model="keyword" placeholder="搜索歌手..." style="width:300px;margin-bottom:20px"
      @keyup.enter="loadArtists" clearable />
    <el-row :gutter="16">
      <el-col v-for="a in artists" :key="a.id" :span="4" style="margin-bottom:16px">
        <el-card shadow="hover" @click="$router.push(`/artist/${a.id}`)" style="cursor:pointer;text-align:center">
          <img :src="a.imageUrl || 'https://via.placeholder.com/100'" style="width:80px;height:80px;border-radius:50%" />
          <p>{{ a.name }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const keyword = ref('')
const artists = ref([])

async function loadArtists() {
  try {
    const res = await request.get('/artists', { params: { keyword: keyword.value, page: 1, size: 100 } })
    artists.value = res.data.list
  } catch (e) {}
}
onMounted(loadArtists)
</script>

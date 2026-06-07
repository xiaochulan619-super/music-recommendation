<template>
  <div>
    <h3>🏆 热门榜单</h3>
    <el-radio-group v-model="chartType" @change="loadCharts" style="margin-bottom:20px">
      <el-radio-button value="daily">日榜</el-radio-button>
      <el-radio-button value="weekly">周榜</el-radio-button>
      <el-radio-button value="total">总榜</el-radio-button>
    </el-radio-group>
    <el-table :data="charts" stripe style="width:100%">
      <el-table-column prop="rank" label="#" width="60" />
      <el-table-column label="封面" width="80">
        <template #default="{row}"><img :src="row.song?.coverUrl || 'https://via.placeholder.com/40'" style="width:40px;height:40px;border-radius:4px" /></template>
      </el-table-column>
      <el-table-column label="歌曲">
        <template #default="{row}">{{ row.song?.title || '未知' }} - {{ row.song?.artistName || '未知' }}</template>
      </el-table-column>
      <el-table-column prop="playCount" label="播放量" width="100" />
      <el-table-column prop="favCount" label="收藏量" width="100" />
      <el-table-column prop="hotScore" label="热度" width="100">
        <template #default="{row}">{{ row.hotScore?.toFixed(1) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const chartType = ref('weekly')
const charts = ref([])

async function loadCharts() {
  try {
    const res = await request.get('/charts', { params: { type: chartType.value, limit: 50 } })
    charts.value = res.data || []
  } catch (e) {}
}
onMounted(loadCharts)
</script>

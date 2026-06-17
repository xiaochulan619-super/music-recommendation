<template>
  <div class="page-wrap">
    <div class="page-hero">
      <h2 class="page-title">🏆 热门榜单</h2>
      <p class="page-desc">大家都在听</p>
    </div>
    <div class="chart-tabs">
      <span v-for="tab in tabs" :key="tab.value" class="chart-tab"
        :class="{ active: chartType === tab.value }" @click="switchTab(tab.value)">{{ tab.label }}</span>
    </div>
    <!-- Loading骨架 -->
    <div class="charts-list" v-if="loading">
      <div v-for="i in 8" :key="'sk'+i" class="chart-row sk-row">
        <div class="ch-rank"><span class="num sk-num"></span></div>
        <div class="ch-cover"><div class="sk-img"></div></div>
        <div class="ch-info">
          <div class="sk-line w-60"></div>
          <div class="sk-line w-40"></div>
        </div>
      </div>
    </div>
    <div class="charts-list" v-else>
      <div v-for="(item, i) in charts" :key="i" class="chart-row"
        @click="$router.push(`/song/${item.song?.id}`)">
        <div class="ch-rank"><span v-if="i<3" class="medal">{{ ['🥇','🥈','🥉'][i] }}</span>
          <span v-else class="num">{{ String(i+1).padStart(2,'0') }}</span></div>
        <div class="ch-cover"><img :src="item.song?.coverUrl || genCover(item.song)" alt="" /></div>
        <div class="ch-info">
          <div class="ch-title">{{ item.song?.title || '未知' }}</div>
          <div class="ch-artist">{{ item.song?.artistName || '未知' }}</div>
        </div>
        <div class="ch-stats">
          <span class="stat">🔥 {{ fmt(item.playCount) }}</span>
          <span class="stat">❤️ {{ fmt(item.favCount) }}</span>
        </div>
      </div>
    </div>
    <div class="empty" v-if="!loading && !charts.length && !errorMsg"><span class="empty-icon">📊</span><p>暂无数据</p></div>
    <div class="empty error" v-if="errorMsg"><span class="empty-icon">⚠️</span><p>{{ errorMsg }}</p></div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
const chartType = ref('weekly'); const charts = ref([]); const loading = ref(false); const errorMsg = ref('')
const tabs = [{label:'📅 日榜',value:'daily'},{label:'📈 周榜',value:'weekly'},{label:'🏅 总榜',value:'total'}]
function genCover(s) { return s?.coverUrl || `https://via.placeholder.com/60/333/fff?text=${encodeURIComponent((s?.title||'?').slice(0,4))}` }
function fmt(n) { if(!n) return '0'; if(n>=10000) return (n/10000).toFixed(1)+'w'; if(n>=1000) return (n/1000).toFixed(1)+'k'; return String(n) }
function switchTab(v) { chartType.value = v; loadCharts() }
async function loadCharts() {
  loading.value = true; errorMsg.value = ''
  try { charts.value = (await request.get('/charts',{params:{type:chartType.value,limit:50}})).data || [] } catch(e){ errorMsg.value = '榜单加载失败' } finally { loading.value = false }
}
onMounted(loadCharts)
</script>

<style scoped>
.page-wrap { max-width: 960px; margin: 0 auto; padding: 0 20px 40px; }
.page-hero { margin-bottom: 20px; }
.page-title { font-size: 26px; font-weight: 800; color: var(--text-primary); margin: 0; }
.page-desc { color: var(--text-muted); margin: 4px 0 0; font-size: 14px; }
.chart-tabs { display: flex; gap: 6px; margin-bottom: 20px; }
.chart-tab {
  padding: 7px 16px; border-radius: 10px; cursor: pointer;
  font-size: 13px; color: var(--text-secondary);
  background: var(--bg-card); border: 1px solid var(--border);
  transition: all 0.2s;
}
.chart-tab:hover { color: var(--text-primary); }
.chart-tab.active { color: var(--accent); border-color: var(--accent); background: rgba(102,126,234,0.08); }
.charts-list { display: flex; flex-direction: column; gap: 4px; }
.chart-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px; border-radius: 12px; cursor: pointer;
  background: var(--bg-card); border: 1px solid transparent;
  transition: all 0.25s;
}
.chart-row:hover { background: var(--bg-card-hover); border-color: var(--border); transform: translateX(4px); }
.ch-rank { width: 36px; text-align: center; flex-shrink: 0; }
.ch-rank .medal { font-size: 22px; }
.ch-rank .num { font-size: 16px; font-weight: 700; color: var(--text-muted); }
.ch-cover { width: 44px; height: 44px; border-radius: 8px; overflow: hidden; flex-shrink: 0; background: var(--bg-secondary); }
.ch-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s; }
.chart-row:hover .ch-cover img { transform: scale(1.12); }
.ch-info { flex: 1; min-width: 0; }
.ch-title { font-size: 14px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ch-artist { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.ch-stats { display: flex; gap: 14px; flex-shrink: 0; }
.stat { font-size: 12px; color: var(--text-muted); }
.empty { text-align: center; padding: 60px 0; }
.empty-icon { font-size: 48px; }
.empty p { color: var(--text-muted); margin: 8px 0; }
.empty.error p { color: #f56c6c; }

/* Skeleton */
.sk-row { pointer-events: none; }
.sk-num { display: block; width: 24px; height: 16px; border-radius: 4px; margin: 0 auto;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.sk-img { width: 44px; height: 44px; border-radius: 8px;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.sk-line { height: 12px; border-radius: 6px;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.w-60 { width: 60%; } .w-40 { width: 40%; margin-top: 6px; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>

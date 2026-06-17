<template>
  <div class="profile-page">
    <div class="page-hero">
      <h2 class="page-title">👤 个人中心</h2>
      <p class="page-desc">管理你的账号信息</p>
    </div>

    <div class="profile-card">
      <div class="profile-avatar">
        <span class="avatar-placeholder">{{ (userStore.userInfo?.nickname || userStore.userInfo?.username || '?')[0] }}</span>
      </div>
      <div class="profile-info">
        <div class="info-row">
          <span class="info-label">用户名</span>
          <span class="info-value">{{ userStore.userInfo?.username }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">昵称</span>
          <span class="info-value">{{ userStore.userInfo?.nickname || '未设置' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">角色</span>
          <el-tag size="small" effect="dark" type="primary">{{ userStore.userInfo?.role || '普通用户' }}</el-tag>
        </div>
      </div>
    </div>

    <div class="profile-actions">
      <router-link to="/favorites" class="action-card">
        <span class="action-icon">❤️</span>
        <span class="action-label">我的收藏</span>
      </router-link>
      <router-link to="/history" class="action-card">
        <span class="action-icon">🕐</span>
        <span class="action-label">播放历史</span>
      </router-link>
      <router-link to="/recommend" class="action-card">
        <span class="action-icon">⭐</span>
        <span class="action-label">智能推荐</span>
      </router-link>
    </div>

    <el-button type="danger" @click="handleLogout" class="logout-btn" plain>
      退出登录
    </el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.profile-page { max-width: 700px; margin: 0 auto; padding: 0 24px 40px; }
.page-hero { margin-bottom: 28px; }
.page-title { font-size: 28px; font-weight: 800; color: var(--text-primary); margin: 0; }
.page-desc { color: var(--text-muted); margin: 6px 0 0; font-size: 14px; }

.profile-card {
  display: flex; align-items: center; gap: 28px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 18px; padding: 32px;
  margin-bottom: 28px;
}
.profile-avatar {
  width: 80px; height: 80px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #e250c0);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.avatar-placeholder {
  font-size: 32px; font-weight: 800; color: #fff;
}
.profile-info { flex: 1; }
.info-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 0;
}
.info-row + .info-row { border-top: 1px solid var(--border); }
.info-label { font-size: 13px; color: var(--text-muted); width: 60px; flex-shrink: 0; }
.info-value { font-size: 14px; color: var(--text-primary); font-weight: 500; }

.profile-actions {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px;
  margin-bottom: 28px;
}
.action-card {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 20px; border-radius: 14px; text-decoration: none;
  background: var(--bg-card);
  border: 1px solid var(--border);
  transition: all 0.3s;
}
.action-card:hover {
  background: var(--bg-card-hover);
  border-color: rgba(102,126,234,0.3);
  transform: translateY(-4px);
}
.action-icon { font-size: 28px; }
.action-label { font-size: 13px; color: var(--text-secondary); font-weight: 500; }

.logout-btn { width: 100%; border-radius: 12px; }
</style>

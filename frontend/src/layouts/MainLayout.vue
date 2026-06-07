<template>
  <el-container class="layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">🎵 音乐推荐</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/"><el-icon><HomeFilled /></el-icon> 首页</el-menu-item>
        <el-menu-item index="/discover"><el-icon><Search /></el-icon> 发现音乐</el-menu-item>
        <el-menu-item index="/artists"><el-icon><Mic /></el-icon> 歌手</el-menu-item>
        <el-menu-item index="/charts"><el-icon><Trophy /></el-icon> 热门榜单</el-menu-item>
        <el-menu-item index="/recommend"><el-icon><Star /></el-icon> 智能推荐</el-menu-item>
        <el-divider style="border-color:#4a5568;margin:8px 0" />
        <el-menu-item index="/favorites"><el-icon><Heart /></el-icon> 我的收藏</el-menu-item>
        <el-menu-item index="/history"><el-icon><Clock /></el-icon> 播放历史</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-input v-model="searchKeyword" placeholder="搜索歌曲..." style="width:300px"
            @keyup.enter="handleSearch" clearable />
        </div>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown>
              <span class="user-name">👤 {{ userStore.userInfo?.nickname }}</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="$router.push('/login')">登录</el-button>
          </template>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const searchKeyword = ref('')

function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (kw) {
    router.push({ path: '/discover', query: { keyword: kw } })
  } else {
    router.push('/discover')
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { min-height: 100vh; }
.sidebar { background: #304156; overflow-y: auto; }
.logo { color: white; font-size: 18px; font-weight: bold; padding: 16px; text-align: center; }
.header { background: white; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e4e7ed; padding: 0 20px; height: 56px; }
.main-content { background: #f5f7fa; min-height: calc(100vh - 56px); }
.user-name { cursor: pointer; color: #409eff; }
</style>

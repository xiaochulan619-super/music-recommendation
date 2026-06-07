<template>
  <div>
    <h3>👤 个人中心</h3>
    <el-card>
      <p><strong>用户名：</strong>{{ userStore.userInfo?.username }}</p>
      <p><strong>昵称：</strong>{{ userStore.userInfo?.nickname }}</p>
      <el-button type="danger" @click="handleLogout">退出登录</el-button>
    </el-card>
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

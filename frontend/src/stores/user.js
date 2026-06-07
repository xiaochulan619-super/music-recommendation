import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  function setLogin(tokenVal, user) {
    token.value = tokenVal
    userInfo.value = user
    localStorage.setItem('token', tokenVal)
    localStorage.setItem('userInfo', JSON.stringify(user))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const isLoggedIn = computed(() => !!token.value)

  return { token, userInfo, setLogin, logout, isLoggedIn }
})

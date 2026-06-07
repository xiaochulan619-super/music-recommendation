import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: { template: '<div>Home</div>' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

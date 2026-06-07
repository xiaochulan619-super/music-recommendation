import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue')
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/HomeView.vue')
      },
      {
        path: 'discover',
        name: 'Discover',
        component: () => import('@/views/DiscoverView.vue')
      },
      {
        path: 'song/:id',
        name: 'SongDetail',
        component: () => import('@/views/SongDetailView.vue')
      },
      {
        path: 'artists',
        name: 'Artists',
        component: () => import('@/views/ArtistListView.vue')
      },
      {
        path: 'artist/:id',
        name: 'ArtistDetail',
        component: () => import('@/views/ArtistDetailView.vue')
      },
      {
        path: 'charts',
        name: 'Charts',
        component: () => import('@/views/ChartsView.vue')
      },
      {
        path: 'recommend',
        name: 'Recommend',
        component: () => import('@/views/RecommendView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('@/views/FavoritesView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('@/views/HistoryView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/')
  } else {
    next()
  }
})

export default router

import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/admin/login',
    component: () => import('../views/admin/AdminLogin.vue'),
  },
  {
    path: '/',
    beforeEnter: (_to: any, _from: any, next: any) => {
      if (!localStorage.getItem('auth_token')) {
        next('/login?redirect=' + encodeURIComponent(_to.fullPath))
      } else {
        next()
      }
    },
    children: [
      { path: '', component: () => import('../views/TaskList.vue') },
      { path: 'task/:id', component: () => import('../views/TaskDetail.vue') },
      { path: 'scan/:id', component: () => import('../views/ScanPage.vue') },
    ],
  },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    beforeEnter: (_to: any, _from: any, next: any) => {
      if (!localStorage.getItem('auth_token') || localStorage.getItem('auth_role') !== 'admin') {
        next('/admin/login')
      } else {
        next()
      }
    },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'tasks', component: () => import('../views/admin/TaskManage.vue') },
      { path: 'review', component: () => import('../views/admin/ReviewList.vue') },
      { path: 'archive', component: () => import('../views/admin/ArchiveList.vue') },
      { path: 'batch', component: () => import('../views/admin/BatchImport.vue') },
      { path: 'engineers', component: () => import('../views/admin/EngineerManage.vue') },
      { path: 'logs', component: () => import('../views/admin/OperationLogs.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'TaskList',
    component: () => import('../views/TaskList.vue'),
  },
  {
    path: '/task/:id',
    name: 'TaskDetail',
    component: () => import('../views/TaskDetail.vue'),
  },
  {
    path: '/scan/:id',
    name: 'ScanPage',
    component: () => import('../views/ScanPage.vue'),
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/admin/AdminLogin.vue'),
  },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    beforeEnter: (_to: any, _from: any, next: any) => {
      const token = localStorage.getItem('admin_token')
      if (!token) {
        next('/admin/login')
      } else {
        next()
      }
    },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'tasks', name: 'TaskManage', component: () => import('../views/admin/TaskManage.vue') },
      { path: 'review', name: 'ReviewList', component: () => import('../views/admin/ReviewList.vue') },
      { path: 'archive', name: 'ArchiveList', component: () => import('../views/admin/ArchiveList.vue') },
      { path: 'batch', name: 'BatchImport', component: () => import('../views/admin/BatchImport.vue') },
      { path: 'logs', name: 'OperationLogs', component: () => import('../views/admin/OperationLogs.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

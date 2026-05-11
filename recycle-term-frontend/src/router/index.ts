import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
  },
  {
    path: '/task',
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
    path: '/warehouse',
    name: 'WarehouseSearch',
    component: () => import('../views/warehouse/WarehouseSearch.vue'),
  },
  {
    path: '/warehouse/:id',
    name: 'WarehouseDetail',
    component: () => import('../views/warehouse/WarehouseDetail.vue'),
  },
  {
    path: '/work-orders',
    name: 'WorkOrderList',
    component: () => import('../views/work-order/WorkOrderList.vue'),
  },
  {
    path: '/work-orders/quick',
    name: 'WorkOrderQuickEntry',
    component: () => import('../views/work-order/WorkOrderQuickEntry.vue'),
  },
  {
    path: '/work-orders/:id',
    name: 'WorkOrderDetail',
    component: () => import('../views/work-order/WorkOrderDetail.vue'),
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
      { path: 'warehouse', name: 'AdminWarehouse', component: () => import('../views/admin/AdminWarehouse.vue') },
      { path: 'warehouse-requests', name: 'AdminWarehouseRequests', component: () => import('../views/admin/AdminWarehouseRequests.vue') },
      { path: 'device-types', name: 'AdminDeviceType', component: () => import('../views/admin/AdminDeviceType.vue') },
      { path: 'work-orders', name: 'AdminWorkOrders', component: () => import('../views/admin/AdminWorkOrders.vue') },
      { path: 'work-order-import', name: 'WorkOrderLegacyImport', component: () => import('../views/admin/WorkOrderLegacyImport.vue') },
      { path: 'work-order-options', name: 'WorkOrderOptions', component: () => import('../views/admin/WorkOrderOptions.vue') },
      { path: 'logs', name: 'OperationLogs', component: () => import('../views/admin/OperationLogs.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

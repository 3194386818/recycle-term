<template>
  <el-container class="app-container" direction="vertical">
    <!-- Header: full width, always on top -->
    <el-header class="app-header">
      <div class="header-left" @click="$router.push('/')" style="cursor:pointer">
        <el-icon :size="24"><Box /></el-icon>
        <h1>{{ showAdminSidebar ? '终端回收管理系统 · 管理后台' : '终端回收管理系统' }}</h1>
      </div>
      <div class="header-right">
        <template v-if="showAdminSidebar">
          <span class="admin-name">{{ adminUsername }}</span>
          <el-button text size="small" @click="$router.push('/')">前台首页</el-button>
          <el-button text size="small" type="danger" @click="logout">退出</el-button>
        </template>
        <template v-else>
          <span>2026年5月 · 南沙黄阁</span>
          <el-button text size="small" @click="$router.push('/admin/dashboard')">管理后台</el-button>
        </template>
      </div>
    </el-header>

    <!-- Below header: sidebar + content -->
    <el-container class="body-container">
      <el-aside v-if="showAdminSidebar" :width="isCollapse ? '64px' : '200px'" class="admin-aside">
        <el-menu :default-active="$route.path" router :collapse="isCollapse" class="sidebar-menu">
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataBoard /></el-icon>
            <template #title>数据看板</template>
          </el-menu-item>
          <el-menu-item index="/admin/tasks">
            <el-icon><List /></el-icon>
            <template #title>任务管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/review">
            <el-icon><Checked /></el-icon>
            <template #title>审核</template>
          </el-menu-item>
          <el-menu-item index="/admin/archive">
            <el-icon><FolderChecked /></el-icon>
            <template #title>归档</template>
          </el-menu-item>
          <el-menu-item index="/admin/batch">
            <el-icon><Upload /></el-icon>
            <template #title>批量导入</template>
          </el-menu-item>
          <el-menu-item index="/admin/logs">
            <el-icon><Document /></el-icon>
            <template #title>操作日志</template>
          </el-menu-item>
        </el-menu>
        <div class="collapse-btn" @click="isCollapse = !isCollapse">
          <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
        </div>
      </el-aside>
      <el-main :class="showAdminSidebar ? 'admin-main' : 'app-main'">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const adminUsername = localStorage.getItem('admin_username') || 'admin'

const showAdminSidebar = computed(() => {
  return route.path.startsWith('/admin') && route.path !== '/admin/login'
})

function logout() {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_username')
  router.push('/admin/login')
}
</script>

<style scoped>
.app-container {
  height: 100vh;
  overflow: hidden;
}

/* Header: full width on top */
.app-header {
  background: #1677ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  box-shadow: 0 2px 8px rgba(0,0,0,.15);
  flex-shrink: 0;
  z-index: 10;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-left h1 {
  font-size: 18px;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  opacity: .9;
}
.header-right .el-button {
  color: rgba(255,255,255,.85);
}
.header-right .el-button:hover {
  color: #fff;
}
.admin-name { font-weight: 500; }

/* Body: sidebar + content */
.body-container {
  flex: 1;
  overflow: hidden;
}

/* Sidebar */
.admin-aside {
  background: #001529;
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-menu {
  flex: 1;
  border: none;
  overflow-y: auto;
}
.sidebar-menu:not(.el-menu--collapse) {
  width: 200px;
}
:deep(.sidebar-menu) {
  background-color: #001529;
}
:deep(.sidebar-menu .el-menu-item) {
  color: rgba(255,255,255,.65);
  background-color: transparent;
}
:deep(.sidebar-menu .el-menu-item:hover) {
  color: #fff;
  background-color: rgba(255,255,255,.08);
}
:deep(.sidebar-menu .el-menu-item.is-active) {
  color: #fff;
  background-color: #1677ff;
}
.collapse-btn {
  padding: 12px;
  text-align: center;
  color: rgba(255,255,255,.45);
  cursor: pointer;
  border-top: 1px solid rgba(255,255,255,.1);
  flex-shrink: 0;
}
.collapse-btn:hover { color: #fff; }

/* Main content */
.app-main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
  overflow-y: auto;
}
.admin-main {
  padding: 20px;
  overflow-y: auto;
  background: #f5f7fa;
}

@media (max-width: 768px) {
  .app-header {
    padding: 0 12px;
    height: 52px;
  }
  .header-left h1 {
    font-size: 14px;
  }
  .header-right {
    font-size: 11px;
    gap: 8px;
  }
  .app-main, .admin-main {
    padding: 12px;
  }
}
</style>

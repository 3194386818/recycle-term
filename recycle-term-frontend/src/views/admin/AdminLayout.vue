<template>
  <el-container class="admin-container">
    <el-header class="admin-header">
      <div class="header-left" style="cursor:pointer" @click="$router.push('/admin/dashboard')">
        <el-icon :size="24"><Box /></el-icon>
        <h1>终端回收管理系统 · 管理后台</h1>
      </div>
      <div class="header-right">
        <span class="admin-name">{{ adminUsername }}</span>
        <el-button text size="small" @click="$router.push('/')">前台首页</el-button>
        <el-button text size="small" type="danger" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-container class="admin-body">
      <el-aside :width="isCollapse ? '64px' : '200px'" class="admin-aside">
        <el-menu :default-active="$route.path" router :collapse="isCollapse" class="sidebar-menu">
          <el-menu-item index="/admin/dashboard"><el-icon><DataBoard /></el-icon><template #title>数据看板</template></el-menu-item>
          <el-menu-item index="/admin/tasks"><el-icon><List /></el-icon><template #title>任务管理</template></el-menu-item>
          <el-menu-item index="/admin/review"><el-icon><Checked /></el-icon><template #title>审核</template></el-menu-item>
          <el-menu-item index="/admin/archive"><el-icon><FolderChecked /></el-icon><template #title>归档</template></el-menu-item>
          <el-menu-item index="/admin/batch"><el-icon><Upload /></el-icon><template #title>批量导入</template></el-menu-item>
          <el-menu-item index="/admin/logs"><el-icon><Document /></el-icon><template #title>操作日志</template></el-menu-item>
          <el-menu-item index="/admin/engineers"><el-icon><User /></el-icon><template #title>账号管理</template></el-menu-item>
        </el-menu>
        <div class="collapse-btn" @click="isCollapse = !isCollapse">
          <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
        </div>
      </el-aside>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isCollapse = ref(false)
const adminUsername = localStorage.getItem('admin_username') || 'admin'

function logout() {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_username')
  localStorage.removeItem('auth_token')
  localStorage.removeItem('auth_role')
  localStorage.removeItem('auth_phone')
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-container { height: 100vh; overflow: hidden; }
.admin-header {
  background: #001529; color: #fff;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; height: 64px; flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.header-left h1 { font-size: 18px; font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 12px; font-size: 13px; opacity: .85; }
.header-right .el-button { color: rgba(255,255,255,.85); }
.admin-name { font-weight: 500; }
.admin-body { flex: 1; overflow: hidden; }
.admin-aside {
  background: #001529; transition: width 0.3s;
  display: flex; flex-direction: column; overflow: hidden;
}
.sidebar-menu { flex: 1; border: none; overflow-y: auto; }
.sidebar-menu:not(.el-menu--collapse) { width: 200px; }
:deep(.sidebar-menu) { background-color: #001529; }
:deep(.sidebar-menu .el-menu-item) { color: rgba(255,255,255,.65); background-color: transparent; }
:deep(.sidebar-menu .el-menu-item:hover) { color: #fff; background-color: rgba(255,255,255,.08); }
:deep(.sidebar-menu .el-menu-item.is-active) { color: #fff; background-color: #1677ff; }
.collapse-btn {
  padding: 12px; text-align: center; color: rgba(255,255,255,.45);
  cursor: pointer; border-top: 1px solid rgba(255,255,255,.1); flex-shrink: 0;
}
.collapse-btn:hover { color: #fff; }
.admin-main { padding: 20px; overflow-y: auto; background: #f5f7fa; }
@media (max-width: 768px) {
  .admin-header { padding: 0 12px; height: 52px; }
  .header-left h1 { font-size: 14px; }
  .admin-main { padding: 12px; }
}
</style>

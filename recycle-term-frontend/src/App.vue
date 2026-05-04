<template>
  <!-- Admin pages: just render AdminLayout (it has its own header/sidebar) -->
  <router-view v-if="isAdminRoute" />

  <!-- Non-admin pages: render our header + content -->
  <el-container v-else class="app-container" direction="vertical">
    <el-header class="app-header">
      <div class="header-left" @click="$router.push('/')" style="cursor:pointer">
        <el-icon :size="24"><Box /></el-icon>
        <h1>终端回收管理系统</h1>
      </div>
      <div class="header-right">
        <span>2026年5月 · 南沙黄阁</span>
        <el-button text size="small" @click="$router.push('/admin/dashboard')">管理后台</el-button>
      </div>
    </el-header>
    <el-container class="body-container">
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
</script>

<style scoped>
.app-container { height: 100vh; overflow: hidden; }
.app-header {
  background: #1677ff; color: #fff;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; height: 64px;
  box-shadow: 0 2px 8px rgba(0,0,0,.15); flex-shrink: 0; z-index: 10;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.header-left h1 { font-size: 18px; font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 12px; font-size: 13px; opacity: .9; }
.header-right .el-button { color: rgba(255,255,255,.85); }
.header-right .el-button:hover { color: #fff; }
.body-container { flex: 1; overflow: hidden; }
.app-main { max-width: 1200px; margin: 0 auto; width: 100%; padding: 20px; overflow-y: auto; overflow-x: hidden; }
@media (max-width: 768px) {
  .app-header { padding: 0 12px; height: 52px; }
  .header-left h1 { font-size: 14px; }
  .header-right { font-size: 11px; gap: 8px; }
  .app-main { padding: 12px; }
}
</style>

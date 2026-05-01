<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="header-left">
        <el-icon :size="24"><Box /></el-icon>
        <h1>终端回收管理系统</h1>
      </div>
      <div class="header-right">2026年5月 · 南沙黄阁</div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: #f5f7fa;
}
.app-header {
  background: #1677ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  box-shadow: 0 2px 8px rgba(0,0,0,.15);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-left h1 {
  font-size: 20px;
  font-weight: 600;
}
.header-right {
  font-size: 13px;
  opacity: .85;
}
.app-main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
}

@media (max-width: 768px) {
  .app-header {
    padding: 0 12px;
    height: 48px;
  }
  .header-left h1 {
    font-size: 16px;
  }
  .header-right {
    font-size: 11px;
  }
  .app-main {
    padding: 12px;
  }
}
</style>

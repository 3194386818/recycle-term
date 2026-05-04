<template>
  <div class="home-page">
    <div class="home-header">
      <h1>终端回收管理系统</h1>
      <p>欢迎使用，请选择功能模块</p>
    </div>
    <div class="module-grid">
      <div class="module-card" @click="$router.push('/task')">
        <el-icon :size="40" color="#1677ff"><List /></el-icon>
        <h3>回收任务</h3>
        <p>查看、处理回收任务，扫码回收终端</p>
      </div>
      <div class="module-card" @click="$router.push('/warehouse')">
        <el-icon :size="40" color="#52c41a"><Box /></el-icon>
        <h3>仓库管理</h3>
        <p>查询仓库设备信息，入库出库管理</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 40px 20px;
}
.home-header {
  text-align: center;
  margin-bottom: 40px;
}
.home-header h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}
.home-header p {
  color: #666;
  font-size: 14px;
}
.module-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.module-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.module-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
}
.module-card h3 {
  margin: 12px 0 6px;
  font-size: 16px;
}
.module-card p {
  font-size: 12px;
  color: #999;
}
@media (max-width: 768px) {
  .home-page { padding: 20px 16px; }
  .module-grid { grid-template-columns: 1fr; }
}
</style>

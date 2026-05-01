<template>
  <div class="dashboard">
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6" v-for="item in statCards" :key="item.label">
        <el-card shadow="hover" class="stat-card" :style="{ borderTop: `3px solid ${item.color}` }">
          <div class="stat-num" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header><span>每日回收趋势（近30天）</span></template>
          <div ref="lineRef" style="height:350px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header><span>任务状态分布</span></template>
          <div ref="pieRef" style="height:350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header><span>区域任务统计</span></template>
          <div ref="barRef" style="height:350px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getDailyStats, getStatusStats, getAreaStats, getAdminTasks } from '../../api/admin'

const lineRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()
const barRef = ref<HTMLElement>()

let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

const statCards = ref([
  { label: '总任务', value: 0, color: '#1677ff' },
  { label: '已完成', value: 0, color: '#52c41a' },
  { label: '待回收', value: 0, color: '#faad14' },
  { label: '已扫描', value: 0, color: '#722ed1' },
])

async function fetchStats() {
  const { data: res } = await getAdminTasks({ page: 0, size: 1 })
  const total = res.data.totalElements
  const { data: statusRes } = await getStatusStats()
  statCards.value[0].value = total
  statCards.value[1].value = statusRes.data['已完成'] || 0
  statCards.value[2].value = statusRes.data['待回收'] || 0
}

async function renderLine() {
  if (!lineRef.value) return
  lineChart = echarts.init(lineRef.value)
  lineChart.showLoading()
  const { data: res } = await getDailyStats(30)
  lineChart.hideLoading()
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['完成任务', '扫描串码'] },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: res.data.map(d => d.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '完成任务', type: 'line', smooth: true, data: res.data.map(d => d.completed), areaStyle: { opacity: 0.15 }, itemStyle: { color: '#52c41a' } },
      { name: '扫描串码', type: 'line', smooth: true, data: res.data.map(d => d.scanned), areaStyle: { opacity: 0.15 }, itemStyle: { color: '#1677ff' } },
    ],
  })
}

async function renderPie() {
  if (!pieRef.value) return
  pieChart = echarts.init(pieRef.value)
  pieChart.showLoading()
  const { data: res } = await getStatusStats()
  pieChart.hideLoading()
  const total = Object.values(res.data).reduce((a, b) => a + b, 0)
  statCards.value[3].value = total
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['50%', '45%'],
      label: { formatter: '{b}: {c} ({d}%)' },
      data: Object.entries(res.data).map(([name, value]) => ({ name, value })),
      itemStyle: { color: (params: any) => (({ '已完成': '#52c41a', '待回收': '#faad14', '需上门': '#1677ff' }) as Record<string, string>)[params.name] || '#999' },
    }],
  })
}

async function renderBar() {
  if (!barRef.value) return
  barChart = echarts.init(barRef.value)
  barChart.showLoading()
  const { data: res } = await getAreaStats()
  barChart.hideLoading()
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 80, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: res.data.map(d => d.area || '未分配').reverse() },
    series: [{
      type: 'bar', barWidth: 24,
      data: res.data.map(d => d.count).reverse(),
      itemStyle: { color: '#1677ff', borderRadius: [0, 4, 4, 0] },
    }],
  })
}

function handleResize() {
  lineChart?.resize()
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  fetchStats()
  renderLine()
  renderPie()
  renderBar()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  lineChart?.dispose()
  pieChart?.dispose()
  barChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.stat-cards .el-col { margin-bottom: 8px; }
.stat-card { border-radius: 8px; }
.stat-num { font-size: 28px; font-weight: 700; }
.stat-label { font-size: 13px; color: #888; margin-top: 4px; }
</style>

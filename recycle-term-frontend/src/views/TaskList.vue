<template>
  <div class="task-list">
    <!-- Stats cards: clickable filters -->
    <el-row :gutter="12" class="stats-row">
      <el-col :xs="12" :sm="6" v-for="item in statCards" :key="item.key">
        <el-card shadow="hover" class="stat-card" :class="{ active: activeFilter === item.key }" :style="{ borderTop: `3px solid ${item.color}` }" @click="setFilter(item.key)">
          <div class="stat-num" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Toolbar: search only -->
    <el-card shadow="never" class="toolbar-card">
      <el-input v-model="keyword" placeholder="搜索用户号码、产品号、姓名、地址..." clearable @input="debouncedFetch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </el-card>

    <!-- Table -->
    <el-card shadow="never">
      <el-table :data="tasks" v-loading="loading" stripe @row-click="goDetail" style="cursor:pointer">
        <el-table-column label="产品号" prop="productId" min-width="120" show-overflow-tooltip />
        <el-table-column label="用户号码" prop="phoneNumber" min-width="120" show-overflow-tooltip class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column label="用户名称" prop="userName" min-width="90" />
        <el-table-column label="用户地址" prop="userAddress" show-overflow-tooltip min-width="160" class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column label="接入间" prop="accessRoom" width="100" show-overflow-tooltip class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column label="应回收" width="70" align="center">
          <template #default="{ row }">{{ row.expectedCount || '-' }}</template>
        </el-table-column>
        <el-table-column label="已回收" width="70" align="center" class-name="hide-mobile" header-class-name="hide-mobile">
          <template #default="{ row }">{{ row.fttrCount || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.status]?.type || 'info'" size="small">{{ statusTypeMap[row.status]?.label || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isMobile ? 140 : 260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="copyProduct(row.productId)">复制产品号</el-button>
            <el-button size="small" type="primary" @click.stop="$router.push(`/scan/${row.id}`)">
              <el-icon><Camera /></el-icon><span class="btn-text">扫码</span>
            </el-button>
            <el-dropdown trigger="click" @command="(cmd: number) => handleStatusChange(row, cmd)" @click.stop>
              <el-button size="small" type="success">状态</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="0">待回收</el-dropdown-item>
                  <el-dropdown-item :command="1">已上门</el-dropdown-item>
                  <el-dropdown-item :command="2">已完成</el-dropdown-item>
                  <el-dropdown-item :command="3">已失败</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          :small="isMobile"
          layout="total, prev, pager, next"
          @current-change="fetchTasks"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTasks, getStats, updateTaskStatus } from '../api'
import type { RecycleTask, Stats } from '../types'

const router = useRouter()

const isMobile = ref(window.innerWidth <= 768)
window.addEventListener('resize', () => { isMobile.value = window.innerWidth <= 768 })

const tasks = ref<RecycleTask[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(50)
const total = ref(0)
const keyword = ref('')
const activeFilter = ref<number | '全部'>('全部')

const statusTypeMap: Record<number, { label: string; type: string }> = {
  0: { label: '待回收', type: 'warning' },
  1: { label: '已上门', type: 'primary' },
  2: { label: '已完成', type: 'success' },
  3: { label: '已失败', type: 'danger' },
}

const stats = ref<Stats>({ total: 0, completed: 0, pending: 0, needVisit: 0, failed: 0, totalScanned: 0 })

const statCards = ref([
  { key: '全部' as const, label: '总任务', value: 0, color: '#1677ff' },
  { key: 0, label: '待回收', value: 0, color: '#faad14' },
  { key: 2, label: '已完成', value: 0, color: '#52c41a' },
  { key: 3, label: '已失败', value: 0, color: '#ff4d4f' },
])

let debounceTimer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { page.value = 0; fetchTasks() }, 300)
}

function setFilter(key: number | '全部') {
  activeFilter.value = key
  page.value = 0
  keyword.value = ''
  fetchTasks()
}

async function fetchTasks() {
  loading.value = true
  try {
    const params: any = {
      keyword: keyword.value,
      page: page.value,
      size: size.value,
    }
    if (activeFilter.value !== '全部') {
      params.status = activeFilter.value
    }
    const { data: res } = await getTasks(params)
    tasks.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  const { data: res } = await getStats()
  stats.value = res.data
  statCards.value[0].value = res.data.total
  statCards.value[1].value = res.data.pending
  statCards.value[2].value = res.data.completed
  statCards.value[3].value = res.data.failed
}

async function handleStatusChange(row: RecycleTask, status: number) {
  try {
    await updateTaskStatus(row.id, status)
    ElMessage.success(`已更新为 ${statusTypeMap[status]?.label}`)
    fetchTasks()
    fetchStats()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '状态更新失败')
  }
}

function copyProduct(text: string) {
  if (!text) {
    ElMessage.warning('产品号为空')
    return
  }
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制: ' + text)
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function goDetail(row: RecycleTask) {
  router.push(`/task/${row.id}`)
}

onMounted(() => {
  fetchTasks()
  fetchStats()
})
</script>

<style scoped>
.stats-row { margin-bottom: 12px; }
.stats-row .el-col { margin-bottom: 8px; }
.stat-card { text-align: center; border-radius: 12px; cursor: pointer; transition: all .2s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,.12); }
.stat-card.active { border: 2px solid #1677ff; }
.stat-num { font-size: 32px; font-weight: 700; }
.stat-label { font-size: 13px; color: #666; margin-top: 4px; }
.toolbar-card { margin-bottom: 12px; }
.pagination { margin-top: 12px; display: flex; justify-content: flex-end; }

@media (max-width: 768px) {
  .stat-num { font-size: 24px; }
  .stat-label { font-size: 12px; }
  .btn-text { display: none; }
  :deep(.hide-mobile) { display: none !important; }
}
</style>

<template>
  <div class="task-list">
    <!-- Stats -->
    <el-row :gutter="12" class="stats-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-label">总任务</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card done">
          <div class="stat-num">{{ stats.completed }}</div>
          <div class="stat-label">已完成</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card pending">
          <div class="stat-num">{{ stats.pending }}</div>
          <div class="stat-label">待回收</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card visit">
          <div class="stat-num">{{ stats.needVisit }}</div>
          <div class="stat-label">需上门</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Toolbar -->
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="8">
          <el-input v-model="keyword" placeholder="搜索号码、姓名、地址..." clearable @input="debouncedFetch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-col>
        <el-col :xs="12" :sm="4" class="filter-col">
          <el-select v-model="filterCompleted" placeholder="完成状态" clearable @change="fetchTasks">
            <el-option label="待回收" :value="false" />
            <el-option label="已完成" :value="true" />
          </el-select>
        </el-col>
        <el-col :xs="12" :sm="4" class="filter-col">
          <el-select v-model="filterVisit" placeholder="上门状态" clearable @change="fetchTasks">
            <el-option label="需上门" :value="true" />
            <el-option label="无需上门" :value="false" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="8" class="toolbar-btns">
          <el-button type="primary" @click="$router.push('/scan/0')">
            <el-icon><Camera /></el-icon><span class="btn-text">扫码查询</span>
          </el-button>
          <el-upload
            :show-file-list="false"
            :before-upload="handleImport"
            accept=".xlsx,.xls"
          >
            <el-button type="success">
              <el-icon><Upload /></el-icon><span class="btn-text">导入Excel</span>
            </el-button>
          </el-upload>
        </el-col>
      </el-row>
    </el-card>

    <!-- Table -->
    <el-card shadow="never">
      <el-table :data="tasks" v-loading="loading" stripe @row-click="goDetail" style="cursor:pointer">
        <el-table-column label="用户名称" prop="userName" min-width="90" />
        <el-table-column label="用户号码" prop="phoneNumber" min-width="120" class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column label="地址" prop="userAddress" show-overflow-tooltip min-width="160" class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column label="应回收" width="70" align="center">
          <template #default="{ row }">
            {{ row.expectedCount || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="FTTR" width="60" align="center" class-name="hide-mobile" header-class-name="hide-mobile">
          <template #default="{ row }">
            {{ row.fttrCount || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.completed ? 'success' : 'warning'" size="small">
              {{ row.completed ? '已完成' : '待回收' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上门" width="60" align="center" class-name="hide-mobile" header-class-name="hide-mobile">
          <template #default="{ row }">
            <el-tag v-if="row.needVisit" type="primary" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isMobile ? 120 : 200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="$router.push(`/scan/${row.id}`)">
              <el-icon><Camera /></el-icon><span class="btn-text">扫码</span>
            </el-button>
            <el-button size="small" :type="row.completed ? 'info' : 'success'" @click.stop="toggleComplete(row)">
              {{ row.completed ? '撤销' : '完成' }}
            </el-button>
            <el-button size="small" :type="row.needVisit ? 'info' : 'warning'" class="btn-text" @click.stop="toggleVisit(row)">
              {{ row.needVisit ? '取消上门' : '上门' }}
            </el-button>
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
import { getTasks, getStats, updateTask, importExcel } from '../api'
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
const filterCompleted = ref<boolean | null>(null)
const filterVisit = ref<boolean | null>(null)
const stats = ref<Stats>({ total: 0, completed: 0, pending: 0, needVisit: 0, totalScanned: 0 })

let debounceTimer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { page.value = 0; fetchTasks() }, 300)
}

async function fetchTasks() {
  loading.value = true
  try {
    const { data: res } = await getTasks({
      keyword: keyword.value,
      completed: filterCompleted.value,
      needVisit: filterVisit.value,
      page: page.value,
      size: size.value,
    })
    tasks.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  const { data: res } = await getStats()
  stats.value = res.data
}

async function toggleComplete(row: RecycleTask) {
  await updateTask(row.id, { completed: !row.completed })
  ElMessage.success(row.completed ? '已撤销' : '已完成')
  fetchTasks()
  fetchStats()
}

async function toggleVisit(row: RecycleTask) {
  await updateTask(row.id, { needVisit: !row.needVisit })
  ElMessage.success(row.needVisit ? '已取消上门' : '已标记上门')
  fetchTasks()
  fetchStats()
}

async function handleImport(file: File) {
  const { data: res } = await importExcel(file)
  ElMessage.success(res.message)
  fetchTasks()
  fetchStats()
  return false // prevent default upload
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
.stat-card { text-align: center; border-radius: 12px; }
.stat-card.done :deep(.el-card__body) { color: #52c41a; }
.stat-card.pending :deep(.el-card__body) { color: #faad14; }
.stat-card.visit :deep(.el-card__body) { color: #1677ff; }
.stat-num { font-size: 32px; font-weight: 700; }
.stat-label { font-size: 13px; color: #666; margin-top: 4px; }
.toolbar-card { margin-bottom: 12px; }
.toolbar-btns { display: flex; gap: 8px; justify-content: flex-end; }
.filter-col { margin-top: 8px; }
.pagination { margin-top: 12px; display: flex; justify-content: flex-end; }

@media (max-width: 768px) {
  .stat-num { font-size: 24px; }
  .stat-label { font-size: 12px; }
  .toolbar-btns {
    margin-top: 8px;
    justify-content: flex-start;
  }
  .btn-text { display: none; }
  :deep(.hide-mobile) {
    display: none !important;
  }
}
</style>

<template>
  <div class="archive-list">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <el-input v-model="keyword" placeholder="搜索号码、姓名、地址..." clearable style="width:300px" @input="debouncedFetch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-tag type="success">已归档：{{ total }}</el-tag>
        </div>
      </template>
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="产品号" prop="productId" width="120" />
        <el-table-column label="用户" prop="userName" width="100" />
        <el-table-column label="用户号码" prop="phoneNumber" width="120" />
        <el-table-column label="地址" prop="userAddress" show-overflow-tooltip min-width="160" />
        <el-table-column label="区域" prop="area" width="80" />
        <el-table-column label="原状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.completed ? 'success' : 'danger'" size="small">
              {{ row.completed ? '已完成' : '已失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回收终端" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.terminals || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchTasks" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="归档详情" width="700px" destroy-on-close>
      <div v-if="detailTask">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="产品号">{{ detailTask.productId }}</el-descriptions-item>
          <el-descriptions-item label="用户号码">{{ detailTask.phoneNumber }}</el-descriptions-item>
          <el-descriptions-item label="用户名称">{{ detailTask.userName }}</el-descriptions-item>
          <el-descriptions-item label="区域">{{ detailTask.area }}</el-descriptions-item>
          <el-descriptions-item label="用户地址" :span="2">{{ detailTask.userAddress }}</el-descriptions-item>
          <el-descriptions-item label="工程师">{{ detailTask.engineerName }} ({{ detailTask.engineerPhone }})</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatDateTime(detailTask.completedAt) }}</el-descriptions-item>
          <el-descriptions-item label="失败原因" :span="2" v-if="detailTask.failReason">
            <el-tag type="danger">{{ detailTask.failReason }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2" v-if="detailTask.reviewRemark">
            {{ detailTask.reviewRemark }}
          </el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:16px 0 8px">已回收串码 ({{ detailRecords.length }})</h4>
        <el-table :data="detailRecords" stripe size="small" max-height="200">
          <el-table-column label="序号" type="index" width="50" />
          <el-table-column label="终端串码" prop="serialNumber" />
          <el-table-column label="扫描时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.scannedAt) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="detailRecords.length === 0" description="无扫描记录" :image-size="60" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminTasks } from '../../api/admin'
import { getRecordsByTaskId } from '../../api'
import type { RecycleTask, TerminalRecord } from '../../types'
import { formatDateTime } from '../../utils/datetime'

const tasks = ref<RecycleTask[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

const detailVisible = ref(false)
const detailTask = ref<RecycleTask | null>(null)
const detailRecords = ref<TerminalRecord[]>([])

let debounceTimer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { page.value = 0; fetchTasks() }, 300)
}

async function fetchTasks() {
  loading.value = true
  try {
    const { data: res } = await getAdminTasks({ keyword: keyword.value, status: 6, page: page.value, size: size.value })
    tasks.value = res.data.content
    total.value = res.data.totalElements
    if (tasks.value.length === 0 && page.value > 0) {
      page.value--
      fetchTasks()
      return
    }
  } finally {
    loading.value = false
  }
}

async function showDetail(row: RecycleTask) {
  detailTask.value = row
  detailVisible.value = true
  const { data: res } = await getRecordsByTaskId(row.id)
  detailRecords.value = res.data
}

onMounted(fetchTasks)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

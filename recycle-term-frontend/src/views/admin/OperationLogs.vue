<template>
  <div class="operation-logs">
    <el-card shadow="never">
      <template #header><span>操作日志</span></template>
      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="操作人" prop="adminUsername" width="100" />
        <el-table-column label="操作" prop="action" width="100">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情" prop="detail" show-overflow-tooltip />
        <el-table-column label="IP" prop="ip" width="130" />
        <el-table-column label="时间" prop="createdAt" width="180" />
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchLogs" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLogs } from '../../api/admin'
import type { OperationLog } from '../../types'

const logs = ref<OperationLog[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const total = ref(0)

async function fetchLogs() {
  loading.value = true
  try {
    const { data: res } = await getLogs({ page: page.value, size: size.value })
    logs.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

function getActionType(action: string) {
  if (action.includes('添加')) return 'success'
  if (action.includes('删除')) return 'danger'
  if (action.includes('修改')) return 'warning'
  if (action.includes('批量')) return 'primary'
  return 'info'
}

onMounted(fetchLogs)
</script>

<style scoped>
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

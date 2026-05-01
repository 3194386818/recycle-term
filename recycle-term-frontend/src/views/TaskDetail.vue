<template>
  <div class="task-detail" v-if="task">
    <el-page-header @back="$router.push('/')">
      <template #content>
        <span class="page-title">{{ task.userName }} 的回收任务</span>
      </template>
      <template #extra>
        <div class="header-actions">
          <el-button type="primary" @click="$router.push(`/scan/${task.id}`)">
            <el-icon><Camera /></el-icon><span class="btn-text">扫码回收</span>
          </el-button>
          <el-button :type="task.completed ? 'info' : 'success'" @click="toggleComplete">
            {{ task.completed ? '撤销完成' : '标记完成' }}
          </el-button>
          <el-button :type="task.needVisit ? 'info' : 'warning'" @click="toggleVisit">
            {{ task.needVisit ? '取消上门' : '标记上门' }}
          </el-button>
        </div>
      </template>
    </el-page-header>

    <!-- Info -->
    <el-card shadow="never" class="info-card">
      <el-descriptions :column="descColumn" border>
        <el-descriptions-item label="用户号码">{{ task.phoneNumber }}</el-descriptions-item>
        <el-descriptions-item label="用户名称">{{ task.userName }}</el-descriptions-item>
        <el-descriptions-item label="用户地址" :span="descColumn">{{ task.userAddress }}</el-descriptions-item>
        <el-descriptions-item label="区域">{{ task.area }}</el-descriptions-item>
        <el-descriptions-item label="工程师">{{ task.engineerName }} ({{ task.engineerPhone }})</el-descriptions-item>
        <el-descriptions-item label="接入间">{{ task.accessRoom || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ task.category || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发展部门">{{ task.devDept || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发展员工">{{ task.devPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="应回收数量">{{ task.expectedCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="FTTR主光猫">{{ task.fttrCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="task.completed ? 'success' : 'warning'">{{ task.completed ? '已完成' : '待回收' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="是否上门">
          <el-tag :type="task.needVisit ? 'primary' : 'info'">{{ task.needVisit ? '是' : '否' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ task.completedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="应回收终端" :span="descColumn">{{ task.terminals || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="descColumn">{{ task.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- Scanned Records -->
    <el-card shadow="never" class="records-card">
      <template #header>
        <div class="card-header">
          <span>已扫描串码 ({{ records.length }})</span>
          <el-button type="primary" size="small" @click="$router.push(`/scan/${task.id}`)">继续扫码</el-button>
        </div>
      </template>
      <el-table :data="records" stripe v-loading="recordsLoading">
        <el-table-column label="序号" type="index" width="50" />
        <el-table-column label="终端串码" prop="serialNumber" />
        <el-table-column label="扫描时间" prop="scannedAt" width="160" class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="removeRecord(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!recordsLoading && records.length === 0" description="暂无扫描记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTaskById, updateTask, getRecordsByTaskId, deleteRecord } from '../api'
import type { RecycleTask, TerminalRecord } from '../types'

const route = useRoute()
const taskId = Number(route.params.id)

const task = ref<RecycleTask | null>(null)
const records = ref<TerminalRecord[]>([])
const recordsLoading = ref(false)

const isMobile = ref(window.innerWidth <= 768)
window.addEventListener('resize', () => { isMobile.value = window.innerWidth <= 768 })
const descColumn = computed(() => isMobile.value ? 1 : 2)

async function fetchTask() {
  const { data: res } = await getTaskById(taskId)
  task.value = res.data
}

async function fetchRecords() {
  recordsLoading.value = true
  try {
    const { data: res } = await getRecordsByTaskId(taskId)
    records.value = res.data
  } finally {
    recordsLoading.value = false
  }
}

async function toggleComplete() {
  if (!task.value) return
  await updateTask(taskId, { completed: !task.value.completed })
  ElMessage.success(task.value.completed ? '已撤销' : '已完成')
  fetchTask()
}

async function toggleVisit() {
  if (!task.value) return
  await updateTask(taskId, { needVisit: !task.value.needVisit })
  ElMessage.success(task.value.needVisit ? '已取消上门' : '已标记上门')
  fetchTask()
}

async function removeRecord(id: number) {
  await deleteRecord(id)
  ElMessage.success('已删除')
  fetchRecords()
}

onMounted(() => {
  fetchTask()
  fetchRecords()
})
</script>

<style scoped>
.task-detail { max-width: 1000px; margin: 0 auto; }
.page-title { font-size: 18px; font-weight: 600; }
.info-card { margin-top: 20px; }
.records-card { margin-top: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 8px; flex-wrap: wrap; }

@media (max-width: 768px) {
  .page-title { font-size: 15px; }
  .header-actions .btn-text { display: none; }
  :deep(.hide-mobile) { display: none !important; }
}
</style>

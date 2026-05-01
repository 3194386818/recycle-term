<template>
  <div class="scan-page">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="page-title">扫码回收终端</span>
      </template>
    </el-page-header>

    <!-- Task selector when no task ID -->
    <el-card v-if="taskId === 0" shadow="never" class="task-select-card">
      <h3>选择要回收的任务</h3>
      <el-input v-model="searchKeyword" placeholder="搜索用户号码或姓名..." clearable @input="debouncedSearch" style="margin: 12px 0">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-table :data="taskList" stripe @row-click="selectTask" style="cursor:pointer" max-height="400">
        <el-table-column label="用户" prop="userName" width="80" />
        <el-table-column label="号码" prop="phoneNumber" width="120" />
        <el-table-column label="地址" prop="userAddress" show-overflow-tooltip class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column label="应回收" prop="expectedCount" width="70" align="center" />
      </el-table>
    </el-card>

    <!-- Scanner -->
    <template v-if="taskId > 0">
      <el-card shadow="never" class="info-card">
        <div class="task-info">
          <span><strong>{{ task?.userName }}</strong> ({{ task?.phoneNumber }})</span>
          <el-tag :type="task?.completed ? 'success' : 'warning'" size="small">
            {{ task?.completed ? '已完成' : '待回收' }}
          </el-tag>
        </div>
        <div class="terminals-hint">{{ task?.terminals }}</div>
        <div class="progress-text">
          已扫描: {{ scannedSNs.length }} / {{ task?.expectedCount || '?' }}
        </div>
      </el-card>

      <!-- Camera -->
      <el-card shadow="never" class="scanner-card">
        <div id="reader"></div>
        <div class="manual-row">
          <el-input v-model="manualSn" placeholder="手动输入终端串码" @keydown.enter="addManual" clearable>
            <template #append>
              <el-button @click="addManual">添加</el-button>
            </template>
          </el-input>
        </div>
      </el-card>

      <!-- Scanned list -->
      <el-card shadow="never" class="scanned-card">
        <template #header>
          <div class="card-header">
            <span>已扫描串码 ({{ scannedSNs.length }})</span>
            <div class="card-header-btns">
              <el-button type="success" size="small" :disabled="scannedSNs.length === 0" @click="submitScan">
                提交保存
              </el-button>
              <el-button type="danger" size="small" plain :disabled="scannedSNs.length === 0" @click="scannedSNs = []">
                清空
              </el-button>
            </div>
          </div>
        </template>
        <el-table :data="scannedSNs.map((s, i) => ({ sn: s, idx: i }))" stripe max-height="300">
          <el-table-column label="序号" type="index" width="50" />
          <el-table-column label="串码">
            <template #default="{ row }">
              <code>{{ row.sn }}</code>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="{ row }">
              <el-button type="danger" size="small" link @click="scannedSNs.splice(row.idx, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="scannedSNs.length === 0" description="扫描条形码/二维码或手动输入串码" />
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Html5Qrcode } from 'html5-qrcode'
import { getTaskById, getTasks, scanTerminals } from '../api'
import type { RecycleTask } from '../types'

const route = useRoute()
const router = useRouter()
const taskId = ref(Number(route.params.id))

const task = ref<RecycleTask | null>(null)
const scannedSNs = ref<string[]>([])
const manualSn = ref('')
const searchKeyword = ref('')
const taskList = ref<RecycleTask[]>([])

let scanner: Html5Qrcode | null = null
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const isMobile = ref(window.innerWidth <= 768)

async function fetchTask() {
  if (taskId.value <= 0) return
  const { data: res } = await getTaskById(taskId.value)
  task.value = res.data
}

async function searchTasks() {
  const { data: res } = await getTasks({ keyword: searchKeyword.value, completed: false, page: 0, size: 50 })
  taskList.value = res.data.content
}

function debouncedSearch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(searchTasks, 300)
}

function selectTask(row: RecycleTask) {
  taskId.value = row.id
  router.replace(`/scan/${row.id}`)
  fetchTask()
  startScanner()
}

function addManual() {
  const val = manualSn.value.trim()
  if (!val) return
  if (scannedSNs.value.includes(val)) {
    ElMessage.warning('已存在')
  } else {
    scannedSNs.value.push(val)
    ElMessage.success('已添加: ' + val)
  }
  manualSn.value = ''
}

async function submitScan() {
  if (scannedSNs.value.length === 0) return
  const { data: res } = await scanTerminals(taskId.value, scannedSNs.value)
  ElMessage.success(res.message)
  scannedSNs.value = []
  fetchTask()
}

function startScanner() {
  try {
    scanner = new Html5Qrcode('reader')
    const qrboxSize = isMobile.value
      ? { width: Math.min(window.innerWidth - 80, 280), height: 150 }
      : { width: 250, height: 150 }
    scanner.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: qrboxSize },
      (text) => {
        if (text && !scannedSNs.value.includes(text)) {
          scannedSNs.value.push(text)
          ElMessage.success('扫描: ' + text)
        }
      },
      () => {}
    ).catch(() => {
      const el = document.getElementById('reader')
      if (el) el.innerHTML = '<div style="text-align:center;padding:40px;color:#999;">摄像头不可用，请手动输入串码</div>'
    })
  } catch (e) {
    console.warn('Scanner init failed')
  }
}

function stopScanner() {
  if (scanner) {
    try { scanner.stop() } catch (e) {}
    scanner = null
  }
}

onMounted(() => {
  if (taskId.value > 0) {
    fetchTask()
    startScanner()
  } else {
    searchTasks()
  }
})

onUnmounted(() => {
  stopScanner()
})
</script>

<style scoped>
.scan-page { max-width: 600px; margin: 0 auto; }
.page-title { font-size: 18px; font-weight: 600; }
.info-card { margin-top: 20px; }
.task-info { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.terminals-hint { font-size: 13px; color: #666; background: #f6f6f6; padding: 8px 12px; border-radius: 6px; word-break: break-all; }
.progress-text { margin-top: 8px; font-size: 14px; font-weight: 600; }
.scanner-card { margin-top: 16px; }
#reader { max-width: 400px; margin: 0 auto; border-radius: 12px; overflow: hidden; }
.manual-row { margin-top: 12px; }
.scanned-card { margin-top: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header-btns { display: flex; gap: 6px; }
.task-select-card { margin-top: 20px; }
code { background: #f0f0f0; padding: 2px 8px; border-radius: 4px; font-family: monospace; font-size: 12px; word-break: break-all; }

@media (max-width: 768px) {
  .page-title { font-size: 15px; }
  .card-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  .card-header-btns { width: 100%; }
  .card-header-btns .el-button { flex: 1; }
  :deep(.hide-mobile) { display: none !important; }
  #reader { max-width: 100%; }
}
</style>

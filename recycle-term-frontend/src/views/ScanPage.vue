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
      <el-input v-model="searchKeyword" placeholder="搜索用户号码、产品号或姓名..." clearable @input="debouncedSearch" style="margin: 12px 0">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-table :data="taskList" stripe @row-click="selectTask" style="cursor:pointer" max-height="400">
        <el-table-column label="用户" prop="userName" width="80" />
        <el-table-column label="用户号码" prop="phoneNumber" width="120" />
        <el-table-column label="产品号" prop="productId" width="120" show-overflow-tooltip class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column label="地址" prop="userAddress" show-overflow-tooltip class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column label="应回收" prop="expectedCount" width="70" align="center" />
      </el-table>
    </el-card>

    <!-- Scanner -->
    <template v-if="taskId > 0">
      <el-card shadow="never" class="info-card">
        <div class="task-info">
          <span><strong>{{ task?.userName }}</strong> ({{ task?.phoneNumber }}) 产品号: {{ task?.productId || '-' }}</span>
          <el-tag :type="statusTypeMap[task?.status ?? 0]?.type || 'info'" size="small">
            {{ statusTypeMap[task?.status ?? 0]?.label || '未知' }}
          </el-tag>
        </div>
        <div class="terminals-hint">{{ task?.terminals }}</div>
        <div class="progress-text">
          已扫描: {{ scannedSNs.length }} / {{ task?.expectedCount || '?' }}
        </div>
      </el-card>

      <!-- Locked notice when status >= 2 -->
      <el-alert v-if="(task?.status ?? 0) >= 2" type="warning" :closable="false" show-icon style="margin-top:16px">
        <template #title>该任务已{{ statusTypeMap[task?.status ?? 0]?.label }}，无法继续操作</template>
      </el-alert>

      <!-- Camera (only when status < 2) -->
      <el-card v-if="(task?.status ?? 0) < 2" shadow="never" class="scanner-card">
        <div class="scanner-viewport">
          <video ref="videoRef" class="scanner-video" autoplay playsinline muted></video>
          <div v-if="cameraError" class="camera-error">{{ cameraError }}</div>
          <div v-if="scanning" class="scan-indicator">扫描中...</div>
        </div>
        <canvas ref="canvasRef" class="scanner-canvas"></canvas>
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
            <div v-if="(task?.status ?? 0) < 2" class="card-header-btns">
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
          <el-table-column v-if="(task?.status ?? 0) < 2" label="操作" width="60">
            <template #default="{ row }">
              <el-button type="danger" size="small" link @click="scannedSNs.splice(row.idx, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="scannedSNs.length === 0" :description="(task?.status ?? 0) >= 2 ? '该任务已锁定' : '扫描条形码/二维码或手动输入串码'" />
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTaskById, getTasks, scanTerminals } from '../api'
// TODO: 设备类型统一后，此处改用 DeviceType API 获取设备类型列表
import { statusTypeMap } from '../constants'
import type { RecycleTask } from '../types'
import { useScanner } from '../composables/useScanner'

const route = useRoute()
const router = useRouter()
const taskId = ref(Number(route.params.id))

const task = ref<RecycleTask | null>(null)
const scannedSNs = ref<string[]>([])
const manualSn = ref('')
const searchKeyword = ref('')
const taskList = ref<RecycleTask[]>([])
const cameraError = ref('')
const { isScanning: scanning, videoRef, canvasRef, startScan, stopScan } = useScanner()
void videoRef
void canvasRef
let debounceTimer: ReturnType<typeof setTimeout> | null = null

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
  nextTick(startScanner)
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

async function startScanner() {
  cameraError.value = ''
  try {
    await startScan(onBarcodeDetected)
  } catch (err: any) {
    cameraError.value = '摄像头不可用: ' + (err?.message || err)
  }
}

function onBarcodeDetected(text: string) {
  if (!text || scannedSNs.value.includes(text)) return
  scannedSNs.value.push(text)
  ElMessage.success('扫描: ' + text)
}

onMounted(async () => {
  if (taskId.value > 0) {
    await fetchTask()
    if ((task.value?.status ?? 0) < 2) {
      nextTick(startScanner)
    }
  } else {
    searchTasks()
  }
})

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
  stopScan()
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
.scanner-viewport {
  position: relative;
  max-width: 400px;
  margin: 0 auto;
  border-radius: 12px;
  overflow: hidden;
  background: #000;
  aspect-ratio: 4 / 3;
}
.scanner-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.scanner-canvas { display: none; }
.camera-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 20px;
  color: #fff;
  background: rgba(0, 0, 0, 0.7);
  font-size: 14px;
}
.scan-indicator {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.5);
  color: #67c23a;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}
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
  .scanner-viewport { max-width: 100%; }
}
</style>

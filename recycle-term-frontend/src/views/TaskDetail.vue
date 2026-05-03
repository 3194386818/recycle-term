<template>
  <div class="task-detail" v-if="task">
    <el-page-header @back="$router.push('/')">
      <template #content>
        <span class="page-title">{{ task.userName }} 的回收任务</span>
      </template>
      <template #extra>
        <div class="header-actions">
          <el-button v-if="task.status === 0" type="primary" @click="changeStatus(1)">已上门</el-button>
          <el-button v-if="task.status === 0 || task.status === 1" type="danger" @click="showFailDialog">已失败</el-button>
          <el-button v-if="task.status === 1" type="success" @click="changeStatus(2)">已完成</el-button>
          <el-button v-if="task.status === 5" type="warning" @click="changeStatus(0)">重新处理</el-button>
          <el-button v-if="task.status < 2" type="primary" @click="$router.push(`/scan/${task.id}`)">
            <el-icon><Camera /></el-icon><span class="btn-text">扫码回收</span>
          </el-button>
        </div>
      </template>
    </el-page-header>

    <!-- Info -->
    <el-card shadow="never" class="info-card">
      <el-descriptions :column="descColumn" border>
        <el-descriptions-item label="用户号码">{{ task.phoneNumber }}</el-descriptions-item>
        <el-descriptions-item label="产品号">{{ task.productId || '-' }}</el-descriptions-item>
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
          <el-tag :type="statusTypeMap[task.status]?.type || 'info'">{{ statusTypeMap[task.status]?.label || '未知' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="是否上门">
          <el-tag :type="task.needVisit ? 'primary' : 'info'">{{ task.needVisit ? '是' : '否' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ task.completedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="应回收终端" :span="descColumn">{{ task.terminals || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="descColumn">{{ task.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="task.failReason" label="失败原因" :span="descColumn">
          <el-tag type="danger">{{ task.failReason }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="task.reviewRemark" label="审核备注" :span="descColumn">
          <el-tag :type="task.status === 4 ? 'success' : 'danger'">{{ task.reviewRemark }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- Scanned Records -->
    <el-card shadow="never" class="records-card">
      <template #header>
        <div class="card-header">
          <span>已扫描串码 ({{ records.length }})</span>
          <el-button v-if="task.status < 2" type="primary" size="small" @click="$router.push(`/scan/${task.id}`)">继续扫码</el-button>
        </div>
      </template>
        <el-table :data="records" stripe v-loading="recordsLoading">
        <el-table-column label="序号" type="index" width="50" />
        <el-table-column label="终端串码" prop="serialNumber" />
        <el-table-column label="扫描时间" prop="scannedAt" width="160" class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column v-if="task.status < 2" label="操作" width="70">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="removeRecord(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!recordsLoading && records.length === 0" description="暂无扫描记录" />
    </el-card>

    <!-- Fail reason dialog -->
    <el-dialog v-model="failDialogVisible" title="标记失败" width="400px" destroy-on-close>
      <el-form>
        <el-form-item label="失败原因" required>
          <el-select v-model="failReason" placeholder="请选择失败原因" style="width:100%">
            <el-option label="用户联系不上" value="用户联系不上" />
            <el-option label="用户终端丢失" value="用户终端丢失" />
            <el-option label="用户拒绝回收" value="用户拒绝回收" />
            <el-option label="设备对不上" value="设备对不上" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="failReason === '其他'" label="具体原因">
          <el-input v-model="customFailReason" type="textarea" :rows="2" placeholder="请输入具体原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="failDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="statusLoading" @click="submitFail">确认标记失败</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTaskById, updateTaskStatus, getRecordsByTaskId, deleteRecord } from '../api'
import type { RecycleTask, TerminalRecord } from '../types'
import { statusTypeMap } from '../constants'
import { useIsMobile } from '../composables/useIsMobile'

const route = useRoute()
const taskId = Number(route.params.id)
const isMobile = useIsMobile()
const descColumn = computed(() => isMobile.value ? 1 : 2)

const task = ref<RecycleTask | null>(null)
const records = ref<TerminalRecord[]>([])
const recordsLoading = ref(false)
const failDialogVisible = ref(false)
const failReason = ref('')
const customFailReason = ref('')
const statusLoading = ref(false)

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

async function changeStatus(status: number) {
  if (!task.value) return
  statusLoading.value = true
  try {
    await updateTaskStatus(taskId, status)
    ElMessage.success(`已更新为 ${statusTypeMap[status]?.label}`)
    fetchTask()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '状态更新失败')
  } finally {
    statusLoading.value = false
  }
}

function showFailDialog() {
  failReason.value = ''
  customFailReason.value = ''
  failDialogVisible.value = true
}

async function submitFail() {
  const reason = failReason.value === '其他' ? customFailReason.value : failReason.value
  if (!reason) {
    ElMessage.warning('请选择或输入失败原因')
    return
  }
  statusLoading.value = true
  try {
    await updateTaskStatus(taskId, 3, reason)
    ElMessage.success('已标记为失败，等待审核')
    failDialogVisible.value = false
    fetchTask()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '状态更新失败')
  } finally {
    statusLoading.value = false
  }
}

async function removeRecord(id: number) {
  try {
    await deleteRecord(id)
    ElMessage.success('已删除')
    fetchRecords()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
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

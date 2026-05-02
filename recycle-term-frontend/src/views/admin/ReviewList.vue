<template>
  <div class="review-list">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <el-input v-model="keyword" placeholder="搜索号码、姓名、地址..." clearable style="width:300px" @input="debouncedFetch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-tag type="warning">待审核任务：{{ total }}</el-tag>
        </div>
      </template>
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="产品号" prop="productId" width="120" />
        <el-table-column label="用户" prop="userName" width="100" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : 'danger'" size="small">
              {{ row.status === 2 ? '已完成' : '已失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" prop="failReason" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.failReason || '-' }}</template>
        </el-table-column>
        <el-table-column label="回收终端" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.terminals">{{ row.terminals }}</span>
            <span v-else style="color:#999">无</span>
          </template>
        </el-table-column>
        <el-table-column label="已扫描" width="80" align="center">
          <template #default="{ row }">{{ row.fttrCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">查看</el-button>
            <el-button size="small" type="success" @click="showReview(row)">审核</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchTasks" />
      </div>
    </el-card>

    <!-- Detail dialog -->
    <el-dialog v-model="detailVisible" title="任务详情" width="700px" destroy-on-close>
      <div v-if="detailTask">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="产品号">{{ detailTask.productId }}</el-descriptions-item>
          <el-descriptions-item label="用户号码">{{ detailTask.phoneNumber }}</el-descriptions-item>
          <el-descriptions-item label="用户名称">{{ detailTask.userName }}</el-descriptions-item>
          <el-descriptions-item label="区域">{{ detailTask.area }}</el-descriptions-item>
          <el-descriptions-item label="用户地址" :span="2">{{ detailTask.userAddress }}</el-descriptions-item>
          <el-descriptions-item label="工程师">{{ detailTask.engineerName }} ({{ detailTask.engineerPhone }})</el-descriptions-item>
          <el-descriptions-item label="应回收">{{ detailTask.expectedCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="失败原因" :span="2">
            <el-tag type="danger" v-if="detailTask.failReason">{{ detailTask.failReason }}</el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailTask.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:16px 0 8px">已扫描串码 ({{ detailRecords.length }})</h4>
        <el-table :data="detailRecords" stripe size="small" max-height="200">
          <el-table-column label="序号" type="index" width="50" />
          <el-table-column label="终端串码" prop="serialNumber" />
          <el-table-column label="扫描时间" prop="scannedAt" width="160" />
        </el-table>
        <el-empty v-if="detailRecords.length === 0" description="暂无扫描记录" :image-size="60" />
      </div>
    </el-dialog>

    <!-- Review dialog -->
    <el-dialog v-model="reviewDialogVisible" title="审核任务" width="450px" destroy-on-close>
      <div v-if="reviewingTask" style="margin-bottom:16px">
        <p><strong>{{ reviewingTask.userName }}</strong> ({{ reviewingTask.productId }})</p>
        <p>状态：<el-tag :type="reviewingTask.status === 2 ? 'success' : 'danger'" size="small">{{ reviewingTask.status === 2 ? '已完成' : '已失败' }}</el-tag></p>
        <p v-if="reviewingTask.failReason">失败原因：<el-tag type="danger" size="small">{{ reviewingTask.failReason }}</el-tag></p>
      </div>
      <el-form>
        <el-form-item label="审核备注">
          <el-input v-model="reviewRemark" type="textarea" :rows="2" placeholder="驳回时请填写原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="reviewLoading" @click="submitReview(false)">驳回</el-button>
        <el-button type="success" :loading="reviewLoading" @click="submitReview(true)">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminTasks, reviewTask } from '../../api/admin'
import { getRecordsByTaskId } from '../../api'
import type { RecycleTask, TerminalRecord } from '../../types'

const tasks = ref<RecycleTask[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

const detailVisible = ref(false)
const detailTask = ref<RecycleTask | null>(null)
const detailRecords = ref<TerminalRecord[]>([])

const reviewDialogVisible = ref(false)
const reviewingTask = ref<RecycleTask | null>(null)
const reviewRemark = ref('')
const reviewLoading = ref(false)

let debounceTimer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { page.value = 0; fetchTasks() }, 300)
}

async function fetchTasks() {
  loading.value = true
  try {
    const { data: res } = await getAdminTasks({ keyword: keyword.value, page: page.value, size: size.value })
    tasks.value = res.data.content.filter((t: RecycleTask) => t.status === 2 || t.status === 3)
    total.value = tasks.value.length
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

function showReview(row: RecycleTask) {
  reviewingTask.value = row
  reviewRemark.value = ''
  reviewDialogVisible.value = true
}

async function submitReview(approved: boolean) {
  if (!reviewingTask.value) return
  reviewLoading.value = true
  try {
    await reviewTask(reviewingTask.value.id, approved, reviewRemark.value || undefined)
    ElMessage.success(approved ? '审核通过' : '已驳回')
    reviewDialogVisible.value = false
    fetchTasks()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '审核失败')
  } finally {
    reviewLoading.value = false
  }
}

onMounted(fetchTasks)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

<template>
  <div class="task-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <el-input v-model="keyword" placeholder="搜索号码、姓名、地址..." clearable style="width:300px" @input="debouncedFetch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="showAdd">添加任务</el-button>
        </div>
      </template>
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="用户" prop="userName" width="100" />
        <el-table-column label="用户号码" prop="phoneNumber" width="120" />
        <el-table-column label="产品号" prop="productId" width="120" />
        <el-table-column label="地址" prop="userAddress" show-overflow-tooltip min-width="160" />
        <el-table-column label="区域" prop="area" width="80" />
        <el-table-column label="应回收" prop="expectedCount" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.status]?.type || 'info'" size="small">{{ statusTypeMap[row.status]?.label || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">查看</el-button>
            <el-button v-if="row.status === 2 || row.status === 3" size="small" type="warning" @click="showReview(row)">审核</el-button>
            <el-button size="small" type="primary" @click="showEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchTasks" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑任务' : '添加任务'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="用户名称"><el-input v-model="form.userName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="用户号码"><el-input v-model="form.phoneNumber" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="产品号"><el-input v-model="form.productId" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="用户地址"><el-input v-model="form.userAddress" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="区域"><el-input v-model="form.area" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="工程师"><el-input v-model="form.engineerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="工程师电话"><el-input v-model="form.engineerPhone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="分类"><el-input v-model="form.category" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="应回收数量"><el-input-number v-model="form.expectedCount" :min="0" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="FTTR数量"><el-input-number v-model="form.fttrCount" :min="0" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="接入间"><el-input v-model="form.accessRoom" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="发展部门"><el-input v-model="form.devDept" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="发展员工"><el-input v-model="form.devPerson" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="终端串码"><el-input v-model="form.terminals" type="textarea" :rows="3" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialogVisible" title="审核任务" width="450px" destroy-on-close>
      <div v-if="reviewingTask" style="margin-bottom:16px">
        <p><strong>{{ reviewingTask.userName }}</strong> ({{ reviewingTask.productId }})</p>
        <p>状态：<el-tag :type="statusTypeMap[reviewingTask.status]?.type" size="small">{{ statusTypeMap[reviewingTask.status]?.label }}</el-tag></p>
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
        <el-button type="success" :loading="reviewLoading" @click="submitReview(true)">归档</el-button>
      </template>
    </el-dialog>

    <!-- Detail dialog -->
    <el-dialog v-model="detailVisible" title="任务详情" width="800px" destroy-on-close>
      <div v-if="detailTask">
        <!-- Status Steps -->
        <el-steps :active="statusStepIndex(detailTask.status)" finish-status="success" align-center style="margin-bottom:24px">
          <el-step title="待回收" />
          <el-step title="已上门" />
          <el-step :title="detailTask.status === 3 ? '已失败' : '已完成'" />
          <el-step :title="detailTask.status === 5 ? '审核失败' : '已归档'" />
        </el-steps>

        <el-tabs type="border-card">
          <el-tab-pane label="用户信息">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="用户名称">{{ detailTask.userName }}</el-descriptions-item>
              <el-descriptions-item label="用户号码">{{ detailTask.phoneNumber }}</el-descriptions-item>
              <el-descriptions-item label="产品号">{{ detailTask.productId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="区域">{{ detailTask.area || '-' }}</el-descriptions-item>
              <el-descriptions-item label="用户地址" :span="2">{{ detailTask.userAddress || '-' }}</el-descriptions-item>
              <el-descriptions-item label="工程师">{{ detailTask.engineerName || '-' }} ({{ detailTask.engineerPhone || '-' }})</el-descriptions-item>
              <el-descriptions-item label="分类">{{ detailTask.category || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="终端信息">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="应回收数量">{{ detailTask.expectedCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="FTTR主光猫">{{ detailTask.fttrCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="接入间">{{ detailTask.accessRoom || '-' }}</el-descriptions-item>
              <el-descriptions-item label="发展部门">{{ detailTask.devDept || '-' }}</el-descriptions-item>
              <el-descriptions-item label="发展员工">{{ detailTask.devPerson || '-' }}</el-descriptions-item>
              <el-descriptions-item label="应回收终端" :span="2">{{ detailTask.terminals || '-' }}</el-descriptions-item>
            </el-descriptions>
            <h4 style="margin:16px 0 8px">已扫描串码 ({{ detailRecords.length }})</h4>
            <el-table :data="detailRecords" stripe size="small" max-height="200">
              <el-table-column label="序号" type="index" width="50" />
              <el-table-column label="终端串码" prop="serialNumber" />
              <el-table-column label="扫描时间" prop="scannedAt" width="160" />
            </el-table>
            <el-empty v-if="detailRecords.length === 0" description="暂无扫描记录" :image-size="60" />
          </el-tab-pane>

          <el-tab-pane label="状态与审核">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="当前状态">
                <el-tag :type="statusTypeMap[detailTask.status]?.type" size="small">{{ statusTypeMap[detailTask.status]?.label }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="是否上门">{{ detailTask.needVisit ? '是' : '否' }}</el-descriptions-item>
              <el-descriptions-item label="完成时间">{{ detailTask.completedAt || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ detailTask.createdAt }}</el-descriptions-item>
              <el-descriptions-item label="失败原因" :span="2" v-if="detailTask.failReason">
                <el-tag type="danger">{{ detailTask.failReason }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="审核备注" :span="2" v-if="detailTask.reviewRemark">{{ detailTask.reviewRemark }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ detailTask.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminTasks, createTask, updateAdminTask, deleteAdminTask, reviewTask, getAdminRecords } from '../../api/admin'
import { statusTypeMap } from '../../constants'
import type { RecycleTask } from '../../types'

const tasks = ref<RecycleTask[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const submitting = ref(false)
const form = ref<Partial<RecycleTask>>({})

const reviewDialogVisible = ref(false)
const reviewingTask = ref<RecycleTask | null>(null)
const reviewRemark = ref('')
const reviewLoading = ref(false)

const detailVisible = ref(false)
const detailTask = ref<RecycleTask | null>(null)
const detailRecords = ref<any[]>([])

let debounceTimer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { page.value = 0; fetchTasks() }, 300)
}

async function fetchTasks() {
  loading.value = true
  try {
    const { data: res } = await getAdminTasks({ keyword: keyword.value, page: page.value, size: size.value })
    tasks.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

function showAdd() {
  editId.value = null
  form.value = {}
  dialogVisible.value = true
}

function showEdit(row: RecycleTask) {
  editId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (editId.value) {
      await updateAdminTask(editId.value, form.value)
      ElMessage.success('修改成功')
    } else {
      await createTask(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchTasks()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteAdminTask(id)
    ElMessage.success('删除成功')
    fetchTasks()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

function statusStepIndex(status: number): number {
  const map: Record<number, number> = { 0: 0, 1: 1, 2: 2, 3: 2, 4: 3, 5: 3, 6: 3 }
  return map[status] ?? 0
}

async function showDetail(row: RecycleTask) {
  detailTask.value = row
  detailVisible.value = true
  try {
    const { data: res } = await getAdminRecords(row.id)
    detailRecords.value = res.data
  } catch {
    detailRecords.value = []
  }
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
    ElMessage.success(approved ? '已归档' : '已驳回')
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

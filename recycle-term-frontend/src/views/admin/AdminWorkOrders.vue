<template>
  <div class="admin-work-orders">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>工单管理</span>
          <el-button type="primary" @click="showAdd">新增工单</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input v-model="filters.keyword" placeholder="搜索工单号/产品号/用户/电话/地址/SN" clearable @input="debouncedFetch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filters.status" placeholder="状态" clearable @change="fetchOrders">
          <el-option v-for="item in options.statuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.type" placeholder="类型" clearable @change="fetchOrders">
          <el-option v-for="item in options.types" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="fetchOrders">查询</el-button>
      </div>

      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column label="工单号" prop="workOrderNo" width="150" />
        <el-table-column label="产品号" prop="productId" width="130" />
        <el-table-column label="用户" prop="userName" width="100" />
        <el-table-column label="电话" prop="contactPhone" width="120" />
        <el-table-column label="类型" prop="workOrderType" width="90" />
        <el-table-column label="地址" prop="address" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" :disabled="isTerminal(row.status)" @click="showStatus(row)">状态</el-button>
            <el-popconfirm title="确定删除该工单？" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="fetchOrders" @size-change="fetchOrders" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑工单' : '新增工单'" width="760px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-row :gutter="14">
          <el-col :span="12"><el-form-item label="工单号"><el-input v-model="form.workOrderNo" placeholder="不填自动生成" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="产品号"><el-input v-model="form.productId" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="用户名"><el-input v-model="form.userName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系方式"><el-input v-model="form.contactPhone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="类型"><el-select v-model="form.workOrderType" style="width:100%"><el-option v-for="item in options.types" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="地址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="CVLAN"><el-input v-model="form.cvlan" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="SVLAN"><el-input v-model="form.svlan" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="分光器"><el-input v-model="form.splitter" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="分光器端口"><el-input v-model="form.splitterPort" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="SN"><el-input v-model="form.onuSn" placeholder="可为空" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="更新状态" width="460px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="目标状态">
          <el-select v-model="statusForm.status" style="width:100%"><el-option v-for="item in nextStatuses" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        </el-form-item>
        <el-form-item v-if="statusForm.status === 'FAILED'" label="失败原因" required>
          <el-select v-model="statusForm.failureReason" style="width:100%" filterable allow-create><el-option v-for="item in options.failureReasons" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitStatus">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createAdminWorkOrder, deleteAdminWorkOrder, getAdminOptions, getAdminWorkOrders, updateAdminWorkOrder, updateAdminWorkOrderStatus } from '../../api/workOrder'
import { workOrderStatusMap } from '../../constants'
import type { WorkOrder, WorkOrderOption, WorkOrderOptions } from '../../types'
import { formatDateTime } from '../../utils/datetime'

const filters = ref({ keyword: '', status: '', type: '' })
const orders = ref<WorkOrder[]>([])
const options = ref<WorkOrderOptions>({ types: [], failureReasons: [], statuses: [] })
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const statusVisible = ref(false)
const editId = ref<number | null>(null)
const form = ref<Partial<WorkOrder>>({})
const currentStatus = ref('')
const statusForm = ref({ id: 0, status: '', failureReason: '' })
let timer: ReturnType<typeof setTimeout> | null = null

const transitionMap: Record<string, string[]> = { ACCEPTED: ['APPOINTED', 'FULFILLING', 'TRANSFERRED', 'FAILED'], APPOINTED: ['FULFILLING', 'TRANSFERRED', 'FAILED'], FULFILLING: ['COMPLETED', 'TRANSFERRED', 'FAILED'] }
const nextStatuses = computed<WorkOrderOption[]>(() => (transitionMap[currentStatus.value] || []).map(value => options.value.statuses.find(item => item.value === value) || { id: 0, category: 'STATUS', value, label: statusLabel(value), sortOrder: 0, enabled: true }))

function statusLabel(value: string) { return options.value.statuses.find(item => item.value === value)?.label || workOrderStatusMap[value]?.label || value }
function statusType(value: string) { return workOrderStatusMap[value]?.type || 'info' }
function isTerminal(value: string) { return !transitionMap[value] }

function debouncedFetch() { if (timer) clearTimeout(timer); timer = setTimeout(() => { page.value = 1; fetchOrders() }, 300) }

async function fetchOptions() {
  try { const { data: res } = await getAdminOptions(); options.value = res.data } catch { options.value = { types: [], failureReasons: [], statuses: [] } }
}

async function fetchOrders() {
  loading.value = true
  try {
    const { data: res } = await getAdminWorkOrders({ keyword: filters.value.keyword, status: filters.value.status || undefined, type: filters.value.type || undefined, page: page.value - 1, size: size.value })
    orders.value = res.data.content
    total.value = res.data.totalElements
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '获取工单失败') }
  finally { loading.value = false }
}

function showAdd() {
  editId.value = null
  form.value = { status: 'ACCEPTED', workOrderType: options.value.types[0]?.value }
  dialogVisible.value = true
}

function showEdit(row: WorkOrder) {
  editId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

async function submitForm() {
  submitting.value = true
  try {
    if (editId.value) await updateAdminWorkOrder(editId.value, form.value)
    else await createAdminWorkOrder(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchOrders()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '保存失败') }
  finally { submitting.value = false }
}

function showStatus(row: WorkOrder) {
  currentStatus.value = row.status
  statusForm.value = { id: row.id, status: nextStatuses.value[0]?.value || '', failureReason: row.failureReason || '' }
  statusVisible.value = true
}

async function submitStatus() {
  if (statusForm.value.status === 'FAILED' && !statusForm.value.failureReason) { ElMessage.warning('请选择失败原因'); return }
  submitting.value = true
  try {
    await updateAdminWorkOrderStatus(statusForm.value.id, statusForm.value.status, statusForm.value.failureReason || undefined)
    ElMessage.success('状态已更新')
    statusVisible.value = false
    fetchOrders()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '状态更新失败') }
  finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try { await deleteAdminWorkOrder(id); ElMessage.success('删除成功'); fetchOrders() }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '删除失败') }
}

onMounted(() => { fetchOptions(); fetchOrders() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.filters .el-input { width: 320px; }
.filters .el-select { width: 150px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
@media (max-width: 768px) { .filters .el-input, .filters .el-select, .filters .el-button { width: 100%; } }
</style>

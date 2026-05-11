<template>
  <div class="detail-page" v-loading="loading">
    <el-page-header class="page-header" @back="router.push('/work-orders')">
      <template #content><span class="page-title">工单详情</span></template>
    </el-page-header>

    <template v-if="order">
      <el-card class="status-card" shadow="never">
        <div class="status-top">
          <div>
            <div class="order-no">{{ order.workOrderNo }}</div>
            <div class="source">产品号：{{ order.productId || '-' }}</div>
          </div>
          <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
        </div>
        <el-steps :active="workOrderStatusStepMap[order.status] ?? 0" finish-status="success" simple>
          <el-step title="接单" />
          <el-step title="预约" />
          <el-step title="履约" />
          <el-step :title="finalStepTitle" :status="order.status === 'FAILED' ? 'error' : undefined" />
        </el-steps>
        <el-button class="status-btn" type="primary" plain :disabled="isTerminal" @click="openStatusDialog">更新状态</el-button>
      </el-card>

      <el-card class="info-card" shadow="never">
        <template #header>用户信息</template>
        <div class="info-list">
          <div><span>用户</span><strong>{{ order.userName || '-' }}</strong></div>
          <div><span>联系方式</span><strong>{{ order.contactPhone || '-' }}</strong></div>
          <div><span>地址</span><strong>{{ order.address || '-' }}</strong></div>
          <div><span>工单类型</span><strong>{{ order.workOrderType || '-' }}</strong></div>
        </div>
      </el-card>

      <el-card class="info-card" shadow="never">
        <template #header>配置信息</template>
        <div class="info-list">
          <div><span>CVLAN</span><strong>{{ order.cvlan || '-' }}</strong></div>
          <div><span>SVLAN</span><strong>{{ order.svlan || '-' }}</strong></div>
          <div><span>分光器</span><strong>{{ order.splitter || '-' }}</strong></div>
          <div><span>分光器端口</span><strong>{{ order.splitterPort || '-' }}</strong></div>
          <div><span>SN</span><strong>{{ order.onuSn || '暂无设备/SN' }}</strong></div>
        </div>
      </el-card>

      <el-card class="info-card" shadow="never">
        <template #header>状态信息</template>
        <div class="info-list">
          <div v-if="order.failureReason"><span>失败原因</span><strong class="danger">{{ order.failureReason }}</strong></div>
          <div><span>预约时间</span><strong>{{ formatDateTime(order.appointedAt) }}</strong></div>
          <div><span>履约时间</span><strong>{{ formatDateTime(order.fulfilledAt) }}</strong></div>
          <div><span>完成时间</span><strong>{{ formatDateTime(order.completedAt) }}</strong></div>
          <div><span>调走时间</span><strong>{{ formatDateTime(order.transferredAt) }}</strong></div>
          <div><span>失败时间</span><strong>{{ formatDateTime(order.failedAt) }}</strong></div>
          <div><span>备注</span><strong>{{ order.remark || '-' }}</strong></div>
          <div><span>创建时间</span><strong>{{ formatDateTime(order.createdAt) }}</strong></div>
        </div>
      </el-card>

      <el-card class="info-card terminal-card" shadow="never">
        <template #header>终端信息</template>
        <template v-if="terminalLoading">
          <div class="terminal-loading">正在加载终端...</div>
        </template>
        <template v-else-if="terminals.length">
          <div class="terminal-list">
            <div v-for="item in terminals" :key="`${item.id || item.sn}-${item.warehouseId || 0}`" class="terminal-item">
              <div class="terminal-header">
                <span class="terminal-type">{{ item.type || '设备' }}</span>
                <el-tag size="small" :type="item.outbound ? 'info' : 'success'">{{ item.outbound ? '已出库' : '在库' }}</el-tag>
              </div>
              <div class="terminal-label">串码</div>
              <div class="terminal-value">{{ item.sn }}</div>
              <div class="terminal-meta">仓库：{{ item.warehouseName || '-' }}</div>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无终端信息" :image-size="56" />
      </el-card>
    </template>

    <el-dialog v-model="statusDialog" title="更新状态" width="92%" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="目标状态">
          <el-select v-model="statusForm.status" style="width:100%">
            <el-option v-for="item in nextStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="statusForm.status === 'FAILED'" label="失败原因" required>
          <el-select v-model="statusForm.failureReason" style="width:100%" filterable allow-create>
            <el-option v-for="item in options.failureReasons" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="statusForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitStatus">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getWorkOrderById, getWorkOrderOptions, updateWorkOrderStatus } from '../../api/workOrder'
import { searchWarehouse } from '../../api/warehouse'
import { workOrderStatusMap, workOrderStatusStepMap } from '../../constants'
import type { WorkOrder, WorkOrderOption, WorkOrderOptions } from '../../types'
import type { WarehouseDevice } from '../../api/warehouse'
import { formatDateTime } from '../../utils/datetime'

const route = useRoute()
const router = useRouter()
const order = ref<WorkOrder | null>(null)
const loading = ref(false)
const submitting = ref(false)
const terminalLoading = ref(false)
const terminals = ref<WarehouseDevice[]>([])
const statusDialog = ref(false)
const options = ref<WorkOrderOptions>({ types: [], failureReasons: [], statuses: [] })
const statusForm = ref({ status: '', failureReason: '', remark: '' })

const transitionMap: Record<string, string[]> = {
  ACCEPTED: ['APPOINTED', 'FULFILLING', 'TRANSFERRED', 'FAILED'],
  APPOINTED: ['FULFILLING', 'TRANSFERRED', 'FAILED'],
  FULFILLING: ['COMPLETED', 'TRANSFERRED', 'FAILED'],
}

const isTerminal = computed(() => !!order.value && !transitionMap[order.value.status])
const finalStepTitle = computed(() => order.value?.status === 'TRANSFERRED' ? '调走' : order.value?.status === 'FAILED' ? '失败' : '完成')
const nextStatuses = computed<WorkOrderOption[]>(() => {
  if (!order.value) return []
  const allowed = transitionMap[order.value.status] || []
  return allowed.map(value => options.value.statuses.find(item => item.value === value) || { id: 0, category: 'STATUS', value, label: statusLabel(value), sortOrder: 0, enabled: true })
})

function statusLabel(value: string) {
  return options.value.statuses.find(item => item.value === value)?.label || workOrderStatusMap[value]?.label || value
}

function statusType(value: string) {
  return workOrderStatusMap[value]?.type || 'info'
}

async function fetchOptions() {
  try {
    const { data: res } = await getWorkOrderOptions()
    options.value = res.data
  } catch {
    options.value = { types: [], failureReasons: [], statuses: [] }
  }
}

async function fetchOrder() {
  loading.value = true
  try {
    const { data: res } = await getWorkOrderById(Number(route.params.id))
    order.value = res.data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '获取工单详情失败')
  } finally {
    loading.value = false
  }
}

async function fetchTerminalsByProductId() {
  if (!order.value?.productId) {
    terminals.value = []
    return
  }
  terminalLoading.value = true
  try {
    const { data: res } = await searchWarehouse(order.value.productId, 0, 20, undefined, true, false)
    const exact = res.data.content.filter(item => item.productId === order.value?.productId)
    terminals.value = exact.flatMap(item => item.devices || [])
  } catch {
    terminals.value = []
  } finally {
    terminalLoading.value = false
  }
}

function openStatusDialog() {
  statusForm.value = { status: nextStatuses.value[0]?.value || '', failureReason: order.value?.failureReason || '', remark: order.value?.remark || '' }
  statusDialog.value = true
}

async function submitStatus() {
  if (!order.value || !statusForm.value.status) return
  if (statusForm.value.status === 'FAILED' && !statusForm.value.failureReason) {
    ElMessage.warning('请选择失败原因')
    return
  }
  submitting.value = true
  try {
    const { data: res } = await updateWorkOrderStatus(order.value.id, statusForm.value.status, statusForm.value.failureReason || undefined)
    order.value = res.data
    ElMessage.success('状态已更新')
    statusDialog.value = false
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '状态更新失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchOptions()
  fetchOrder().then(fetchTerminalsByProductId)
})
</script>

<style scoped>
.detail-page { max-width: 820px; margin: 0 auto; padding: 14px; min-height: 100vh; background: #f5f7fa; }
.page-header { margin-bottom: 12px; }
.page-title { font-weight: 700; }
.status-card, .info-card { margin-bottom: 12px; border-radius: 14px; }
.status-top { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.order-no { font-size: 17px; font-weight: 700; }
.source { margin-top: 4px; color: #909399; font-size: 12px; }
.status-btn { width: 100%; margin-top: 14px; }
.info-list { display: grid; gap: 12px; }
.info-list div { display: flex; gap: 12px; align-items: flex-start; }
.info-list span { width: 88px; flex-shrink: 0; color: #909399; }
.info-list strong { flex: 1; font-weight: 500; word-break: break-all; }
.danger { color: #f56c6c; }
.terminal-card :deep(.el-card__body) { padding-top: 10px; }
.terminal-loading { color: #909399; padding: 10px 0; }
.terminal-list { display: grid; gap: 10px; }
.terminal-item {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 14px;
  background: #fafafa;
}
.terminal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.terminal-type { font-weight: 600; color: #303133; }
.terminal-label { color: #909399; font-size: 12px; margin-bottom: 6px; }
.terminal-value { color: #303133; font-weight: 600; word-break: break-all; }
.terminal-meta { color: #909399; font-size: 12px; margin-top: 8px; }
</style>

<template>
  <div class="work-order-page">
    <el-page-header class="page-header" @back="router.push('/')">
      <template #content><span class="page-title">工单系统</span></template>
      <template #extra><el-button type="primary" @click="router.push('/work-orders/quick')">快速录入</el-button></template>
    </el-page-header>

    <el-card class="toolbar" shadow="never">
      <el-input v-model="keyword" placeholder="搜索工单号、产品号、用户、电话、地址、SN" clearable @input="debouncedFetch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <div class="status-scroll">
        <el-check-tag :checked="!status" @change="setStatus('')">全部</el-check-tag>
        <el-check-tag v-for="item in statusOptions" :key="item.value" :checked="status === item.value" @change="setStatus(item.value)">
          {{ item.label }}
        </el-check-tag>
      </div>
    </el-card>

    <div v-loading="loading" class="order-list">
      <el-empty v-if="!loading && orders.length === 0" description="暂无工单" />
      <el-card v-for="order in orders" :key="order.id" class="order-card" shadow="hover" @click="router.push(`/work-orders/${order.id}`)">
        <div class="card-top">
          <div>
            <div class="order-no">{{ order.workOrderNo }}</div>
            <div class="product-id">产品号：{{ order.productId || '-' }}</div>
          </div>
          <el-tag :type="statusMeta(order.status).type" size="small">{{ statusMeta(order.status).label }}</el-tag>
        </div>
        <div class="main-info">
          <div><strong>{{ order.userName || '未填写用户' }}</strong><span v-if="order.contactPhone"> · {{ order.contactPhone }}</span></div>
          <div class="address">{{ order.address || '-' }}</div>
        </div>
        <div class="chips">
          <el-tag v-if="order.workOrderType" size="small" effect="plain">{{ order.workOrderType }}</el-tag>
          <el-tag v-if="order.onuSn" size="small" effect="plain">SN: {{ order.onuSn }}</el-tag>
          <el-tag v-if="order.splitter" size="small" effect="plain">{{ order.splitter }}</el-tag>
        </div>
        <div class="card-actions" @click.stop>
          <el-button size="small" @click="copy(order.productId || order.workOrderNo)">复制</el-button>
          <el-button v-if="order.contactPhone" size="small" type="primary" plain @click="call(order.contactPhone)">拨号</el-button>
        </div>
      </el-card>
    </div>

    <div v-if="total > 0" class="pagination">
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="fetchOrders" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getWorkOrderOptions, getWorkOrders } from '../../api/workOrder'
import { workOrderStatusMap } from '../../constants'
import type { WorkOrder, WorkOrderOption, WorkOrderOptions } from '../../types'

const router = useRouter()
const keyword = ref('')
const status = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const orders = ref<WorkOrder[]>([])
const options = ref<WorkOrderOptions>({ types: [], failureReasons: [], statuses: [] })
let timer: ReturnType<typeof setTimeout> | null = null

const statusOptions = computed<WorkOrderOption[]>(() => options.value.statuses.length ? options.value.statuses : Object.entries(workOrderStatusMap).map(([value, meta], index) => ({ id: index, category: 'STATUS', value, label: meta.label, sortOrder: index, enabled: true })))

function statusMeta(value: string) {
  const option = statusOptions.value.find(item => item.value === value)
  return { label: option?.label || workOrderStatusMap[value]?.label || value || '未知', type: workOrderStatusMap[value]?.type || 'info' }
}

function setStatus(value: string) {
  status.value = value
  page.value = 1
  fetchOrders()
}

function debouncedFetch() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { page.value = 1; fetchOrders() }, 300)
}

async function fetchOptions() {
  try {
    const { data: res } = await getWorkOrderOptions()
    options.value = res.data
  } catch {
    options.value = { types: [], failureReasons: [], statuses: [] }
  }
}

async function fetchOrders() {
  loading.value = true
  try {
    const { data: res } = await getWorkOrders({ keyword: keyword.value, status: status.value || undefined, page: page.value - 1, size: size.value })
    orders.value = res.data.content
    total.value = res.data.totalElements
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '获取工单失败')
  } finally {
    loading.value = false
  }
}

function copy(text: string) {
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制')).catch(() => ElMessage.error('复制失败'))
}

function call(phone: string) {
  window.location.href = `tel:${phone}`
}

onMounted(() => {
  fetchOptions()
  fetchOrders()
})
</script>

<style scoped>
.work-order-page { max-width: 820px; margin: 0 auto; padding: 14px; min-height: 100vh; background: #f5f7fa; }
.page-header { margin-bottom: 12px; }
.page-title { font-size: 18px; font-weight: 700; }
.toolbar { margin-bottom: 12px; border-radius: 12px; }
.status-scroll { display: flex; gap: 8px; overflow-x: auto; padding-top: 12px; }
.order-list { display: grid; gap: 12px; }
.order-card { border-radius: 14px; cursor: pointer; }
.card-top { display: flex; justify-content: space-between; gap: 12px; }
.order-no { font-weight: 700; color: #1f2937; }
.product-id { margin-top: 4px; font-size: 12px; color: #8c8c8c; }
.main-info { margin-top: 12px; color: #303133; line-height: 1.7; }
.address { color: #606266; word-break: break-all; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.card-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; padding-top: 12px; border-top: 1px solid #f0f0f0; }
.pagination { display: flex; justify-content: center; margin-top: 16px; }
@media (min-width: 769px) { .order-list { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>

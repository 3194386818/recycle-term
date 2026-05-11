<template>
  <div class="warehouse-detail" v-if="item">
    <el-page-header @back="$router.push('/warehouse')">
      <template #content><span class="page-title">{{ item.productId }}</span></template>
    </el-page-header>

    <el-card shadow="never" style="margin-top:16px">
      <el-descriptions :column="descColumn" border>
        <el-descriptions-item label="产品号">{{ item.productId }}</el-descriptions-item>
        <el-descriptions-item label="入库时间">{{ formatDateTime(item.receivedAt) }}</el-descriptions-item>
        <el-descriptions-item label="客户名字">{{ item.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ item.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="SN号">{{ item.snNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分光器">{{ item.splitter || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接入间">{{ item.accessRoom || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="descColumn">{{ item.address || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header><span>设备列表 ({{ devices.length }})</span></template>
      <el-table :data="devices" stripe size="small">
        <el-table-column label="序号" type="index" width="60" />
        <el-table-column label="设备类型" prop="type" width="150" />
        <el-table-column label="串码" prop="sn" show-overflow-tooltip />
        <el-table-column label="所属仓库" prop="warehouseName" width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.outbound ? 'info' : 'success'">{{ row.outbound ? '已出库' : '在库' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="出库时间" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.outboundAt) }}</template>
        </el-table-column>
        <el-table-column label="轨迹" width="90">
          <template #default="{ row }"><el-button size="small" @click="showMovements(row)">查看</el-button></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="devices.length === 0" description="暂无设备" />
    </el-card>

    <el-dialog v-model="movementVisible" title="设备轨迹" width="560px">
      <el-timeline>
        <el-timeline-item v-for="m in movements" :key="m.id" :timestamp="formatDateTime(m.createdAt)">
          {{ movementLabel(m) }}
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="movements.length === 0" description="暂无轨迹" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getWarehouseItem, getWarehouseDeviceMovements } from '../../api/warehouse'
import type { WarehouseItem, DeviceInfo, WarehouseDeviceMovement } from '../../api/warehouse'
import { formatDateTime } from '../../utils/datetime'

const route = useRoute()
const itemId = Number(route.params.id)
const isMobile = ref(window.innerWidth <= 768)
window.addEventListener('resize', () => { isMobile.value = window.innerWidth <= 768 })
const descColumn = computed(() => isMobile.value ? 1 : 2)

const item = ref<WarehouseItem | null>(null)
const movementVisible = ref(false)
const movements = ref<WarehouseDeviceMovement[]>([])

const devices = computed<DeviceInfo[]>(() => item.value?.devices || [])

async function fetchItem() {
  try {
    const { data: res } = await getWarehouseItem(itemId)
    item.value = res.data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  }
}

async function showMovements(device: DeviceInfo) {
  if (!device.id) return
  const { data: res } = await getWarehouseDeviceMovements(device.id)
  movements.value = res.data
  movementVisible.value = true
}

function movementLabel(m: WarehouseDeviceMovement) {
  if (m.movementType === 'INBOUND') return `入库到 ${m.toWarehouseName || '-'}`
  if (m.movementType === 'TRANSFER') return `从 ${m.fromWarehouseName || '-'} 转到 ${m.toWarehouseName || '-'}`
  if (m.movementType === 'OUTBOUND') return `从 ${m.fromWarehouseName || '-'} 出库`
  return m.movementType
}

onMounted(fetchItem)
</script>

<style scoped>
.page-title { font-weight: 600; }
</style>

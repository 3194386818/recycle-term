<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>仓库变更申请审核</span>
          <el-button size="small" @click="fetchRequests">刷新</el-button>
        </div>
      </template>
      <el-table :data="requests" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="工单ID" prop="warehouseItemId" width="90" />
        <el-table-column label="申请类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.requestType === 'DELETE' ? 'danger' : 'warning'">{{ row.requestType === 'DELETE' ? '删除' : '修改' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请人" prop="requestedBy" width="110" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="驳回原因" prop="rejectReason" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showContent(row)">查看内容</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="approve(row.id)">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="openReject(row.id)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="contentVisible" title="申请内容" width="680px">
      <template v-if="currentParsed">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="产品号">{{ currentParsed.productId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="客户名字">{{ currentParsed.customerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系方式">{{ currentParsed.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SN号">{{ currentParsed.snNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分光器">{{ currentParsed.splitter || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接入间">{{ currentParsed.accessRoom || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ currentParsed.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="设备列表">
            <el-tag v-for="(d, i) in parsedDevices" :key="i" style="margin:2px">{{ d.type || '设备' }}: {{ d.sn }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <pre v-else class="json-box">{{ currentContent }}</pre>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回申请" width="420px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入驳回原因" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="reject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAdminWarehouseRequests, reviewAdminWarehouseRequest, type WarehouseChangeRequest } from '../../api/warehouse'
import { formatDateTime } from '../../utils/datetime'

const loading = ref(false)
const requests = ref<WarehouseChangeRequest[]>([])
const contentVisible = ref(false)
const currentContent = ref('')
const currentParsed = ref<any | null>(null)
const parsedDevices = ref<Array<{ type: string; sn: string }>>([])
const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectId = ref<number | null>(null)

function statusLabel(status: string) {
  return status === 'PENDING' ? '待审核' : status === 'APPROVED' ? '已通过' : '已驳回'
}

function statusType(status: string) {
  return status === 'PENDING' ? 'warning' : status === 'APPROVED' ? 'success' : 'danger'
}

function showContent(row: WarehouseChangeRequest) {
  currentContent.value = row.requestContent || '(无内容)'
  try {
    currentParsed.value = row.requestContent ? JSON.parse(row.requestContent) : null
    parsedDevices.value = currentParsed.value?.devices ? JSON.parse(currentParsed.value.devices) : []
  } catch {
    currentParsed.value = null
    parsedDevices.value = []
  }
  contentVisible.value = true
}

async function fetchRequests() {
  loading.value = true
  try {
    const { data: res } = await listAdminWarehouseRequests()
    requests.value = res.data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function approve(id: number) {
  try {
    await reviewAdminWarehouseRequest(id, true)
    ElMessage.success('审核通过')
    fetchRequests()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

function openReject(id: number) {
  rejectId.value = id
  rejectReason.value = ''
  rejectVisible.value = true
}

async function reject() {
  if (!rejectId.value) return
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  try {
    await reviewAdminWarehouseRequest(rejectId.value, false, rejectReason.value.trim())
    ElMessage.success('已驳回')
    rejectVisible.value = false
    fetchRequests()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

onMounted(fetchRequests)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.json-box { background: #f7f8fa; padding: 12px; border-radius: 6px; white-space: pre-wrap; word-break: break-all; max-height: 420px; overflow: auto; }
</style>

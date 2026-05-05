<template>
  <div class="warehouse-search">
    <el-page-header @back="$router.push('/')">
      <template #content><span class="page-title">仓库管理</span></template>
    </el-page-header>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div class="card-header">
          <el-input v-model="keyword" class="search-input" placeholder="搜索产品号、串码、客户名..." clearable @input="debouncedFetch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <div style="display:flex;gap:8px">
            <el-button type="success" @click="quickDialogVisible = true">快速入库</el-button>
            <el-button type="primary" @click="showAdd">新增入库</el-button>
          </div>
        </div>
      </template>

      <el-table :data="items" v-loading="loading" stripe @row-click="handleRowClick">
        <el-table-column label="产品号" prop="productId" width="130" show-overflow-tooltip />
        <el-table-column v-if="!isMobile" label="设备数量" width="80" align="center">
          <template #default="{ row }">{{ getDeviceList(row.id).length }}</template>
        </el-table-column>
        <el-table-column label="设备列表" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-for="d in getDeviceList(row.id).slice(0, 3)" :key="d.sn" size="small" style="margin:1px">{{ d.type }}: {{ d.sn }}</el-tag>
            <span v-if="getDeviceList(row.id).length > 3" style="color:#999">+{{ getDeviceList(row.id).length - 3 }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isMobile" label="分光器" prop="splitter" width="150" show-overflow-tooltip />
        <el-table-column v-if="!isMobile" label="接入间" prop="accessRoom" min-width="180" show-overflow-tooltip />
        <el-table-column label="入库时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.receivedAt) }}</template>
        </el-table-column>
        <el-table-column v-if="!isMobile" width="320" fixed="right" label="操作">
          <template #default="{ row }">
            <el-button size="small" @click.stop="showEditRequest(row)">申请修改</el-button>
            <el-popconfirm title="确认提交删除申请？" @confirm="handleDeleteRequest(row.id)">
              <template #reference>
                <el-button size="small" type="danger" @click.stop>申请删除</el-button>
              </template>
            </el-popconfirm>
            <el-button size="small" type="warning" @click.stop="openOutboundDialog(row)">设备出库</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchItems" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '申请修改入库信息' : '新增入库'" width="700px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="产品号" required><el-input v-model="form.productId" placeholder="020开头" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户名字"><el-input v-model="form.customerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系方式"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="SN号"><el-input v-model="form.snNumber" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="地址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="分光器"><el-input v-model="form.splitter" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="接入间"><el-input v-model="form.accessRoom" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">设备列表</el-divider>
        <div v-for="(d, i) in formDevices" :key="i" style="display:flex;gap:8px;margin-bottom:8px;align-items:center">
          <el-select v-model="d.type" placeholder="设备类型" style="width:140px" filterable allow-create>
            <el-option v-for="dt in deviceTypes" :key="dt.id" :label="dt.name" :value="dt.name" />
          </el-select>
          <el-input v-model="d.sn" placeholder="串码" style="flex:1" />
          <el-button :type="scanningIndex === i ? 'danger' : 'success'" :icon="Camera" circle size="small" @click="toggleScan(i)" />
          <el-button type="danger" :icon="Delete" circle size="small" @click="formDevices.splice(i, 1)" />
        </div>
        <el-button type="primary" plain size="small" @click="formDevices.push({ type: '', sn: '' })">+ 添加设备</el-button>
        <div v-if="scanningIndex !== null" class="scanner-preview">
          <video ref="videoRef" class="scanner-video" autoplay playsinline muted></video>
          <div v-if="scanError" class="scan-error">{{ scanError }}</div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ editId ? '提交修改申请' : '确认入库' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quickDialogVisible" title="快速入库" :width="quickDialogWidth" destroy-on-close @close="resetQuickForm">
      <el-steps :active="quickStep" finish-status="success" align-center>
        <el-step title="文本解析" />
        <el-step title="录入串码" />
        <el-step title="二次确认" />
      </el-steps>

      <div v-if="quickStep === 0" style="margin-top:18px">
        <el-form label-width="90px">
          <el-form-item label="短信文本">
            <el-input v-model="quickText" type="textarea" :rows="8" placeholder="粘贴工单短信文本" />
          </el-form-item>
        </el-form>
      </div>

      <div v-if="quickStep === 1" style="margin-top:18px">
        <el-form :model="quickForm" label-width="90px">
          <el-row :gutter="12">
            <el-col :span="12"><el-form-item label="产品号"><el-input v-model="quickForm.productId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="客户名字"><el-input v-model="quickForm.customerName" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="联系方式"><el-input v-model="quickForm.phone" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="SN号"><el-input v-model="quickForm.snNumber" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="分光器"><el-input v-model="quickForm.splitter" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="接入间"><el-input v-model="quickForm.accessRoom" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="地址"><el-input v-model="quickForm.address" /></el-form-item></el-col>
          </el-row>
          <el-divider content-position="left">设备列表</el-divider>
          <div v-for="(d, i) in quickDevices" :key="`q-${i}`" style="display:flex;gap:8px;margin-bottom:8px;align-items:center">
            <el-select v-model="d.type" placeholder="设备类型" style="width:140px" filterable allow-create>
              <el-option v-for="dt in deviceTypes" :key="dt.id" :label="dt.name" :value="dt.name" />
            </el-select>
            <el-input v-model="d.sn" placeholder="串码" style="flex:1" />
            <el-button :type="quickScanningIndex === i ? 'danger' : 'success'" :icon="Camera" circle size="small" @click="toggleQuickScan(i)" />
            <el-button type="danger" :icon="Delete" circle size="small" @click="quickDevices.splice(i, 1)" />
          </div>
          <el-button type="primary" plain size="small" @click="quickDevices.push({ type: '', sn: '' })">+ 添加设备</el-button>
          <div v-if="quickScanningIndex !== null" class="scanner-preview">
            <video ref="videoRef" class="scanner-video" autoplay playsinline muted></video>
            <div v-if="quickScanError" class="scan-error">{{ quickScanError }}</div>
          </div>
        </el-form>
      </div>

      <div v-if="quickStep === 2" style="margin-top:18px">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="产品号">{{ quickForm.productId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="客户名字">{{ quickForm.customerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系方式">{{ quickForm.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SN号">{{ quickForm.snNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分光器">{{ quickForm.splitter || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接入间">{{ quickForm.accessRoom || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ quickForm.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="设备列表">
            <el-tag v-for="(d, i) in quickDevices.filter(x => x.sn.trim())" :key="`c-${i}`" style="margin:2px">{{ d.type || '未命名设备' }}: {{ d.sn }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <el-button @click="quickDialogVisible = false">取消</el-button>
        <el-button v-if="quickStep > 0" @click="quickStep--">上一步</el-button>
        <el-button v-if="quickStep < 2" type="primary" @click="nextQuickStep">下一步</el-button>
        <el-button v-else type="success" :loading="quickSubmitting" @click="submitQuickInbound">确认入库</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="outboundVisible" title="选择出库设备" width="520px">
      <el-radio-group v-model="selectedOutboundSn" style="display:flex;flex-direction:column;gap:8px">
        <el-radio v-for="d in outboundCandidates" :key="d.sn" :label="d.sn" :disabled="!!d.outbound">
          {{ d.type || '设备' }} - {{ d.sn }}
          <el-tag v-if="d.outbound" size="small" type="info" style="margin-left:6px">已出库</el-tag>
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="outboundVisible = false">取消</el-button>
        <el-button type="warning" :loading="outboundSubmitting" @click="confirmOutbound">确认出库</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="mobileActionVisible"
      direction="btt"
      size="52%"
      :with-header="false"
      class="mobile-action-drawer"
    >
      <div v-if="mobileActionRow" class="mobile-action-panel">
        <div class="drawer-title">工单操作</div>
        <el-button type="primary" size="large" @click="goDetail(mobileActionRow)">查看详情</el-button>
        <el-button size="large" @click="showEditRequest(mobileActionRow)">申请修改</el-button>
        <el-button type="warning" size="large" @click="openOutboundDialog(mobileActionRow)">设备出库</el-button>
        <el-popconfirm title="确认提交删除申请？" @confirm="handleDeleteRequest(mobileActionRow.id)">
          <template #reference>
            <el-button type="danger" size="large">申请删除</el-button>
          </template>
        </el-popconfirm>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete, Camera, Search } from '@element-plus/icons-vue'
import {
  searchWarehouse,
  createWarehouseItem,
  outboundWarehouseItem,
  createWarehouseChangeRequest,
  type WarehouseItem,
  type DeviceInfo,
} from '../../api/warehouse'
import { useScanner } from '../../composables/useScanner'
import { useIsMobile } from '../../composables/useIsMobile'
import { formatDateTime } from '../../utils/datetime'

interface DeviceType { id: number; name: string }

const router = useRouter()
const isMobile = useIsMobile()
const items = ref<WarehouseItem[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

const deviceTypes = ref<DeviceType[]>([])
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const form = ref<Partial<WarehouseItem>>({})
const formDevices = ref<DeviceInfo[]>([{ type: '', sn: '' }])
const submitting = ref(false)

const quickDialogVisible = ref(false)
const quickStep = ref(0)
const quickText = ref('')
const quickForm = ref<Partial<WarehouseItem>>({})
const quickDevices = ref<DeviceInfo[]>([{ type: '', sn: '' }])
const quickSubmitting = ref(false)
const outboundVisible = ref(false)
const outboundSubmitting = ref(false)
const outboundItemId = ref<number | null>(null)
const selectedOutboundSn = ref('')
const outboundCandidates = ref<DeviceInfo[]>([])
const mobileActionVisible = ref(false)
const mobileActionRow = ref<WarehouseItem | null>(null)

const scanningIndex = ref<number | null>(null)
const quickScanningIndex = ref<number | null>(null)
const scanError = ref('')
const quickScanError = ref('')
const { videoRef, startScan, stopScan } = useScanner()
void videoRef

let timer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { page.value = 0; fetchItems() }, 300)
}

function parseDevices(json: string): DeviceInfo[] {
  try { return JSON.parse(json || '[]') } catch { return [] }
}

const devicesById = computed(() => {
  const map = new Map<number, DeviceInfo[]>()
  for (const item of items.value) {
    map.set(item.id, parseDevices(item.devices))
  }
  return map
})

function getDeviceList(itemId: number): DeviceInfo[] {
  return devicesById.value.get(itemId) || []
}

async function fetchItems() {
  loading.value = true
  try {
    const { data: res } = await searchWarehouse(keyword.value, page.value, size.value)
    items.value = res.data.content
    total.value = res.data.totalElements
    if (items.value.length === 0 && page.value > 0) { page.value--; fetchItems(); return }
  } finally { loading.value = false }
}

function handleRowClick(row: WarehouseItem) {
  if (isMobile.value) {
    mobileActionRow.value = row
    mobileActionVisible.value = true
    return
  }
  goDetail(row)
}

function goDetail(row: WarehouseItem) {
  mobileActionVisible.value = false
  router.push(`/warehouse/${row.id}`)
}

function showAdd() {
  editId.value = null
  form.value = { productId: '', customerName: '', phone: '', address: '', splitter: '', snNumber: '', accessRoom: '' }
  formDevices.value = [{ type: '', sn: '' }]
  scanningIndex.value = null
  dialogVisible.value = true
}

function showEditRequest(row: WarehouseItem) {
  mobileActionVisible.value = false
  editId.value = row.id
  form.value = { ...row }
  formDevices.value = parseDevices(row.devices)
  if (formDevices.value.length === 0) formDevices.value = [{ type: '', sn: '' }]
  scanningIndex.value = null
  dialogVisible.value = true
}

async function toggleScan(index: number) {
  if (scanningIndex.value === index) {
    stopScan()
    scanningIndex.value = null
    scanError.value = ''
    return
  }
  stopScan()
  scanningIndex.value = index
  quickScanningIndex.value = null
  scanError.value = ''
  try {
    await nextTick()
    await startScan((code: string) => {
      formDevices.value[index].sn = code
      scanningIndex.value = null
      scanError.value = ''
      ElMessage.success('扫码成功: ' + code)
    })
  } catch (e: any) {
    scanError.value = e.message || '扫码失败'
    scanningIndex.value = null
  }
}

async function toggleQuickScan(index: number) {
  if (quickScanningIndex.value === index) {
    stopScan()
    quickScanningIndex.value = null
    quickScanError.value = ''
    return
  }
  stopScan()
  quickScanningIndex.value = index
  scanningIndex.value = null
  quickScanError.value = ''
  try {
    await nextTick()
    await startScan((code: string) => {
      quickDevices.value[index].sn = code
      quickScanningIndex.value = null
      quickScanError.value = ''
      ElMessage.success('扫码成功: ' + code)
    })
  } catch (e: any) {
    quickScanError.value = e.message || '扫码失败'
    quickScanningIndex.value = null
  }
}

function closeDialog() {
  stopScan()
  scanningIndex.value = null
  quickScanningIndex.value = null
  dialogVisible.value = false
}

async function fetchDeviceTypes() {
  const res = await fetch('/api/device-types?sortBy=id&sortOrder=asc')
  const data = await res.json()
  deviceTypes.value = data.data
}

async function handleSubmit() {
  if (!form.value.productId) { ElMessage.warning('请输入产品号'); return }
  const validDevices = formDevices.value.filter(d => d.sn.trim())
  submitting.value = true
  try {
    const payload = { ...form.value, devices: JSON.stringify(validDevices) }
    if (editId.value) {
      await createWarehouseChangeRequest({
        warehouseItemId: editId.value,
        requestType: 'UPDATE',
        requestContent: JSON.stringify(payload),
        requestedBy: 'front-user',
      })
      ElMessage.success('修改申请已提交，等待后台审核')
    } else {
      await createWarehouseItem(payload)
      ElMessage.success('入库成功')
    }
    closeDialog()
    fetchItems()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally { submitting.value = false }
}

async function handleDeleteRequest(id: number) {
  try {
    await createWarehouseChangeRequest({ warehouseItemId: id, requestType: 'DELETE', requestedBy: 'front-user' })
    ElMessage.success('删除申请已提交，等待后台审核')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  }
}

async function handleOutbound(id: number) {
  try {
    if (!selectedOutboundSn.value) {
      ElMessage.warning('请选择设备')
      return
    }
    outboundSubmitting.value = true
    await outboundWarehouseItem(id, selectedOutboundSn.value)
    ElMessage.success('出库成功')
    outboundVisible.value = false
    selectedOutboundSn.value = ''
    fetchItems()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '出库失败')
  } finally {
    outboundSubmitting.value = false
  }
}

function openOutboundDialog(row: WarehouseItem) {
  outboundItemId.value = row.id
  outboundCandidates.value = parseDevices(row.devices)
  selectedOutboundSn.value = ''
  outboundVisible.value = true
  mobileActionVisible.value = false
}

function confirmOutbound() {
  if (!outboundItemId.value) return
  handleOutbound(outboundItemId.value)
}

function parseQuickText() {
  const text = quickText.value
  const pick = (reg: RegExp) => (text.match(reg)?.[1] || '').trim()
  quickForm.value.productId = pick(/业务号码为[:：]\s*([^，。]+)/)
  quickForm.value.customerName = pick(/客户[:：]\s*([^，。]+)/)
  quickForm.value.phone = pick(/联系方式[:：]\s*([^，。]+)/)
  quickForm.value.splitter = pick(/分光器[:：]\s*([^，。]+)/)
  quickForm.value.accessRoom = pick(/接入间[:：]\s*([^，。]+)/)
  quickForm.value.address = pick(/装机地址[:：]\s*([^。]+)/)
  quickForm.value.snNumber = pick(/[O0]NU\s*SN号[:：]\s*([^，。]+)/)
}

function nextQuickStep() {
  if (quickStep.value === 0) {
    if (!quickText.value.trim()) { ElMessage.warning('请先粘贴文本'); return }
    parseQuickText()
    quickDevices.value = [{ type: '', sn: quickForm.value.snNumber || '' }]
  }
  if (quickStep.value === 1 && !quickForm.value.productId) {
    ElMessage.warning('产品号不能为空')
    return
  }
  quickStep.value++
}

async function submitQuickInbound() {
  quickSubmitting.value = true
  try {
    const validDevices = quickDevices.value.filter(d => d.sn.trim())
    await createWarehouseItem({ ...quickForm.value, devices: JSON.stringify(validDevices) })
    ElMessage.success('快速入库成功')
    quickDialogVisible.value = false
    fetchItems()
    resetQuickForm()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '快速入库失败')
  } finally {
    quickSubmitting.value = false
  }
}

function resetQuickForm() {
  stopScan()
  quickStep.value = 0
  quickText.value = ''
  quickForm.value = {}
  quickDevices.value = [{ type: '', sn: '' }]
  quickScanningIndex.value = null
  quickScanError.value = ''
}

const quickDialogWidth = computed(() => isMobile.value ? '96%' : '760px')

onMounted(() => {
  fetchItems()
  fetchDeviceTypes()
})

onUnmounted(() => {
  if (timer) clearTimeout(timer)
})
</script>

<style scoped>
.page-title { font-weight: 600; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.scanner-preview { margin-top: 12px; border-radius: 8px; overflow: hidden; border: 1px solid #eee; position: relative; }
.scanner-video { width: 100%; max-height: 240px; object-fit: cover; }
.scan-error { position: absolute; bottom: 8px; left: 50%; transform: translateX(-50%); background: rgba(0,0,0,.6); color: #fff; padding: 4px 12px; border-radius: 4px; font-size: 13px; }
@media (max-width: 768px) {
  .card-header { flex-direction: column; align-items: stretch; gap: 10px; }
  .search-input { width: 100% !important; }
  .pagination { justify-content: center; }
  :deep(.hide-mobile) { display: none !important; }
}

.mobile-action-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 2px 6px;
}

.drawer-title {
  text-align: center;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}

:deep(.mobile-action-drawer .el-drawer__body) {
  padding: 14px 14px calc(env(safe-area-inset-bottom, 0px) + 10px);
  border-top-left-radius: 14px;
  border-top-right-radius: 14px;
}
</style>

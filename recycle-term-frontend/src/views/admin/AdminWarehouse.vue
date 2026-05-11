<template>
  <div class="admin-warehouse">
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-header">
          <span>仓库设置</span>
          <el-button type="primary" size="small" @click="showWarehouseAdd">新增仓库</el-button>
        </div>
      </template>
      <el-table :data="warehouses" stripe size="small">
        <el-table-column label="名称" prop="name" min-width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
            <el-tag v-if="row.defaultWarehouse" type="warning" style="margin-left:4px">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showWarehouseEdit(row)">改名</el-button>
            <el-button v-if="!row.defaultWarehouse" size="small" :type="row.enabled ? 'warning' : 'success'" @click="toggleWarehouse(row)">{{ row.enabled ? '禁用' : '启用' }}</el-button>
            <el-popconfirm v-if="!row.defaultWarehouse" title="确认删除该仓库？仓库下有设备时会被拒绝。" @confirm="removeWarehouse(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-header">
          <div class="toolbar-left">
            <el-input v-model="keyword" placeholder="搜索产品号、串码、客户名..." clearable class="search-input" @input="debouncedFetch">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="selectedWarehouseId" placeholder="全部仓库" clearable class="warehouse-filter" @change="fetchItems">
              <el-option v-for="w in warehouses" :key="w.id" :label="`${w.name}${w.enabled ? '' : '（禁用）'}`" :value="w.id" />
            </el-select>
            <el-button :type="outboundOnly ? 'primary' : 'info'" plain @click="toggleOutboundOnly">
              {{ outboundOnly ? '查看全部库存' : '查看已出库' }}
            </el-button>
          </div>
          <div class="toolbar-actions">
            <el-button type="warning" :disabled="selectedDeviceIds.length === 0" @click="openBatchTransfer">批量转仓</el-button>
            <el-button type="primary" @click="showAdd">新增入库</el-button>
          </div>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column label="产品号" prop="productId" width="130" show-overflow-tooltip />
        <el-table-column label="设备数量" width="80" align="center">
          <template #default="{ row }">{{ row.devices.length }}</template>
        </el-table-column>
        <el-table-column label="设备列表" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-for="d in row.devices.slice(0, 3)" :key="d.id || d.sn" size="small" style="margin:1px">
              {{ d.type }}: {{ d.sn }} · {{ d.warehouseName || '-' }} · {{ d.outbound ? '已出库' : '在库' }}
            </el-tag>
            <span v-if="row.devices.length > 3" style="color:#999">+{{ row.devices.length - 3 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="客户" prop="customerName" width="90" />
        <el-table-column label="入库时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.receivedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="openTransfer(row)">转仓</el-button>
            <el-popconfirm title="确定删除？有设备时会被拒绝。" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchItems" />
      </div>
    </el-card>

    <el-dialog v-model="warehouseDialogVisible" :title="warehouseEditId ? '修改仓库名称' : '新增仓库'" width="420px">
      <el-form label-width="80px" @submit.prevent="submitWarehouse">
        <el-form-item label="仓库名称" required>
          <el-input v-model="warehouseForm.name" @keydown.enter.prevent.stop="submitWarehouse" />
        </el-form-item>
        <el-form-item v-if="!warehouseEditId" label="状态"><el-switch v-model="warehouseForm.enabled" active-text="启用" inactive-text="禁用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="warehouseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWarehouse">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑入库' : '新增入库'" width="720px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="所属仓库" required><el-select v-model="formWarehouseId" style="width:100%"><el-option v-for="w in enabledWarehouses" :key="w.id" :label="w.name" :value="w.id" /></el-select></el-form-item></el-col>
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
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferVisible" :title="batchTransferMode ? '批量设备转仓' : '设备转仓'" width="520px">
      <el-form label-width="90px">
        <el-form-item v-if="!batchTransferMode" label="设备"><el-select v-model="transferDeviceId" style="width:100%"><el-option v-for="d in transferCandidates" :key="d.id" :label="`${d.type || '设备'} - ${d.sn}（${d.warehouseName || '-'}）`" :value="d.id" /></el-select></el-form-item>
        <el-form-item v-else label="设备数量"><span>{{ selectedDeviceIds.length }} 台</span></el-form-item>
        <el-form-item label="目标仓库"><el-select v-model="transferTargetWarehouseId" style="width:100%"><el-option v-for="w in enabledWarehouses" :key="w.id" :label="w.name" :value="w.id" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="transferRemark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="transferSubmitting" @click="confirmTransfer">确认转仓</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Camera, Search } from '@element-plus/icons-vue'
import { useScanner } from '../../composables/useScanner'
import type { WarehouseItem, DeviceInfo, Warehouse } from '../../api/warehouse'
import {
  searchWarehouse,
  listAdminWarehouses,
  createAdminWarehouse,
  updateAdminWarehouse,
  setAdminWarehouseEnabled,
  deleteAdminWarehouse,
  createWarehouseItem,
  updateWarehouseItem,
  deleteWarehouseItem,
  batchTransferWarehouseDevices,
} from '../../api/warehouse'
import { formatDateTime } from '../../utils/datetime'

interface DeviceType { id: number; name: string }

const items = ref<WarehouseItem[]>([])
const warehouses = ref<Warehouse[]>([])
const selectedWarehouseId = ref<number | undefined>()
const selectedDeviceIds = ref<number[]>([])
const outboundOnly = ref(false)
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

const deviceTypes = ref<DeviceType[]>([])
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const form = ref<Partial<WarehouseItem>>({})
const formWarehouseId = ref<number | undefined>()
const formDevices = ref<DeviceInfo[]>([{ type: '', sn: '' }])
const submitting = ref(false)

const warehouseDialogVisible = ref(false)
const warehouseEditId = ref<number | null>(null)
const warehouseForm = ref<{ name: string; enabled: boolean }>({ name: '', enabled: true })

const transferVisible = ref(false)
const batchTransferMode = ref(false)
const transferCandidates = ref<DeviceInfo[]>([])
const transferDeviceId = ref<number | undefined>()
const transferTargetWarehouseId = ref<number | undefined>()
const transferRemark = ref('')
const transferSubmitting = ref(false)

const scanningIndex = ref<number | null>(null)
const scanError = ref('')
const scanner = useScanner()
const { videoRef, startScan, stopScan } = scanner
void videoRef

const enabledWarehouses = computed(() => warehouses.value.filter(w => w.enabled))
function defaultWarehouseId() { return warehouses.value.find(w => w.defaultWarehouse)?.id || enabledWarehouses.value[0]?.id }

let timer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { page.value = 0; fetchItems() }, 300)
}

async function fetchWarehouses() {
  const { data: res } = await listAdminWarehouses(true)
  warehouses.value = res.data
  formWarehouseId.value = defaultWarehouseId()
  transferTargetWarehouseId.value = defaultWarehouseId()
}

async function fetchDeviceTypes() {
  const res = await fetch('/api/device-types?sortBy=id&sortOrder=asc')
  const data = await res.json()
  deviceTypes.value = data.data
}

async function fetchItems() {
  loading.value = true
  try {
    const { data: res } = await searchWarehouse(keyword.value, page.value, size.value, selectedWarehouseId.value, true, outboundOnly.value)
    items.value = res.data.content
    total.value = res.data.totalElements
    selectedDeviceIds.value = []
    if (items.value.length === 0 && page.value > 0) { page.value--; fetchItems(); return }
  } finally { loading.value = false }
}

function toggleOutboundOnly() {
  outboundOnly.value = !outboundOnly.value
  page.value = 0
  selectedDeviceIds.value = []
  fetchItems()
}

function showWarehouseAdd() {
  warehouseEditId.value = null
  warehouseForm.value = { name: '', enabled: true }
  warehouseDialogVisible.value = true
}

function showWarehouseEdit(row: Warehouse) {
  warehouseEditId.value = row.id
  warehouseForm.value = { name: row.name, enabled: row.enabled }
  warehouseDialogVisible.value = true
}

async function submitWarehouse() {
  if (!warehouseForm.value.name.trim()) { ElMessage.warning('请输入仓库名称'); return }
  if (warehouseEditId.value) await updateAdminWarehouse(warehouseEditId.value, warehouseForm.value)
  else await createAdminWarehouse(warehouseForm.value)
  ElMessage.success('操作成功')
  warehouseDialogVisible.value = false
  fetchWarehouses()
  fetchItems()
}

async function toggleWarehouse(row: Warehouse) {
  await setAdminWarehouseEnabled(row.id, !row.enabled)
  ElMessage.success(row.enabled ? '已禁用' : '已启用')
  fetchWarehouses()
  fetchItems()
}

async function removeWarehouse(id: number) {
  try {
    await deleteAdminWarehouse(id)
    ElMessage.success('删除成功')
    fetchWarehouses()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '删除失败') }
}

function showAdd() {
  editId.value = null
  form.value = { productId: '', customerName: '', phone: '', address: '', splitter: '', snNumber: '', accessRoom: '' }
  formWarehouseId.value = defaultWarehouseId()
  formDevices.value = [{ type: '', sn: '' }]
  scanningIndex.value = null
  dialogVisible.value = true
}

function showEdit(row: WarehouseItem) {
  editId.value = row.id
  form.value = { ...row }
  formWarehouseId.value = row.devices.find(d => !d.outbound)?.warehouseId || row.devices[0]?.warehouseId || defaultWarehouseId()
  formDevices.value = row.devices.map(d => ({ ...d }))
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
  scanningIndex.value = index
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

function closeDialog() {
  stopScan()
  scanningIndex.value = null
  dialogVisible.value = false
}

async function handleSubmit() {
  if (!form.value.productId) { ElMessage.warning('请输入产品号'); return }
  if (!formWarehouseId.value) { ElMessage.warning('请选择仓库'); return }
  const validDevices = formDevices.value.filter(d => d.sn.trim()).map(d => ({ ...d, warehouseId: d.warehouseId || formWarehouseId.value }))
  submitting.value = true
  try {
    const payload = { ...form.value, devices: validDevices }
    if (editId.value) await updateWarehouseItem(editId.value, payload)
    else await createWarehouseItem(payload)
    ElMessage.success(editId.value ? '修改成功' : '入库成功')
    closeDialog()
    fetchItems()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '操作失败') }
  finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try {
    await deleteWarehouseItem(id)
    ElMessage.success('删除成功')
    fetchItems()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '删除失败') }
}

function handleSelectionChange(rows: WarehouseItem[]) {
  selectedDeviceIds.value = rows.flatMap(row => row.devices.filter(d => !d.outbound && d.id).map(d => d.id as number))
}

function openTransfer(row: WarehouseItem) {
  batchTransferMode.value = false
  transferCandidates.value = row.devices.filter(d => !d.outbound && d.id)
  transferDeviceId.value = transferCandidates.value[0]?.id
  transferTargetWarehouseId.value = defaultWarehouseId()
  transferRemark.value = ''
  transferVisible.value = true
}

function openBatchTransfer() {
  batchTransferMode.value = true
  transferDeviceId.value = undefined
  transferTargetWarehouseId.value = defaultWarehouseId()
  transferRemark.value = ''
  transferVisible.value = true
}

async function confirmTransfer() {
  if (!transferTargetWarehouseId.value) { ElMessage.warning('请选择目标仓库'); return }
  const ids = batchTransferMode.value ? selectedDeviceIds.value : (transferDeviceId.value ? [transferDeviceId.value] : [])
  if (ids.length === 0) { ElMessage.warning('请选择设备'); return }
  transferSubmitting.value = true
  try {
    await batchTransferWarehouseDevices(ids, transferTargetWarehouseId.value, transferRemark.value)
    ElMessage.success('转仓成功')
    transferVisible.value = false
    fetchItems()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '转仓失败') }
  finally { transferSubmitting.value = false }
}

onMounted(() => { fetchWarehouses(); fetchItems(); fetchDeviceTypes() })
</script>

<style scoped>
.section-card { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-left { display: flex; align-items: center; gap: 8px; min-width: 0; }
.toolbar-actions { display: flex; gap: 8px; }
.search-input { width: 300px; }
.warehouse-filter { width: 180px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.scanner-preview { margin-top: 12px; border-radius: 8px; overflow: hidden; border: 1px solid #eee; position: relative; }
.scanner-video { width: 100%; max-height: 240px; object-fit: cover; }
.scan-error { position: absolute; bottom: 8px; left: 50%; transform: translateX(-50%); background: rgba(0,0,0,.6); color: #fff; padding: 4px 12px; border-radius: 4px; font-size: 13px; }
@media (max-width: 768px) {
  .card-header { flex-direction: column; align-items: stretch; }
  .toolbar-left, .toolbar-actions { flex-direction: column; align-items: stretch; }
  .search-input, .warehouse-filter { width: 100% !important; }
  .pagination { justify-content: center; }
}
</style>

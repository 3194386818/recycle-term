<template>
  <div class="admin-warehouse">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <el-input v-model="keyword" placeholder="搜索产品号、串码、客户名..." clearable style="width:300px" @input="debouncedFetch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="showAdd">新增入库</el-button>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="产品号" prop="productId" width="130" show-overflow-tooltip />
        <el-table-column label="设备数量" width="80" align="center">
          <template #default="{ row }">{{ parseDevices(row.devices).length }}</template>
        </el-table-column>
        <el-table-column label="设备列表" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-for="d in parseDevices(row.devices).slice(0, 3)" :key="d.sn" size="small" style="margin:1px">{{ d.type }}: {{ d.sn }}</el-tag>
            <span v-if="parseDevices(row.devices).length > 3" style="color:#999">+{{ parseDevices(row.devices).length - 3 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="客户" prop="customerName" width="90" />
        <el-table-column label="入库时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.receivedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchItems" />
      </div>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑入库' : '新增入库'" width="700px" destroy-on-close>
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
        <div style="display:flex;gap:12px;align-items:center">
          <el-button type="primary" plain size="small" @click="formDevices.push({ type: '', sn: '' })">+ 添加设备</el-button>
        </div>
        <!-- Scanner preview -->
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Camera } from '@element-plus/icons-vue'
import { useScanner } from '../../composables/useScanner'
import type { WarehouseItem, DeviceInfo } from '../../api/warehouse'
import { formatDateTime } from '../../utils/datetime'

interface DeviceType { id: number; name: string }

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

const scanningIndex = ref<number | null>(null)
const scanError = ref('')
const scanner = useScanner()
const { videoRef, startScan, stopScan } = scanner
void videoRef

let timer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { page.value = 0; fetchItems() }, 300)
}

function parseDevices(json: string): DeviceInfo[] {
  try { return JSON.parse(json || '[]') } catch { return [] }
}

async function fetchDeviceTypes() {
  const res = await fetch('/api/device-types?sortBy=id&sortOrder=asc')
  const data = await res.json()
  deviceTypes.value = data.data
}

async function fetchItems() {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: String(page.value), size: String(size.value) })
    if (keyword.value) params.set('keyword', keyword.value)
    const res = await fetch(`/api/warehouse?${params}`)
    const data = await res.json()
    items.value = data.data.content
    total.value = data.data.totalElements
    if (items.value.length === 0 && page.value > 0) { page.value--; fetchItems(); return }
  } finally { loading.value = false }
}

function showAdd() {
  editId.value = null
  form.value = { productId: '', customerName: '', phone: '', address: '', splitter: '', snNumber: '', accessRoom: '' }
  formDevices.value = [{ type: '', sn: '' }]
  scanningIndex.value = null
  dialogVisible.value = true
}

function showEdit(row: WarehouseItem) {
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
  const validDevices = formDevices.value.filter(d => d.sn.trim())
  submitting.value = true
  try {
    const payload = { ...form.value, devices: JSON.stringify(validDevices) }
    const url = editId.value ? `/api/warehouse/${editId.value}` : '/api/warehouse'
    const method = editId.value ? 'PUT' : 'POST'
    const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
    const data = await res.json()
    if (data.code === 200) {
      ElMessage.success(editId.value ? '修改成功' : '入库成功')
      closeDialog()
      fetchItems()
    } else {
      ElMessage.error(data.message)
    }
  } catch { ElMessage.error('操作失败') }
  finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try {
    const res = await fetch(`/api/warehouse/${id}`, { method: 'DELETE' })
    const data = await res.json()
    if (data.code === 200) { ElMessage.success('删除成功'); fetchItems() }
    else ElMessage.error(data.message)
  } catch { ElMessage.error('删除失败') }
}

onMounted(() => { fetchItems(); fetchDeviceTypes() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.scanner-preview { margin-top: 12px; border-radius: 8px; overflow: hidden; border: 1px solid #eee; position: relative; }
.scanner-video { width: 100%; max-height: 240px; object-fit: cover; }
.scan-error { position: absolute; bottom: 8px; left: 50%; transform: translateX(-50%); background: rgba(0,0,0,.6); color: #fff; padding: 4px 12px; border-radius: 4px; font-size: 13px; }
</style>

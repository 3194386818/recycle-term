<template>
  <div class="warehouse-search">
    <el-page-header @back="$router.push('/')">
      <template #content><span class="page-title">仓库管理</span></template>
    </el-page-header>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div class="card-header">
          <el-input v-model="keyword" placeholder="搜索产品号、串码、客户名..." clearable style="width:300px" @input="debouncedFetch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="showAdd">新增入库</el-button>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" stripe @row-click="goDetail">
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
        <el-table-column label="入库时间" prop="receivedAt" width="160" class-name="hide-mobile" header-class-name="hide-mobile" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="showEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" @click.stop>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchItems" />
      </div>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑入库' : '新增入库'" width="650px" destroy-on-close>
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
          <el-input v-model="d.type" placeholder="设备类型" style="width:150px" />
          <el-input v-model="d.sn" placeholder="串码" style="flex:1" />
          <el-button type="danger" :icon="Delete" circle size="small" @click="formDevices.splice(i, 1)" />
        </div>
        <el-button type="primary" plain size="small" @click="formDevices.push({ type: '', sn: '' })">+ 添加设备</el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { searchWarehouse, createWarehouseItem, updateWarehouseItem, deleteWarehouseItem } from '../../api/warehouse'
import type { WarehouseItem, DeviceInfo } from '../../api/warehouse'

const router = useRouter()
const items = ref<WarehouseItem[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const form = ref<Partial<WarehouseItem>>({})
const formDevices = ref<DeviceInfo[]>([{ type: '', sn: '' }])
const submitting = ref(false)

let timer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { page.value = 0; fetchItems() }, 300)
}

function parseDevices(json: string): DeviceInfo[] {
  try { return JSON.parse(json || '[]') } catch { return [] }
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

function goDetail(row: WarehouseItem) {
  router.push(`/warehouse/${row.id}`)
}

function showAdd() {
  editId.value = null
  form.value = { productId: '', customerName: '', phone: '', address: '', splitter: '', snNumber: '', accessRoom: '' }
  formDevices.value = [{ type: '', sn: '' }]
  dialogVisible.value = true
}

function showEdit(row: WarehouseItem) {
  editId.value = row.id
  form.value = { ...row }
  formDevices.value = parseDevices(row.devices)
  if (formDevices.value.length === 0) formDevices.value = [{ type: '', sn: '' }]
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.productId) { ElMessage.warning('请输入产品号'); return }
  const validDevices = formDevices.value.filter(d => d.sn.trim())
  submitting.value = true
  try {
    const payload = { ...form.value, devices: JSON.stringify(validDevices) }
    if (editId.value) {
      await updateWarehouseItem(editId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createWarehouseItem(payload)
      ElMessage.success('入库成功')
    }
    dialogVisible.value = false
    fetchItems()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try {
    await deleteWarehouseItem(id)
    ElMessage.success('删除成功')
    fetchItems()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(fetchItems)
</script>

<style scoped>
.page-title { font-weight: 600; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
:deep(.hide-mobile) { display: none !important; }
</style>

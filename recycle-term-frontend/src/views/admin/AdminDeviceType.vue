<template>
  <div class="device-type-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>设备类型管理</span>
          <div style="display:flex;gap:8px">
            <el-input v-model="newName" placeholder="输入新类型名称" style="width:200px" @keydown.enter="handleAdd" />
            <el-button type="primary" :loading="adding" @click="handleAdd">添加</el-button>
          </div>
        </div>
      </template>
      <el-table :data="types" v-loading="loading" stripe @sort-change="handleSortChange">
        <el-table-column label="ID" prop="id" width="80" sortable="custom" />
        <el-table-column label="类型名称" prop="name" sortable="custom" />
        <el-table-column label="创建时间" prop="createdAt" width="180" sortable="custom" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

interface DeviceType { id: number; name: string; createdAt: string }

const types = ref<DeviceType[]>([])
const loading = ref(false)
const newName = ref('')
const adding = ref(false)
const sortParams = ref({
  sortBy: 'id',
  sortOrder: 'desc'
})

async function fetchTypes() {
  loading.value = true
  try {
    const url = `/api/device-types?sortBy=${sortParams.value.sortBy}&sortOrder=${sortParams.value.sortOrder}`
    const res = await fetch(url)
    const data = await res.json()
    types.value = data.data
  } finally { loading.value = false }
}

function handleSortChange({ column, prop, order }) {
  if (!prop || !order) {
    // 如果取消排序，则恢复默认排序
    sortParams.value.sortBy = 'id'
    sortParams.value.sortOrder = 'desc'
  } else {
    sortParams.value.sortBy = prop
    sortParams.value.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  }
  fetchTypes()
}

async function handleAdd() {
  if (!newName.value.trim()) { ElMessage.warning('请输入类型名称'); return }
  adding.value = true
  try {
    const res = await fetch('/api/device-types', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: newName.value.trim() })
    })
    const data = await res.json()
    if (data.code === 200) {
      ElMessage.success('添加成功')
      newName.value = ''
      fetchTypes()
    } else {
      ElMessage.error(data.message)
    }
  } catch { ElMessage.error('添加失败') }
  finally { adding.value = false }
}

async function handleDelete(id: number) {
  try {
    const res = await fetch(`/api/device-types/${id}`, { method: 'DELETE' })
    const data = await res.json()
    if (data.code === 200) {
      ElMessage.success('删除成功')
      fetchTypes()
    } else {
      ElMessage.error(data.message)
    }
  } catch { ElMessage.error('删除失败') }
}

onMounted(fetchTypes)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>

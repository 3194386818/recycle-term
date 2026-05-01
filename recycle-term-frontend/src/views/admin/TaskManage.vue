<template>
  <div class="task-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <el-input v-model="keyword" placeholder="搜索号码、姓名、地址..." clearable style="width:300px" @input="debouncedFetch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="showAdd">添加任务</el-button>
        </div>
      </template>
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="用户" prop="userName" width="100" />
        <el-table-column label="号码" prop="phoneNumber" width="130" />
        <el-table-column label="地址" prop="userAddress" show-overflow-tooltip min-width="160" />
        <el-table-column label="区域" prop="area" width="80" />
        <el-table-column label="应回收" prop="expectedCount" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.completed ? 'success' : 'warning'" size="small">{{ row.completed ? '已完成' : '待回收' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="showEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchTasks" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑任务' : '添加任务'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="用户名称"><el-input v-model="form.userName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="用户号码"><el-input v-model="form.phoneNumber" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="用户地址"><el-input v-model="form.userAddress" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="区域"><el-input v-model="form.area" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="工程师"><el-input v-model="form.engineerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="工程师电话"><el-input v-model="form.engineerPhone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="分类"><el-input v-model="form.category" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="应回收数量"><el-input-number v-model="form.expectedCount" :min="0" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="FTTR数量"><el-input-number v-model="form.fttrCount" :min="0" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="接入间"><el-input v-model="form.accessRoom" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="发展部门"><el-input v-model="form.devDept" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="发展员工"><el-input v-model="form.devPerson" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="终端串码"><el-input v-model="form.terminals" type="textarea" :rows="3" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
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
import { ElMessage } from 'element-plus'
import { getAdminTasks, createTask, updateAdminTask, deleteAdminTask } from '../../api/admin'
import type { RecycleTask } from '../../types'

const tasks = ref<RecycleTask[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const submitting = ref(false)
const form = ref<Partial<RecycleTask>>({})

let debounceTimer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { page.value = 0; fetchTasks() }, 300)
}

async function fetchTasks() {
  loading.value = true
  try {
    const { data: res } = await getAdminTasks({ keyword: keyword.value, page: page.value, size: size.value })
    tasks.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

function showAdd() {
  editId.value = null
  form.value = {}
  dialogVisible.value = true
}

function showEdit(row: RecycleTask) {
  editId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (editId.value) {
      await updateAdminTask(editId.value, form.value)
      ElMessage.success('修改成功')
    } else {
      await createTask(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchTasks()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  await deleteAdminTask(id)
  ElMessage.success('删除成功')
  fetchTasks()
}

onMounted(fetchTasks)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

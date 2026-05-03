<template>
  <div class="engineer-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>工程师账号管理</span>
          <el-button type="primary" @click="showAdd">添加账号</el-button>
        </div>
      </template>
      <el-table :data="engineers" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="姓名" prop="name" />
        <el-table-column label="手机号" prop="phone" width="140" />
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-popconfirm title="确定重置密码为 admin123？" @confirm="handleResetPwd(row.id)">
              <template #reference>
                <el-button size="small">重置密码</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchEngineers" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="添加工程师" width="400px" destroy-on-close>
      <el-form>
        <el-form-item label="手机号" required>
          <el-input v-model="formPhone" placeholder="工程师登录手机号" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="formName" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getEngineers, createEngineer, deleteEngineer, resetEngineerPassword } from '../../api/admin'

interface Engineer { id: number; phone: string; name: string; createdAt: string }

const engineers = ref<Engineer[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const formPhone = ref('')
const formName = ref('')
const submitting = ref(false)

async function fetchEngineers() {
  loading.value = true
  try {
    const { data: res } = await getEngineers({ page: page.value, size: size.value })
    engineers.value = res.data.content
    total.value = res.data.totalElements
  } finally { loading.value = false }
}

function showAdd() {
  formPhone.value = ''
  formName.value = ''
  dialogVisible.value = true
}

async function handleAdd() {
  if (!formPhone.value) { ElMessage.warning('请输入手机号'); return }
  submitting.value = true
  try {
    await createEngineer(formPhone.value, formName.value)
    ElMessage.success('创建成功，默认密码 admin123')
    dialogVisible.value = false
    fetchEngineers()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '创建失败')
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try {
    await deleteEngineer(id)
    ElMessage.success('删除成功')
    fetchEngineers()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

async function handleResetPwd(id: number) {
  try {
    await resetEngineerPassword(id)
    ElMessage.success('密码已重置为 admin123')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '重置失败')
  }
}

onMounted(fetchEngineers)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

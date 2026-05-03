<template>
  <div class="engineer-home">
    <el-container>
      <el-header class="eng-header">
        <span class="eng-logo">工程师工作台</span>
        <div class="eng-user">
          <span style="margin-right:12px">{{ engineerPhone }}</span>
          <el-button size="small" @click="showPwdDialog">修改密码</el-button>
          <el-button size="small" type="danger" plain @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <el-input v-model="keyword" placeholder="搜索..." clearable style="width:260px" @input="debouncedFetch">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <span>共 {{ total }} 个任务</span>
            </div>
          </template>
          <el-table :data="tasks" v-loading="loading" stripe>
            <el-table-column label="#" type="index" width="50" />
            <el-table-column label="产品号" prop="productId" width="120" show-overflow-tooltip />
            <el-table-column label="用户" prop="userName" width="100" />
            <el-table-column label="用户号码" prop="phoneNumber" width="120" show-overflow-tooltip />
            <el-table-column label="地址" prop="userAddress" show-overflow-tooltip min-width="160" />
            <el-table-column label="区域" prop="area" width="80" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTypeMap[row.status]?.type || 'info'" size="small">{{ statusTypeMap[row.status]?.label || '未知' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="$router.push(`/task/${row.id}`)">查看详细</el-button>
                <el-button size="small" @click="$router.push(`/scan/${row.id}`)" v-if="row.status < 2">扫码</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchTasks" />
          </div>
        </el-card>
      </el-main>
    </el-container>

    <!-- Password change dialog -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px" destroy-on-close>
      <el-form>
        <el-form-item label="原密码">
          <el-input v-model="oldPwd" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPwd" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleChangePwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTasks } from '../../api'
import { changePassword } from '../../api/engineer'
import { statusTypeMap } from '../../constants'
import type { RecycleTask } from '../../types'

const router = useRouter()
const engineerPhone = localStorage.getItem('engineer_phone') || ''

const tasks = ref<RecycleTask[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(50)
const total = ref(0)
const keyword = ref('')

const pwdDialogVisible = ref(false)
const oldPwd = ref('')
const newPwd = ref('')
const pwdLoading = ref(false)

let timer: ReturnType<typeof setTimeout> | null = null
function debouncedFetch() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { page.value = 0; fetchTasks() }, 300)
}

async function fetchTasks() {
  loading.value = true
  try {
    const { data: res } = await getTasks({ keyword: keyword.value, page: page.value, size: size.value })
    tasks.value = res.data.content
    total.value = res.data.totalElements
    if (tasks.value.length === 0 && page.value > 0) { page.value--; fetchTasks(); return }
  } finally {
    loading.value = false
  }
}

function showPwdDialog() {
  oldPwd.value = ''
  newPwd.value = ''
  pwdDialogVisible.value = true
}

async function handleChangePwd() {
  if (!oldPwd.value || !newPwd.value) {
    ElMessage.warning('请填写密码')
    return
  }
  pwdLoading.value = true
  try {
    await changePassword(oldPwd.value, newPwd.value)
    ElMessage.success('密码修改成功')
    pwdDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '修改失败')
  } finally {
    pwdLoading.value = false
  }
}

function handleLogout() {
  localStorage.removeItem('engineer_token')
  localStorage.removeItem('engineer_phone')
  router.push('/engineer/login')
}

onMounted(fetchTasks)
</script>

<style scoped>
.eng-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #1677ff;
  color: #fff;
  padding: 0 20px;
}
.eng-logo { font-size: 18px; font-weight: 600; }
.eng-user { display: flex; align-items: center; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

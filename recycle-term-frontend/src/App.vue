<template>
  <el-container class="app-container" direction="vertical">
    <el-header class="app-header">
      <div class="header-left" @click="$router.push('/')" style="cursor:pointer">
        <el-icon :size="24"><Box /></el-icon>
        <h1>终端回收管理系统</h1>
      </div>
      <div class="header-right">
        <template v-if="authPhone">
          <span class="auth-name">{{ authPhone }}</span>
          <el-button v-if="authRole === 'admin'" text size="small" @click="$router.push('/admin')">管理后台</el-button>
          <el-button v-if="authRole === 'engineer'" text size="small" @click="showPwdDialog">修改密码</el-button>
          <el-button text size="small" type="danger" @click="handleLogout">退出</el-button>
        </template>
        <template v-else>
          <el-button text size="small" @click="$router.push('/login')">登录</el-button>
        </template>
      </div>
    </el-header>

    <el-container class="body-container">
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px" destroy-on-close>
    <el-form>
      <el-form-item label="原密码"><el-input v-model="oldPwd" type="password" show-password /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="newPwd" type="password" show-password /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="pwdLoading" @click="handleChangePwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from './api/engineer'

const router = useRouter()

const authPhone = computed(() => localStorage.getItem('auth_phone'))
const authRole = computed(() => localStorage.getItem('auth_role'))

// Engineer password change
const pwdDialogVisible = ref(false)
const oldPwd = ref('')
const newPwd = ref('')
const pwdLoading = ref(false)

function showPwdDialog() {
  oldPwd.value = ''
  newPwd.value = ''
  pwdDialogVisible.value = true
}

async function handleChangePwd() {
  if (!oldPwd.value || !newPwd.value) { ElMessage.warning('请填写密码'); return }
  pwdLoading.value = true
  try {
    await changePassword(oldPwd.value, newPwd.value)
    ElMessage.success('密码修改成功')
    pwdDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '修改失败')
  } finally { pwdLoading.value = false }
}

function handleLogout() {
  localStorage.removeItem('auth_token')
  localStorage.removeItem('auth_role')
  localStorage.removeItem('auth_phone')
  router.push('/')
}
</script>

<style scoped>
.app-container { height: 100vh; overflow: hidden; }
.app-header {
  background: #1677ff; color: #fff;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; height: 64px;
  box-shadow: 0 2px 8px rgba(0,0,0,.15); flex-shrink: 0; z-index: 10;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.header-left h1 { font-size: 18px; font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 12px; font-size: 13px; opacity: .9; }
.header-right .el-button { color: rgba(255,255,255,.85); }
.header-right .el-button:hover { color: #fff; }
.auth-name { font-weight: 500; }
.body-container { flex: 1; overflow: hidden; }
.app-main { max-width: 1200px; margin: 0 auto; width: 100%; padding: 20px; overflow-y: auto; }
@media (max-width: 768px) {
  .app-header { padding: 0 12px; height: 52px; }
  .header-left h1 { font-size: 14px; }
  .header-right { font-size: 11px; gap: 8px; }
  .app-main { padding: 12px; }
}
</style>

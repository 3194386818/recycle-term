<template>
  <div class="login-page">
    <div class="login-card">
      <el-card shadow="always">
        <template #header>
          <h2 class="login-title">终端回收管理系统</h2>
        </template>
        <el-form @submit.prevent="handleLogin">
          <el-form-item>
            <el-input v-model="phone" placeholder="手机号码" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="password" type="password" placeholder="密码" size="large" show-password />
          </el-form-item>
          <el-form-item>
            <el-radio-group v-model="loginRole">
              <el-radio value="engineer">工程师</el-radio>
              <el-radio value="admin">管理员</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" native-type="submit" style="width:100%">
              登录
            </el-button>
          </el-form-item>
          <div style="text-align:center;color:#999;font-size:13px">管理员: admin / admin123 | 工程师: 手机号 admin123</div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLogin } from '../api/admin'
import { engineerLogin } from '../api/engineer'

const router = useRouter()
const route = useRoute()
const phone = ref('')
const password = ref('')
const loading = ref(false)
const loginRole = ref<'engineer' | 'admin'>('engineer')

async function handleLogin() {
  if (!phone.value || !password.value) {
    ElMessage.warning('请输入手机号和密码')
    return
  }
  loading.value = true
  try {
    if (loginRole.value === 'admin') {
      const { data: res } = await adminLogin(phone.value, password.value)
      localStorage.setItem('auth_token', res.data.token)
      localStorage.setItem('auth_role', 'admin')
      localStorage.setItem('auth_phone', phone.value)
      ElMessage.success('管理员登录成功')
    } else {
      const { data: res } = await engineerLogin(phone.value, password.value)
      localStorage.setItem('auth_token', res.data.token)
      localStorage.setItem('auth_role', 'engineer')
      localStorage.setItem('auth_phone', res.data.phone)
    }
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.login-card { width: 360px; }
.login-title { text-align: center; margin: 0; }
</style>

<template>
  <div class="login-page">
    <div class="login-card">
      <el-card shadow="always">
        <template #header>
          <h2 class="login-title">工程师登录</h2>
        </template>
        <el-form @submit.prevent="handleLogin">
          <el-form-item>
            <el-input v-model="phone" placeholder="手机号码" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="password" type="password" placeholder="密码" size="large" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" native-type="submit" style="width:100%">
              登录
            </el-button>
          </el-form-item>
          <div style="text-align:center;color:#999;font-size:13px">默认密码: admin123</div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { engineerLogin } from '../../api/engineer'

const router = useRouter()
const phone = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!phone.value || !password.value) {
    ElMessage.warning('请输入手机号和密码')
    return
  }
  loading.value = true
  try {
    const { data: res } = await engineerLogin(phone.value, password.value)
    localStorage.setItem('engineer_token', res.data.token)
    localStorage.setItem('engineer_phone', res.data.phone)
    localStorage.setItem('engineer_name', res.data.name)
    ElMessage.success('登录成功')
    router.push('/engineer')
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

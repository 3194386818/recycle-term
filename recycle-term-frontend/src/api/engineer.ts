import axios from 'axios'
import type { ApiResult } from '../types'

const engineerApi = axios.create({
  baseURL: '/api/engineer',
  timeout: 10000,
})

engineerApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('engineer_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

engineerApi.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('engineer_token')
      window.location.href = '/engineer/login'
    }
    return Promise.reject(err)
  }
)

export function engineerLogin(phone: string, password: string) {
  return engineerApi.post<ApiResult<{ token: string; phone: string; name: string }>>('/login', { phone, password })
}

export function changePassword(oldPassword: string, newPassword: string) {
  return engineerApi.post<ApiResult<void>>('/change-password', { oldPassword, newPassword })
}

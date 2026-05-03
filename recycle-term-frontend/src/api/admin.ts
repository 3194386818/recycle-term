import axios from 'axios'
import type { RecycleTask, ApiResult, PageResult, OperationLog } from '../types'

const adminApi = axios.create({
  baseURL: '/api/admin',
  timeout: 10000,
})

adminApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

adminApi.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('admin_token')
      window.location.href = '/admin/login'
    }
    return Promise.reject(err)
  }
)

export function adminLogin(username: string, password: string) {
  return adminApi.post<ApiResult<{ token: string; username: string }>>('/login', { username, password })
}

export function getAdminTasks(params: { keyword?: string; status?: number; page?: number; size?: number }) {
  return adminApi.get<ApiResult<PageResult<RecycleTask>>>('/tasks', { params })
}

export function createTask(data: Partial<RecycleTask>) {
  return adminApi.post<ApiResult<RecycleTask>>('/tasks', data)
}

export function batchCreateTasks(data: Partial<RecycleTask>[]) {
  return adminApi.post<ApiResult<RecycleTask[]>>('/tasks/batch', data)
}

export function updateAdminTask(id: number, data: Partial<RecycleTask>) {
  return adminApi.put<ApiResult<RecycleTask>>(`/tasks/${id}`, data)
}

export function deleteAdminTask(id: number) {
  return adminApi.delete<ApiResult<void>>(`/tasks/${id}`)
}

export function batchDeleteAdminTasks(ids: number[]) {
  return adminApi.delete<ApiResult<void>>('/tasks/batch', { data: ids })
}

export function getDailyStats(days?: number) {
  return adminApi.get<ApiResult<Array<{ date: string; completed: number; scanned: number }>>>('/stats/daily', { params: { days } })
}

export function getStatusStats() {
  return adminApi.get<ApiResult<Record<string, number>>>('/stats/status')
}

export function getAreaStats() {
  return adminApi.get<ApiResult<Array<{ area: string; count: number }>>>('/stats/area')
}

export function getLogs(params: { page?: number; size?: number }) {
  return adminApi.get<ApiResult<PageResult<OperationLog>>>('/logs', { params })
}

export function reviewTask(id: number, approved: boolean, reviewRemark?: string) {
  return adminApi.patch<ApiResult<RecycleTask>>(`/tasks/${id}/review`, null, { params: { approved, reviewRemark } })
}

export function getAdminRecords(taskId: number) {
  return adminApi.get<ApiResult<any[]>>(`/records/task/${taskId}`, { baseURL: '/api' })
}

// Engineer management
export function getEngineers(params: { page?: number; size?: number }) {
  return adminApi.get<ApiResult<PageResult<{ id: number; phone: string; name: string; createdAt: string }>>>('/engineers', { params })
}

export function createEngineer(phone: string, name: string) {
  return adminApi.post<ApiResult<any>>('/engineers', { phone, name })
}

export function deleteEngineer(id: number) {
  return adminApi.delete<ApiResult<void>>(`/engineers/${id}`)
}

export function resetEngineerPassword(id: number) {
  return adminApi.post<ApiResult<void>>(`/engineers/${id}/reset-password`)
}

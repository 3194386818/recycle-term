import axios from 'axios'
import type { WorkOrder, WorkOrderOption, WorkOrderOptions, WorkOrderQuickParseDto, WorkOrderLegacyImportPreview, WorkOrderLegacyImportResult, ApiResult, PageResult } from '../types'

const baseURL = import.meta.env.VITE_API_BASE_URL
  ? import.meta.env.VITE_API_BASE_URL + '/api'
  : '/api'

const api = axios.create({
  baseURL,
  timeout: 10000,
})

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

// Public APIs
export function getWorkOrders(params: { keyword?: string; status?: string; page?: number; size?: number }) {
  return api.get<ApiResult<PageResult<WorkOrder>>>('/work-orders', { params })
}

export function getWorkOrderById(id: number) {
  return api.get<ApiResult<WorkOrder>>(`/work-orders/${id}`)
}

export function createWorkOrder(data: Partial<WorkOrder>) {
  return api.post<ApiResult<WorkOrder>>('/work-orders', data)
}

export function updateWorkOrder(id: number, data: Partial<WorkOrder>) {
  return api.put<ApiResult<WorkOrder>>(`/work-orders/${id}`, data)
}

export function updateWorkOrderStatus(id: number, status: string, failureReason?: string) {
  return api.patch<ApiResult<WorkOrder>>(`/work-orders/${id}/status`, { status, failureReason })
}

export function quickParseWorkOrder(data: WorkOrderQuickParseDto) {
  return api.post<ApiResult<Partial<WorkOrder>>>('/work-orders/quick-parse', data)
}

export function quickCreateWorkOrder(data: WorkOrderQuickParseDto) {
  return api.post<ApiResult<WorkOrder>>('/work-orders/quick', data)
}

export function getWorkOrderOptions() {
  return api.get<ApiResult<WorkOrderOptions>>('/work-orders/options')
}

// Admin APIs
export function getAdminWorkOrders(params: { keyword?: string; status?: string; type?: string; page?: number; size?: number }) {
  return adminApi.get<ApiResult<PageResult<WorkOrder>>>('/work-orders', { params })
}

export function getAdminWorkOrderById(id: number) {
  return adminApi.get<ApiResult<WorkOrder>>(`/work-orders/${id}`)
}

export function createAdminWorkOrder(data: Partial<WorkOrder>) {
  return adminApi.post<ApiResult<WorkOrder>>('/work-orders', data)
}

export function updateAdminWorkOrder(id: number, data: Partial<WorkOrder>) {
  return adminApi.put<ApiResult<WorkOrder>>(`/work-orders/${id}`, data)
}

export function deleteAdminWorkOrder(id: number) {
  return adminApi.delete<ApiResult<void>>(`/work-orders/${id}`)
}

export function updateAdminWorkOrderStatus(id: number, status: string, failureReason?: string) {
  return adminApi.patch<ApiResult<WorkOrder>>(`/work-orders/${id}/status`, { status, failureReason })
}

export function previewLegacyImport(sampleSize: number = 20) {
  return adminApi.get<ApiResult<WorkOrderLegacyImportPreview>>('/work-orders/legacy-import/preview', { params: { sampleSize } })
}

export function executeLegacyImport() {
  return adminApi.post<ApiResult<WorkOrderLegacyImportResult>>('/work-orders/legacy-import')
}

export function getAdminOptions() {
  return adminApi.get<ApiResult<WorkOrderOptions>>('/work-orders/options')
}

export function listAdminOptionItems(category?: string) {
  return adminApi.get<ApiResult<WorkOrderOption[]>>('/work-orders/option-items', { params: { category } })
}

export function createAdminOption(data: Partial<WorkOrderOption>) {
  return adminApi.post<ApiResult<WorkOrderOption>>('/work-orders/option-items', data)
}

export function updateAdminOption(id: number, data: Partial<WorkOrderOption>) {
  return adminApi.put<ApiResult<WorkOrderOption>>(`/work-orders/option-items/${id}`, data)
}

export function deleteAdminOption(id: number) {
  return adminApi.delete<ApiResult<void>>(`/work-orders/option-items/${id}`)
}

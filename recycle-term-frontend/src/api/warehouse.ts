import axios from 'axios'
import type { ApiResult, PageResult } from '../types'

export interface WarehouseItem {
  id: number
  productId: string
  devices: string
  outbound: boolean
  outboundAt: string | null
  customerName: string
  phone: string
  splitter: string
  address: string
  snNumber: string
  accessRoom: string
  receivedAt: string
}

export interface DeviceInfo {
  type: string
  sn: string
  outbound?: boolean
  outboundAt?: string | null
}

export interface WarehouseChangeRequest {
  id: number
  warehouseItemId: number
  requestType: 'UPDATE' | 'DELETE'
  requestContent: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  rejectReason: string | null
  requestedBy: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  createdAt: string
}

const api = axios.create({
  baseURL: '/api/warehouse',
  timeout: 10000,
})

export function searchWarehouse(keyword?: string, page?: number, size?: number) {
  return api.get<ApiResult<PageResult<WarehouseItem>>>('', { params: { keyword, page, size } })
}

export function getWarehouseItem(id: number) {
  return api.get<ApiResult<WarehouseItem>>(`/${id}`)
}

export function createWarehouseItem(data: Partial<WarehouseItem>) {
  return api.post<ApiResult<WarehouseItem>>('', data)
}

export function updateWarehouseItem(id: number, data: Partial<WarehouseItem>) {
  return api.put<ApiResult<WarehouseItem>>(`/${id}`, data)
}

export function deleteWarehouseItem(id: number) {
  return api.delete<ApiResult<void>>(`/${id}`)
}

export function outboundWarehouseItem(id: number, sn: string) {
  return api.post<ApiResult<WarehouseItem>>(`/${id}/outbound`, null, { params: { sn } })
}

export function createWarehouseChangeRequest(data: {
  warehouseItemId: number
  requestType: 'UPDATE' | 'DELETE'
  requestContent?: string
  requestedBy?: string
}) {
  return api.post<ApiResult<WarehouseChangeRequest>>('/requests', data)
}

export function listAdminWarehouseRequests() {
  return axios.get<ApiResult<WarehouseChangeRequest[]>>('/api/admin/warehouse-requests', {
    headers: { Authorization: `Bearer ${localStorage.getItem('admin_token') || ''}` }
  })
}

export function reviewAdminWarehouseRequest(id: number, approved: boolean, rejectReason?: string) {
  return axios.patch<ApiResult<WarehouseChangeRequest>>(`/api/admin/warehouse-requests/${id}/review`,
    { approved, rejectReason },
    { headers: { Authorization: `Bearer ${localStorage.getItem('admin_token') || ''}` } }
  )
}

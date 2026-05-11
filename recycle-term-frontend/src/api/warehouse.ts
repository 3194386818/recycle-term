import axios from 'axios'
import type { ApiResult, PageResult } from '../types'

export interface Warehouse {
  id: number
  name: string
  enabled: boolean
  defaultWarehouse: boolean
  createdAt: string
  updatedAt: string
}

export interface WarehouseDevice {
  id?: number
  warehouseItemId?: number
  warehouseId?: number | null
  warehouseName?: string | null
  type: string
  sn: string
  status?: 'IN_STOCK' | 'OUTBOUND'
  outbound?: boolean
  inboundAt?: string | null
  outboundAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export type DeviceInfo = WarehouseDevice

export interface WarehouseDeviceMovement {
  id: number
  deviceId: number
  fromWarehouseId: number | null
  fromWarehouseName: string | null
  toWarehouseId: number | null
  toWarehouseName: string | null
  movementType: 'INBOUND' | 'TRANSFER' | 'OUTBOUND'
  operator: string | null
  remark: string | null
  createdAt: string
}

export interface WarehouseItem {
  id: number
  productId: string
  devices: WarehouseDevice[]
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

function adminHeaders() {
  return { Authorization: `Bearer ${localStorage.getItem('admin_token') || ''}` }
}

export function searchWarehouse(keyword?: string, page?: number, size?: number, warehouseId?: number, includeDisabled?: boolean, outboundOnly?: boolean) {
  return api.get<ApiResult<PageResult<WarehouseItem>>>('', { params: { keyword, page, size, warehouseId, includeDisabled, outboundOnly } })
}

export function listWarehouses(includeDisabled = false) {
  return api.get<ApiResult<Warehouse[]>>('/warehouses', { params: { includeDisabled } })
}

export function getWarehouseItem(id: number, includeDisabled = false) {
  return api.get<ApiResult<WarehouseItem>>(`/${id}`, { params: { includeDisabled } })
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

export function outboundWarehouseDevice(deviceId: number) {
  return api.post<ApiResult<WarehouseDevice>>(`/devices/${deviceId}/outbound`)
}

export function transferWarehouseDevice(deviceId: number, toWarehouseId: number, remark?: string) {
  return api.post<ApiResult<WarehouseDevice>>(`/devices/${deviceId}/transfer`, { toWarehouseId, remark })
}

export function getWarehouseDeviceMovements(deviceId: number) {
  return api.get<ApiResult<WarehouseDeviceMovement[]>>(`/devices/${deviceId}/movements`)
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
  return axios.get<ApiResult<WarehouseChangeRequest[]>>('/api/admin/warehouse-requests', { headers: adminHeaders() })
}

export function reviewAdminWarehouseRequest(id: number, approved: boolean, rejectReason?: string) {
  return axios.patch<ApiResult<WarehouseChangeRequest>>(`/api/admin/warehouse-requests/${id}/review`,
    { approved, rejectReason },
    { headers: adminHeaders() }
  )
}

export function listAdminWarehouses(includeDisabled = true) {
  return axios.get<ApiResult<Warehouse[]>>('/api/admin/warehouses', { params: { includeDisabled }, headers: adminHeaders() })
}

export function createAdminWarehouse(data: { name: string; enabled?: boolean }) {
  return axios.post<ApiResult<Warehouse>>('/api/admin/warehouses', data, { headers: adminHeaders() })
}

export function updateAdminWarehouse(id: number, data: { name: string; enabled?: boolean }) {
  return axios.put<ApiResult<Warehouse>>(`/api/admin/warehouses/${id}`, data, { headers: adminHeaders() })
}

export function setAdminWarehouseEnabled(id: number, enabled: boolean) {
  return axios.patch<ApiResult<Warehouse>>(`/api/admin/warehouses/${id}/enabled`, null, { params: { enabled }, headers: adminHeaders() })
}

export function deleteAdminWarehouse(id: number) {
  return axios.delete<ApiResult<void>>(`/api/admin/warehouses/${id}`, { headers: adminHeaders() })
}

export function batchTransferWarehouseDevices(deviceIds: number[], toWarehouseId: number, remark?: string) {
  return axios.post<ApiResult<WarehouseDevice[]>>('/api/admin/warehouses/devices/batch-transfer', { deviceIds, toWarehouseId, remark }, { headers: adminHeaders() })
}

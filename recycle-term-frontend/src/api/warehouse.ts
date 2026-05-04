import axios from 'axios'
import type { ApiResult, PageResult } from '../types'

export interface WarehouseItem {
  id: number
  productId: string
  devices: string
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

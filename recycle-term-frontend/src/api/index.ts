import axios from 'axios'
import type { RecycleTask, TerminalRecord, Stats, PageResult, ApiResult } from '../types'

const baseURL = import.meta.env.VITE_API_BASE_URL
  ? import.meta.env.VITE_API_BASE_URL + '/api'
  : '/api'

const api = axios.create({
  baseURL,
  timeout: 10000,
})

// Tasks
export function getTasks(params: {
  keyword?: string
  completed?: boolean | null
  needVisit?: boolean | null
  status?: string
  page?: number
  size?: number
}) {
  return api.get<ApiResult<PageResult<RecycleTask>>>('/tasks', { params })
}

export function getTaskById(id: number) {
  return api.get<ApiResult<RecycleTask>>(`/tasks/${id}`)
}

export function updateTask(id: number, data: Partial<RecycleTask>) {
  return api.put<ApiResult<RecycleTask>>(`/tasks/${id}`, data)
}

export function deleteTask(id: number) {
  return api.delete<ApiResult<void>>(`/tasks/${id}`)
}

export function getStats() {
  return api.get<ApiResult<Stats>>('/tasks/stats')
}

// Terminal Records
export function getRecordsByTaskId(taskId: number) {
  return api.get<ApiResult<TerminalRecord[]>>(`/records/task/${taskId}`)
}

export function scanTerminals(taskId: number, serialNumbers: string[]) {
  return api.post<ApiResult<TerminalRecord[]>>(`/records/scan/${taskId}`, { serialNumbers })
}

export function deleteRecord(id: number) {
  return api.delete<ApiResult<void>>(`/records/${id}`)
}

// Import
export function importExcel(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post<ApiResult<RecycleTask[]>>('/import/excel', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

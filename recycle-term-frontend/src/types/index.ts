export interface RecycleTask {
  id: number
  phoneNumber: string
  userName: string
  userAddress: string
  area: string
  engineerName: string
  engineerPhone: string
  detailDesc: string
  terminals: string
  expectedCount: number
  fttrCount: number
  accessRoom: string
  category: string
  devDept: string
  devPerson: string
  needVisit: boolean
  status: string
  completed: boolean
  completedAt: string | null
  remark: string
  createdAt: string
  updatedAt: string
}

export interface TerminalRecord {
  id: number
  taskId: number
  serialNumber: string
  scannedAt: string
}

export interface Stats {
  total: number
  completed: number
  pending: number
  needVisit: number
  failed: number
  totalScanned: number
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface OperationLog {
  id: number
  adminId: number
  adminUsername: string
  action: string
  detail: string
  ip: string
  createdAt: string
}

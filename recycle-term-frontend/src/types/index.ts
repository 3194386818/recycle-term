export interface RecycleTask {
  id: number
  phoneNumber: string
  productId: string
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
  status: number
  completed: boolean
  completedAt: string | null
  failReason: string
  reviewRemark: string
  reviewerId: number | null
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
  pendingReview: number
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

export interface WorkOrder {
  id: number
  workOrderNo: string
  productId: string
  userName: string
  contactPhone: string
  workOrderType: string
  address: string
  cvlan: string
  svlan: string
  splitter: string
  splitterPort: string
  onuSn: string
  status: string
  failureReason: string
  remark: string
  appointedAt: string | null
  fulfilledAt: string | null
  completedAt: string | null
  transferredAt: string | null
  failedAt: string | null
  sourceType: string | null
  sourceId: string | null
  rawSource: string | null
  createdAt: string
  updatedAt: string
}

export interface WorkOrderOption {
  id: number
  category: string
  value: string
  label: string
  sortOrder: number
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export interface WorkOrderOptions {
  types: WorkOrderOption[]
  failureReasons: WorkOrderOption[]
  statuses: WorkOrderOption[]
}

export interface WorkOrderQuickParseDto {
  text: string
}

export interface WorkOrderLegacyImportPreview {
  total: number
  importable: number
  existing: number
  samples: Partial<WorkOrder>[]
}

export interface WorkOrderLegacyImportResult {
  total: number
  created: number
  skipped: number
}

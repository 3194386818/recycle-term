export const statusTypeMap: Record<number, { label: string; type: string }> = {
  0: { label: '待回收', type: 'warning' },
  1: { label: '已上门', type: 'primary' },
  2: { label: '待审核(完成)', type: 'success' },
  3: { label: '待审核(失败)', type: 'danger' },
  4: { label: '审核成功', type: 'success' },
  5: { label: '审核失败', type: 'danger' },
  6: { label: '已归档', type: 'info' },
}

export const statusStepIndexMap: Record<number, number> = {
  0: 0,
  1: 1,
  2: 2,
  3: 2,
  4: 3,
  5: 3,
  6: 3,
}

export function getReviewStatusMeta(status: number): { label: string; type: 'success' | 'danger' } {
  return status === 2
    ? { label: '已完成', type: 'success' }
    : { label: '已失败', type: 'danger' }
}

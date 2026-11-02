export const DECLARATION_STATUS: Record<string, { label: string; className: string }> = {
  DRAFT: { label: '草稿', className: 'status-draft' },
  PENDING_REVIEW: { label: '待审核', className: 'status-pending' },
  APPROVED: { label: '审核通过', className: 'status-approved' },
  RETURNED: { label: '已退回', className: 'status-returned' },
  WITHDRAWN: { label: '已撤回', className: 'status-withdrawn' },
}

export const TAXPAYER_TYPE: Record<string, string> = {
  ENTERPRISE: '企业',
  INDIVIDUAL: '个人',
  OTHER: '其他',
}

export const USER_STATUS: Record<string, string> = {
  ACTIVE: '启用',
  INACTIVE: '停用',
}

export const TEMPLATE_STATUS: Record<string, { label: string; className: string }> = {
  DRAFT: { label: '草稿', className: 'status-draft' },
  PUBLISHED: { label: '已发布', className: 'status-published' },
  DISABLED: { label: '已停用', className: 'status-disabled' },
}

export const FIELD_TYPE: Record<string, string> = {
  TEXT: '文本',
  INTEGER: '整数',
  DECIMAL: '金额/小数',
  DATE: '日期',
  RADIO: '单选',
  MULTI_SELECT: '多选',
  BOOLEAN: '是否',
}

export const REVIEW_ACTION: Record<string, string> = {
  APPROVE: '审核通过',
  RETURN: '退回修改',
}

export const declarationStatus = (status?: string) =>
  DECLARATION_STATUS[status ?? ''] ?? { label: status ?? '-', className: '' }

export const templateStatus = (status?: string) =>
  TEMPLATE_STATUS[status ?? ''] ?? { label: status ?? '-', className: '' }

export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function formatDate(value?: string | null): string {
  if (!value) return '-'
  return value.slice(0, 10)
}

export function formatAmount(value?: number | string | null): string {
  if (value === null || value === undefined || value === '') return '0.00'
  const number = Number(value)
  if (Number.isNaN(number)) return String(value)
  return number.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatFormValue(value: unknown): string {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (Array.isArray(value)) return value.join('、')
  return String(value)
}

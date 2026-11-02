export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId?: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

export interface MenuItem {
  code: string
  name: string
  path: string
  sortOrder: number
}

export interface CurrentUser {
  id: number
  username: string
  displayName: string
  roles: string[]
  permissions: string[]
  menus: MenuItem[]
}

export interface UserRoleRef {
  id: number
  roleCode: string
  roleName: string
}

export interface User {
  id: number
  username: string
  displayName: string
  status: string
  remark?: string
  lastLoginAt?: string
  createdAt?: string
  roles: UserRoleRef[]
}

export interface Permission {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: string
  routePath?: string
  apiPattern?: string
  httpMethod?: string
  sortOrder?: number
}

export interface Role {
  id: number
  roleCode: string
  roleName: string
  description?: string
  status: string
  userCount: number
  permissionIds: number[]
  createdAt?: string
}

export interface Taxpayer {
  id: number
  taxpayerCode: string
  taxpayerName: string
  taxpayerType: string
  contactName?: string
  contactPhone?: string
  registrationStatus: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface DeclarationType {
  id: number
  typeCode: string
  typeName: string
  description?: string
  enabled: boolean
  defaultTemplateId?: number
  defaultTemplateName?: string
  createdAt?: string
  updatedAt?: string
}

export interface FieldOption {
  label: string
  value: string
}

export interface FieldDef {
  id?: number
  templateId?: number
  fieldCode: string
  fieldLabel: string
  fieldType: string
  required: boolean
  minValue?: number | null
  maxValue?: number | null
  maxLength?: number | null
  options: FieldOption[]
  helpText?: string
  sortOrder: number
  enabled: boolean
}

export interface Template {
  id: number
  templateCode: string
  templateName: string
  declarationTypeId: number
  declarationTypeName?: string
  versionNo: number
  status: string
  createdBy?: number
  createdByName?: string
  publishedAt?: string
  createdAt?: string
  updatedAt?: string
  fields: FieldDef[]
}

export interface Review {
  id: number
  declarationId: number
  reviewerId: number
  reviewerName: string
  action: string
  comment?: string
  fromStatus: string
  toStatus: string
  reviewedAt: string
}

export interface Declaration {
  id: number
  declarationNo: string
  taxpayerId: number
  taxpayerCode?: string
  taxpayerName: string
  declarationTypeId: number
  declarationTypeName: string
  templateId: number
  templateName?: string
  templateVersionNo: number
  periodStart: string
  periodEnd: string
  status: string
  declaredAmount: number | null
  formData: Record<string, unknown>
  createdBy: number
  createdByName?: string
  submittedAt?: string
  lastReviewedAt?: string
  createdAt?: string
  updatedAt?: string
  fields: FieldDef[]
  reviews: Review[]
}

export interface Overview {
  total: number
  draft: number
  pending: number
  approved: number
  returned: number
  withdrawn: number
  totalAmount: number | null
  approvalRate: number | null
}

export interface TrendStat {
  month: string
  count: number
  amount: number
}

export interface StatusStat {
  status: string
  count: number
}

export interface TypeStat {
  typeName: string
  count: number
  amount: number
}

export interface TaxpayerTypeStat {
  taxpayerType: string
  count: number
  amount: number
}

export interface AuditLog {
  id: number
  operatorId?: number
  operatorName?: string
  operationType: string
  targetType?: string
  targetId?: string
  result: string
  summary?: string
  traceId?: string
  createdAt: string
}

export interface Dashboard {
  overview: Overview
  trend: TrendStat[]
  byStatus: StatusStat[]
  byType: TypeStat[]
  byTaxpayerType: TaxpayerTypeStat[]
  todos: Declaration[]
  recentLogs: PageResult<AuditLog>
}

export interface ReviewHistory {
  id: number
  declarationId: number
  declarationNo: string
  taxpayerName: string
  declarationTypeName: string
  action: string
  comment?: string
  reviewerId: number
  reviewerName: string
  fromStatus: string
  toStatus: string
  reviewedAt: string
}

export interface DeclQuery {
  keyword?: string
  declarationNo?: string
  taxpayerCode?: string
  taxpayerName?: string
  declarationTypeId?: number | null
  status?: string
  periodFrom?: string
  periodTo?: string
  submittedFrom?: string
  submittedTo?: string
  minAmount?: number | null
  maxAmount?: number | null
  sortBy?: string
  sortOrder?: string
  page?: number
  pageSize?: number
}

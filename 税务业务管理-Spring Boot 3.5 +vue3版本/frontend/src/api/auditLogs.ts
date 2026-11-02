import http from '@/utils/request'
import type { ApiResponse, AuditLog, PageResult } from '@/types/models'

export interface AuditQuery {
  page?: number
  pageSize?: number
  operatorId?: number | null
  operationType?: string
  result?: string
  from?: string
  to?: string
  keyword?: string
}

export async function fetchAuditLogs(params: AuditQuery): Promise<PageResult<AuditLog>> {
  const response = await http.get<ApiResponse<PageResult<AuditLog>>>('/audit-logs', { params })
  return response.data.data
}

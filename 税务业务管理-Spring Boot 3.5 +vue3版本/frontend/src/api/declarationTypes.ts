import http from '@/utils/request'
import type { ApiResponse, DeclarationType, PageResult } from '@/types/models'

export interface DeclarationTypePayload {
  typeCode: string
  typeName: string
  description?: string
}

export async function fetchDeclarationTypes(params: {
  page?: number
  pageSize?: number
  keyword?: string
  enabled?: boolean | null
}): Promise<PageResult<DeclarationType>> {
  const response = await http.get<ApiResponse<PageResult<DeclarationType>>>('/declaration-types', { params })
  return response.data.data
}

export async function fetchDeclarationTypeOptions(): Promise<DeclarationType[]> {
  const response = await http.get<ApiResponse<DeclarationType[]>>('/declaration-types/options')
  return response.data.data
}

export async function createDeclarationType(payload: DeclarationTypePayload): Promise<DeclarationType> {
  const response = await http.post<ApiResponse<DeclarationType>>('/declaration-types', payload)
  return response.data.data
}

export async function updateDeclarationType(id: number, payload: DeclarationTypePayload): Promise<DeclarationType> {
  const response = await http.put<ApiResponse<DeclarationType>>(`/declaration-types/${id}`, payload)
  return response.data.data
}

export async function updateDeclarationTypeStatus(id: number, enabled: boolean): Promise<void> {
  await http.patch(`/declaration-types/${id}/status`, { enabled })
}

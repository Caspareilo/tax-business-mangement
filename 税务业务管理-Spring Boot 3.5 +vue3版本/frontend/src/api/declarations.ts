import http from '@/utils/request'
import type { ApiResponse, Declaration, DeclQuery, PageResult } from '@/types/models'

export interface DeclarationPayload {
  taxpayerId: number
  declarationTypeId: number
  periodStart: string
  periodEnd: string
  declaredAmount?: number | null
  formData: Record<string, unknown>
}

export async function fetchDeclarations(params: DeclQuery): Promise<PageResult<Declaration>> {
  const response = await http.get<ApiResponse<PageResult<Declaration>>>('/declarations', { params })
  return response.data.data
}

export async function fetchDeclaration(id: number): Promise<Declaration> {
  const response = await http.get<ApiResponse<Declaration>>(`/declarations/${id}`)
  return response.data.data
}

export async function createDeclaration(payload: DeclarationPayload): Promise<Declaration> {
  const response = await http.post<ApiResponse<Declaration>>('/declarations', payload)
  return response.data.data
}

export async function updateDeclaration(id: number, payload: DeclarationPayload): Promise<Declaration> {
  const response = await http.put<ApiResponse<Declaration>>(`/declarations/${id}`, payload)
  return response.data.data
}

export async function submitDeclaration(id: number): Promise<void> {
  await http.post(`/declarations/${id}/submit`)
}

export async function withdrawDeclaration(id: number): Promise<void> {
  await http.post(`/declarations/${id}/withdraw`)
}

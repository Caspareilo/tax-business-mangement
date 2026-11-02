import http from '@/utils/request'
import type { ApiResponse, Declaration, PageResult, Taxpayer } from '@/types/models'

export interface TaxpayerQuery {
  page?: number
  pageSize?: number
  keyword?: string
  type?: string
  status?: string
}

export interface TaxpayerPayload {
  taxpayerCode: string
  taxpayerName: string
  taxpayerType: string
  contactName?: string
  contactPhone?: string
  remark?: string
}

export async function fetchTaxpayers(params: TaxpayerQuery): Promise<PageResult<Taxpayer>> {
  const response = await http.get<ApiResponse<PageResult<Taxpayer>>>('/taxpayers', { params })
  return response.data.data
}

export async function fetchTaxpayerOptions(): Promise<Taxpayer[]> {
  const response = await http.get<ApiResponse<Taxpayer[]>>('/taxpayers/options')
  return response.data.data
}

export async function createTaxpayer(payload: TaxpayerPayload): Promise<Taxpayer> {
  const response = await http.post<ApiResponse<Taxpayer>>('/taxpayers', payload)
  return response.data.data
}

export async function updateTaxpayer(id: number, payload: TaxpayerPayload): Promise<Taxpayer> {
  const response = await http.put<ApiResponse<Taxpayer>>(`/taxpayers/${id}`, payload)
  return response.data.data
}

export async function updateTaxpayerStatus(id: number, registrationStatus: string): Promise<void> {
  await http.patch(`/taxpayers/${id}/status`, { registrationStatus })
}

export async function deleteTaxpayer(id: number): Promise<void> {
  await http.delete(`/taxpayers/${id}`)
}

export async function fetchTaxpayerDeclarations(
  id: number,
  params: { page?: number; pageSize?: number },
): Promise<PageResult<Declaration>> {
  const response = await http.get<ApiResponse<PageResult<Declaration>>>(`/taxpayers/${id}/declarations`, { params })
  return response.data.data
}

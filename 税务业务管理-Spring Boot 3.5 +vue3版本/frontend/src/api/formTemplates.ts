import http from '@/utils/request'
import type { ApiResponse, FieldDef, PageResult, Template } from '@/types/models'

export interface TemplatePayload {
  templateCode: string
  templateName: string
  declarationTypeId: number
  fields?: FieldDef[]
}

export async function fetchTemplates(params: {
  page?: number
  pageSize?: number
  keyword?: string
  declarationTypeId?: number | null
  status?: string
}): Promise<PageResult<Template>> {
  const response = await http.get<ApiResponse<PageResult<Template>>>('/form-templates', { params })
  return response.data.data
}

export async function fetchTemplate(id: number): Promise<Template> {
  const response = await http.get<ApiResponse<Template>>(`/form-templates/${id}`)
  return response.data.data
}

export async function fetchTemplateVersions(id: number): Promise<Template[]> {
  const response = await http.get<ApiResponse<Template[]>>(`/form-templates/${id}/versions`)
  return response.data.data
}

export async function createTemplate(payload: TemplatePayload): Promise<Template> {
  const response = await http.post<ApiResponse<Template>>('/form-templates', payload)
  return response.data.data
}

export async function updateTemplate(id: number, payload: TemplatePayload): Promise<Template> {
  const response = await http.put<ApiResponse<Template>>(`/form-templates/${id}`, payload)
  return response.data.data
}

export async function publishTemplate(id: number): Promise<Template> {
  const response = await http.post<ApiResponse<Template>>(`/form-templates/${id}/publish`)
  return response.data.data
}

export async function disableTemplate(id: number): Promise<void> {
  await http.post(`/form-templates/${id}/disable`)
}

export async function newTemplateVersion(id: number): Promise<Template> {
  const response = await http.post<ApiResponse<Template>>(`/form-templates/${id}/new-version`)
  return response.data.data
}

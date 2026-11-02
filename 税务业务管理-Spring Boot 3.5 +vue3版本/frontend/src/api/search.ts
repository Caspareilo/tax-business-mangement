import http from '@/utils/request'
import type { ApiResponse, Declaration, DeclQuery, PageResult } from '@/types/models'

export async function searchDeclarations(params: DeclQuery): Promise<PageResult<Declaration>> {
  const response = await http.get<ApiResponse<PageResult<Declaration>>>('/search/declarations', { params })
  return response.data.data
}

export async function exportDeclarations(params: DeclQuery): Promise<Blob> {
  const response = await http.get('/search/declarations/export', {
    params,
    responseType: 'blob',
  })
  return response.data as Blob
}

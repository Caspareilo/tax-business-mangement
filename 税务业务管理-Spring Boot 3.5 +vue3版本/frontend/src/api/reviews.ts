import http from '@/utils/request'
import type { ApiResponse, Declaration, PageResult, ReviewHistory } from '@/types/models'

export async function fetchPendingReviews(params: {
  page?: number
  pageSize?: number
  keyword?: string
  typeId?: number | null
}): Promise<PageResult<Declaration>> {
  const response = await http.get<ApiResponse<PageResult<Declaration>>>('/reviews/pending', { params })
  return response.data.data
}

export async function fetchReviewHistory(params: {
  page?: number
  pageSize?: number
  reviewerId?: number | null
  action?: string
  from?: string
  to?: string
  keyword?: string
}): Promise<PageResult<ReviewHistory>> {
  const response = await http.get<ApiResponse<PageResult<ReviewHistory>>>('/reviews/history', { params })
  return response.data.data
}

export async function approveDeclaration(id: number, comment: string): Promise<void> {
  await http.post(`/declarations/${id}/review/approve`, { comment })
}

export async function returnDeclaration(id: number, comment: string): Promise<void> {
  await http.post(`/declarations/${id}/review/return`, { comment })
}

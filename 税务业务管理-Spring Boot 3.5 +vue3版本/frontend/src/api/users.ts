import http from '@/utils/request'
import type { ApiResponse, PageResult, User } from '@/types/models'

export interface UserQuery {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
  roleId?: number | null
}

export interface UserPayload {
  username?: string
  password?: string
  displayName: string
  status: string
  remark?: string
  roleIds: number[]
}

export async function fetchUsers(params: UserQuery): Promise<PageResult<User>> {
  const response = await http.get<ApiResponse<PageResult<User>>>('/users', { params })
  return response.data.data
}

export async function createUser(payload: UserPayload): Promise<User> {
  const response = await http.post<ApiResponse<User>>('/users', payload)
  return response.data.data
}

export async function updateUser(id: number, payload: UserPayload): Promise<User> {
  const response = await http.put<ApiResponse<User>>(`/users/${id}`, payload)
  return response.data.data
}

export async function updateUserStatus(id: number, status: string): Promise<void> {
  await http.patch(`/users/${id}/status`, { status })
}

export async function resetUserPassword(id: number, newPassword: string): Promise<void> {
  await http.post(`/users/${id}/reset-password`, { newPassword })
}

export async function deleteUser(id: number): Promise<void> {
  await http.delete(`/users/${id}`)
}

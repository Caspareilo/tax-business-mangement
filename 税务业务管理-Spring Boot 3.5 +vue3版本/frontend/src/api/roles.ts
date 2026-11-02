import http from '@/utils/request'
import type { ApiResponse, PageResult, Permission, Role } from '@/types/models'

export interface RolePayload {
  roleCode: string
  roleName: string
  description?: string
  status: string
}

export async function fetchRoles(params: {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
}): Promise<PageResult<Role>> {
  const response = await http.get<ApiResponse<PageResult<Role>>>('/roles', { params })
  return response.data.data
}

export async function createRole(payload: RolePayload): Promise<Role> {
  const response = await http.post<ApiResponse<Role>>('/roles', payload)
  return response.data.data
}

export async function updateRole(id: number, payload: RolePayload): Promise<Role> {
  const response = await http.put<ApiResponse<Role>>(`/roles/${id}`, payload)
  return response.data.data
}

export async function assignPermissions(id: number, permissionIds: number[]): Promise<void> {
  await http.put(`/roles/${id}/permissions`, { permissionIds })
}

export async function fetchPermissions(): Promise<Permission[]> {
  const response = await http.get<ApiResponse<Permission[]>>('/roles/permissions')
  return response.data.data
}

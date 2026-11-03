import http from '@/utils/request'
import type { ApiResponse, CurrentUser } from '@/types/models'

export async function login(payload: { username: string; password: string }): Promise<CurrentUser> {
  const response = await http.post<ApiResponse<CurrentUser>>('/auth/login', payload)
  return response.data.data
}

export async function register(payload: {
  username: string
  displayName: string
  password: string
  confirmPassword: string
}): Promise<void> {
  await http.post('/auth/register', payload)
}

export async function fetchMe(): Promise<CurrentUser> {
  const response = await http.get<ApiResponse<CurrentUser>>('/auth/me')
  return response.data.data
}

export async function logout(): Promise<void> {
  await http.post('/auth/logout')
}

export async function changePassword(payload: { oldPassword: string; newPassword: string }): Promise<void> {
  await http.put('/auth/password', payload)
}

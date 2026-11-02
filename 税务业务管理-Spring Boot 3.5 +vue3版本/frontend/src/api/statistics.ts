import http from '@/utils/request'
import type {
  ApiResponse,
  Dashboard,
  Overview,
  StatusStat,
  TaxpayerTypeStat,
  TrendStat,
  TypeStat,
} from '@/types/models'

export interface StatsQuery {
  from?: string
  to?: string
  typeId?: number | null
  months?: number
}

export async function fetchDashboard(): Promise<Dashboard> {
  const response = await http.get<ApiResponse<Dashboard>>('/statistics/dashboard')
  return response.data.data
}

export async function fetchOverview(params: StatsQuery): Promise<Overview> {
  const response = await http.get<ApiResponse<Overview>>('/statistics/overview', { params })
  return response.data.data
}

export async function fetchTrend(params: StatsQuery): Promise<TrendStat[]> {
  const response = await http.get<ApiResponse<TrendStat[]>>('/statistics/declarations/trend', { params })
  return response.data.data
}

export async function fetchByStatus(params: StatsQuery): Promise<StatusStat[]> {
  const response = await http.get<ApiResponse<StatusStat[]>>('/statistics/declarations/by-status', { params })
  return response.data.data
}

export async function fetchByType(params: StatsQuery): Promise<TypeStat[]> {
  const response = await http.get<ApiResponse<TypeStat[]>>('/statistics/declarations/by-type', { params })
  return response.data.data
}

export async function fetchByTaxpayerType(params: StatsQuery): Promise<TaxpayerTypeStat[]> {
  const response = await http.get<ApiResponse<TaxpayerTypeStat[]>>('/statistics/declarations/by-taxpayer-type', {
    params,
  })
  return response.data.data
}

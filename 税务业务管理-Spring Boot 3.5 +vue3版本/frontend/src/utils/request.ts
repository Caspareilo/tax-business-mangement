import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

interface ErrorBody {
  code?: string
  message?: string
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 20000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ErrorBody>) => {
    const status = error.response?.status
    const url = error.config?.url ?? ''
    const message =
      error.response?.data?.message ||
      (status === 403
        ? '没有权限执行该操作'
        : status === 401
          ? '登录状态已失效，请重新登录'
          : status === 409
            ? '数据状态已变化，请刷新后重试'
            : status === 400
              ? '请求参数不合法'
              : status === 404
                ? '请求的数据不存在'
                : '请求失败，请稍后重试')

    if (status === 401) {
      if (!url.includes('/auth/login') && !url.includes('/auth/me') && !window.location.pathname.startsWith('/login')) {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      }
    } else if (status === 403) {
      ElMessage.error(message)
    } else if (status !== 401) {
      ElMessage.error(message)
    }
    return Promise.reject(new Error(message))
  },
)

export default http

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchMe, login as loginApi, logout as logoutApi } from '@/api/auth'
import type { CurrentUser } from '@/types/models'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<CurrentUser | null>(null)
  const loading = ref(false)

  const permissions = computed(() => new Set(user.value?.permissions ?? []))
  const menus = computed(() => [...(user.value?.menus ?? [])].sort((a, b) => a.sortOrder - b.sortOrder))

  function hasPerm(code?: string): boolean {
    if (!code) return true
    return permissions.value.has(code)
  }

  async function login(username: string, password: string): Promise<void> {
    loading.value = true
    try {
      user.value = await loginApi({ username, password })
    } finally {
      loading.value = false
    }
  }

  async function fetchCurrentUser(): Promise<void> {
    user.value = await fetchMe()
  }

  function clear(): void {
    user.value = null
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } finally {
      clear()
    }
  }

  return { user, loading, permissions, menus, hasPerm, login, fetchCurrentUser, logout, clear }
})

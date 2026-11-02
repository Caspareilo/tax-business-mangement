import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    permission?: string
    public?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '分析页', permission: 'dashboard:view' },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/UsersView.vue'),
        meta: { title: '用户管理', permission: 'user:menu' },
      },
      {
        path: 'roles',
        name: 'roles',
        component: () => import('@/views/RolesView.vue'),
        meta: { title: '角色权限', permission: 'role:menu' },
      },
      {
        path: 'taxpayers',
        name: 'taxpayers',
        component: () => import('@/views/TaxpayersView.vue'),
        meta: { title: '纳税人管理', permission: 'taxpayer:menu' },
      },
      {
        path: 'declaration-types',
        name: 'declaration-types',
        component: () => import('@/views/DeclarationTypesView.vue'),
        meta: { title: '申报类型', permission: 'declaration-type:menu' },
      },
      {
        path: 'form-templates',
        name: 'form-templates',
        component: () => import('@/views/FormTemplatesView.vue'),
        meta: { title: '表单模板', permission: 'form-template:menu' },
      },
      {
        path: 'declarations',
        name: 'declarations',
        component: () => import('@/views/DeclarationsView.vue'),
        meta: { title: '申报管理', permission: 'declaration:menu' },
      },
      {
        path: 'reviews',
        name: 'reviews',
        component: () => import('@/views/ReviewView.vue'),
        meta: { title: '审核管理', permission: 'review:menu' },
      },
      {
        path: 'search',
        name: 'search',
        component: () => import('@/views/SearchView.vue'),
        meta: { title: '综合查询', permission: 'search:menu' },
      },
      {
        path: 'statistics',
        name: 'statistics',
        component: () => import('@/views/StatisticsView.vue'),
        meta: { title: '统计分析', permission: 'statistics:menu' },
      },
      {
        path: 'audit-logs',
        name: 'audit-logs',
        component: () => import('@/views/AuditLogsView.vue'),
        meta: { title: '操作日志', permission: 'audit:menu' },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人设置' },
      },
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/views/ForbiddenView.vue'),
        meta: { title: '无权访问' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在', public: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  document.title = `${to.meta.title ?? '税务业务管理'} - 税务业务管理系统`
  const auth = useAuthStore()

  if (to.meta.public) {
    if (to.path === '/login' && auth.user) {
      return '/dashboard'
    }
    return true
  }

  if (!auth.user) {
    try {
      await auth.fetchCurrentUser()
    } catch {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.permission && !auth.hasPerm(to.meta.permission)) {
    return '/403'
  }
  return true
})

export default router

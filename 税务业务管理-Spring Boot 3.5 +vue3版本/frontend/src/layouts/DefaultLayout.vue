<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const mobileOpen = ref(false)
const isNarrow = ref(false)

const iconMap: Record<string, string> = {
  'dashboard:view': 'Odometer',
  'user:menu': 'User',
  'role:menu': 'Avatar',
  'taxpayer:menu': 'OfficeBuilding',
  'declaration-type:menu': 'CollectionTag',
  'form-template:menu': 'Files',
  'declaration:menu': 'Document',
  'review:menu': 'Stamp',
  'search:menu': 'Search',
  'statistics:menu': 'DataAnalysis',
  'audit:menu': 'Notebook',
}

const iconFor = (code: string) => iconMap[code] ?? 'Menu'
const currentTitle = computed(() => (route.meta.title as string | undefined) ?? '工作台')
const menuCollapsed = computed(() => collapsed.value && !isNarrow.value)
const roleLabelMap: Record<string, string> = {
  ADMIN: '系统管理员',
  CLERK: '业务经办人',
  REVIEWER: '审核人员',
  ANALYST: '统计分析员',
  VIEWER: '只读访客',
}
const currentRole = computed(() => auth.user?.roles?.map((role) => roleLabelMap[role] ?? role).join(' / ') || '-')

const groupedMenus = computed(() => {
  const definitions = [
    { label: '总览', codes: ['dashboard:view'] },
    {
      label: '业务办理',
      codes: ['declaration:menu', 'review:menu', 'search:menu', 'statistics:menu'],
    },
    {
      label: '基础数据',
      codes: ['taxpayer:menu', 'declaration-type:menu', 'form-template:menu'],
    },
    { label: '系统管理', codes: ['user:menu', 'role:menu', 'audit:menu'] },
  ]
  return definitions
    .map((group) => ({
      label: group.label,
      items: auth.menus
        .filter((menu) => group.codes.includes(menu.code))
        .map((menu) => (menu.code === 'dashboard:view' ? { ...menu, name: '分析页' } : menu)),
    }))
    .filter((group) => group.items.length)
})

function updateScreenState() {
  isNarrow.value = window.matchMedia('(max-width: 860px)').matches
  if (!isNarrow.value) mobileOpen.value = false
}

function toggleNavigation() {
  if (isNarrow.value) mobileOpen.value = !mobileOpen.value
  else collapsed.value = !collapsed.value
}

function closeMobileMenu() {
  if (isNarrow.value) mobileOpen.value = false
}

async function handleCommand(command: string) {
  if (command === 'profile') {
    await router.push('/profile')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning',
      })
    } catch {
      return
    }
    await auth.logout()
    await router.push('/login')
  }
}

watch(
  () => route.path,
  () => closeMobileMenu(),
)

onMounted(() => {
  updateScreenState()
  window.addEventListener('resize', updateScreenState)
})

onBeforeUnmount(() => window.removeEventListener('resize', updateScreenState))
</script>

<template>
  <div class="layout">
    <button
      v-if="mobileOpen"
      class="sidebar-backdrop"
      type="button"
      aria-label="关闭导航"
      @click="mobileOpen = false"
    />

    <aside class="app-sidebar" :class="{ collapsed: menuCollapsed, 'mobile-open': mobileOpen }">
      <div class="brand">
        <div class="brand-mark" aria-hidden="true">税</div>
        <div v-if="!menuCollapsed" class="brand-text">
          <strong>税务业务管理</strong>
          <span>教学演示系统 V1.0</span>
        </div>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="menuCollapsed"
          :collapse-transition="false"
          router
          @select="closeMobileMenu"
        >
          <el-menu-item-group v-for="group in groupedMenus" :key="group.label">
            <template #title>
              <span class="nav-group-title">{{ group.label }}</span>
            </template>
            <el-menu-item v-for="menu in group.items" :key="menu.code" :index="menu.path">
              <el-icon><component :is="iconFor(menu.code)" /></el-icon>
              <template #title>{{ menu.name }}</template>
            </el-menu-item>
          </el-menu-item-group>
        </el-menu>
      </el-scrollbar>

      <div v-if="!menuCollapsed" class="sidebar-foot">仅用于课程教学，全部数据为虚构演示数据。</div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="topbar-left">
          <el-button text class="collapse-btn" aria-label="展开或收起导航" @click="toggleNavigation">
            <el-icon :size="18"><component :is="menuCollapsed || mobileOpen ? 'Expand' : 'Fold'" /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>税务业务管理</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <span class="role-label"
            >当前角色：<strong>{{ currentRole }}</strong></span
          >
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-trigger" type="button">
              <span class="user-avatar">{{ auth.user?.displayName?.slice(0, 1) || '税' }}</span>
              <span class="user-meta">
                <strong>{{ auth.user?.displayName }}</strong>
              </span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  height: 100%;
  display: flex;
  overflow: hidden;
  background: var(--tax-bg);
}

.app-sidebar {
  width: 232px;
  flex: 0 0 232px;
  z-index: 40;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid #e8e8e8;
  background: #ffffff;
  transition:
    width 0.18s ease,
    flex-basis 0.18s ease,
    transform 0.2s ease;
}

.app-sidebar.collapsed {
  width: 64px;
  flex-basis: 64px;
}

.brand {
  height: 56px;
  flex: 0 0 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  padding: 0 17px;
  border-bottom: 1px solid var(--tax-border);
}

.brand-mark {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  background: var(--tax-primary);
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
}

.brand-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.25;
  white-space: nowrap;
}

.brand-text strong {
  color: var(--tax-text);
  font-size: 14px;
  font-weight: 600;
}

.brand-text span {
  margin-top: 2px;
  color: var(--tax-muted);
  font-size: 11px;
}

.menu-scroll {
  flex: 1;
  padding: 10px 10px 18px;
}

.nav-group-title {
  display: block;
  padding: 8px 10px 5px;
  color: var(--tax-muted);
  font-size: 12px;
  font-weight: 500;
}

:deep(.el-menu) {
  border-right: 0 !important;
}

:deep(.el-menu-item) {
  height: 38px;
  margin: 2px 0;
  padding: 0 12px !important;
  border-radius: 6px;
  color: var(--tax-text-secondary);
  font-size: 14px;
  line-height: 38px;
}

:deep(.el-menu-item:hover) {
  background: #f7f9fc;
  color: var(--tax-primary);
}

:deep(.el-menu-item.is-active) {
  background: var(--tax-primary-soft) !important;
  color: var(--tax-primary) !important;
  font-weight: 500;
}

:deep(.el-menu-item .el-icon) {
  width: 18px;
  margin-right: 11px;
  color: currentColor;
  font-size: 16px;
}

.sidebar-foot {
  padding: 14px 18px;
  border-top: 1px solid var(--tax-border);
  color: var(--tax-muted);
  font-size: 11px;
  line-height: 1.55;
}

.main {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 56px;
  flex: 0 0 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 24px 0 16px;
  border-bottom: 1px solid #e8e8e8;
  background: #ffffff;
}

.topbar-left,
.topbar-right {
  min-width: 0;
  display: flex;
  align-items: center;
}

.topbar-left {
  gap: 8px;
}

.topbar-right {
  gap: 18px;
}

.collapse-btn {
  color: var(--tax-text-secondary);
}

.role-label {
  padding-right: 18px;
  border-right: 1px solid #f0f0f0;
  color: var(--tax-muted);
  font-size: 12px;
}

.role-label strong {
  color: var(--tax-text);
  font-weight: 500;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 5px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--tax-text);
  cursor: pointer;
}

.user-trigger:hover {
  background: #fafafa;
}

.user-avatar {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--tax-primary-soft);
  color: var(--tax-primary);
  font-size: 12px;
  font-weight: 500;
}

.user-meta strong {
  font-size: 13px;
  font-weight: 500;
}

.content {
  min-height: 0;
  flex: 1;
  overflow: auto;
  background: var(--tax-bg);
}

.sidebar-backdrop {
  position: fixed;
  inset: 0;
  z-index: 35;
  border: 0;
  background: rgba(0, 0, 0, 0.32);
}

@media (max-width: 860px) {
  .app-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 40;
    width: 232px;
    flex-basis: 232px;
    transform: translateX(-100%);
    box-shadow: 8px 0 24px rgba(0, 0, 0, 0.08);
  }

  .app-sidebar.mobile-open {
    transform: translateX(0);
  }

  .topbar {
    padding: 0 16px;
  }

  .role-label {
    display: none;
  }
}

@media (max-width: 560px) {
  .topbar-right {
    gap: 6px;
  }

  .user-meta {
    display: none;
  }
}
</style>

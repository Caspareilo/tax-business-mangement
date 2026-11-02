<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { assignPermissions, createRole, fetchPermissions, fetchRoles, updateRole, type RolePayload } from '@/api/roles'
import type { Permission, Role } from '@/types/models'
import { formatDateTime } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<Role[]>([])
const total = ref(0)
const permissions = ref<Permission[]>([])
const query = reactive({ keyword: '', status: '', page: 1, pageSize: 20 })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ roleCode: '', roleName: '', description: '', status: 'ACTIVE' })
const rules: FormRules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]{1,63}$/, message: '大写字母开头，仅含大写字母、数字、下划线', trigger: 'blur' },
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

const permDialogVisible = ref(false)
const permTarget = ref<Role | null>(null)
const selectedPermissionIds = ref<number[]>([])
const permSubmitting = ref(false)

const menuPermissions = computed(() => permissions.value.filter((item) => item.permissionType === 'MENU'))
const apiPermissions = computed(() => permissions.value.filter((item) => item.permissionType === 'API'))

async function load() {
  loading.value = true
  try {
    const result = await fetchRoles({
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      status: query.status || undefined,
    })
    rows.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadPermissions() {
  try {
    permissions.value = await fetchPermissions()
  } catch {
    permissions.value = []
  }
}

function search() {
  query.page = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.status = ''
  search()
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, { roleCode: '', roleName: '', description: '', status: 'ACTIVE' })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row: Role) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    roleCode: row.roleCode,
    roleName: row.roleName,
    description: row.description ?? '',
    status: row.status,
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: RolePayload = {
      roleCode: form.roleCode.trim(),
      roleName: form.roleName.trim(),
      description: form.description.trim() || undefined,
      status: form.status,
    }
    if (dialogMode.value === 'create') {
      await createRole(payload)
      ElMessage.success('角色已创建')
    } else if (editingId.value) {
      await updateRole(editingId.value, payload)
      ElMessage.success('角色已更新')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

function openPermissions(row: Role) {
  permTarget.value = row
  selectedPermissionIds.value = [...row.permissionIds]
  permDialogVisible.value = true
}

function selectAll(ids: number[]) {
  const merged = new Set(selectedPermissionIds.value)
  ids.forEach((id) => merged.add(id))
  selectedPermissionIds.value = [...merged]
}

function clearAll(ids: number[]) {
  const removing = new Set(ids)
  selectedPermissionIds.value = selectedPermissionIds.value.filter((id) => !removing.has(id))
}

async function submitPermissions() {
  if (!permTarget.value) return
  permSubmitting.value = true
  try {
    await assignPermissions(permTarget.value.id, selectedPermissionIds.value)
    ElMessage.success('权限已保存')
    permDialogVisible.value = false
    await load()
  } finally {
    permSubmitting.value = false
  }
}

onMounted(() => {
  load()
  loadPermissions()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">角色权限</h1>
        <p class="page-description">维护角色定义，并为角色分配菜单与接口权限</p>
      </div>
      <el-button v-if="auth.hasPerm('role:write')" type="primary" @click="openCreate">新建角色</el-button>
    </div>

    <div
      class="filter-bar card"
      style="border-radius: var(--tax-radius); margin-bottom: 14px; border: 1px solid var(--tax-border)"
    >
      <div class="filter-item">
        <label>关键词</label>
        <el-input
          v-model="query.keyword"
          placeholder="角色编码或名称"
          clearable
          style="width: 210px"
          @keyup.enter="search"
        />
      </div>
      <div class="filter-item">
        <label>状态</label>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="roleCode" label="角色编码" width="150">
          <template #default="{ row }"
            ><span class="mono">{{ row.roleCode }}</span></template
          >
        </el-table-column>
        <el-table-column prop="roleName" label="角色名称" width="160" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span class="status-tag" :class="row.status === 'ACTIVE' ? 'status-approved' : 'status-disabled'">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" width="80" />
        <el-table-column label="权限数" width="90">
          <template #default="{ row }">{{ row.permissionIds.length }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="auth.hasPerm('role:write')">
              <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button text type="primary" size="small" @click="openPermissions(row)">分配权限</el-button>
            </template>
            <span v-else class="text-muted" style="font-size: 12px">只读</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="summary">共 {{ total }} 个角色</span>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建角色' : '编辑角色'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="dialogMode === 'edit'" placeholder="例如 AUDITOR" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="例如 稽核员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permDialogVisible" title="分配权限" width="760px" :close-on-click-modal="false">
      <p class="text-muted" style="margin-bottom: 16px; font-size: 13px">
        角色：<strong>{{ permTarget?.roleName }}</strong
        >（{{ permTarget?.roleCode }}） · 已选 {{ selectedPermissionIds.length }} 项
      </p>
      <div class="perm-section">
        <div class="perm-head">
          <span>菜单权限</span>
          <span class="toolbar-inline">
            <el-button text size="small" @click="selectAll(menuPermissions.map((item) => item.id))">全选</el-button>
            <el-button text size="small" @click="clearAll(menuPermissions.map((item) => item.id))">清空</el-button>
          </span>
        </div>
        <el-checkbox-group v-model="selectedPermissionIds" class="perm-grid">
          <el-checkbox v-for="perm in menuPermissions" :key="perm.id" :value="perm.id">
            {{ perm.permissionName }} <span class="text-muted mono">{{ perm.permissionCode }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <div class="perm-section">
        <div class="perm-head">
          <span>接口权限</span>
          <span class="toolbar-inline">
            <el-button text size="small" @click="selectAll(apiPermissions.map((item) => item.id))">全选</el-button>
            <el-button text size="small" @click="clearAll(apiPermissions.map((item) => item.id))">清空</el-button>
          </span>
        </div>
        <el-checkbox-group v-model="selectedPermissionIds" class="perm-grid">
          <el-checkbox v-for="perm in apiPermissions" :key="perm.id" :value="perm.id">
            {{ perm.permissionName }} <span class="text-muted mono">{{ perm.permissionCode }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitting" @click="submitPermissions">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.perm-section {
  border: 1px solid var(--tax-border);
  border-radius: var(--tax-radius);
  padding: 14px 16px;
  margin-bottom: 14px;
}

.perm-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
}

.perm-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 12px;
}

.perm-grid :deep(.el-checkbox) {
  height: 30px;
  margin-right: 0;
}
</style>

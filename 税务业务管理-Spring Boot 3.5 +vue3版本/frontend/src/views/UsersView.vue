<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createUser,
  deleteUser,
  fetchUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus,
  type UserPayload,
} from '@/api/users'
import { fetchRoles } from '@/api/roles'
import type { Role, User } from '@/types/models'
import { formatDateTime, USER_STATUS } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<User[]>([])
const total = ref(0)
const roles = ref<Role[]>([])

const query = reactive({
  keyword: '',
  status: '',
  roleId: null as number | null,
  page: 1,
  pageSize: 10,
})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  displayName: '',
  status: 'ACTIVE',
  remark: '',
  roleIds: [] as number[],
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_]{2,63}$/,
      message: '账号需以字母开头，3-64 位字母、数字或下划线',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/, message: '密码需为 8-64 位且同时包含字母和数字', trigger: 'blur' },
  ],
  displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  roleIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个角色', trigger: 'change' }],
}

const resetDialogVisible = ref(false)
const resetTarget = ref<User | null>(null)
const resetFormRef = ref<FormInstance>()
const resetSubmitting = ref(false)
const resetForm = reactive({ newPassword: '', confirmPassword: '' })
const resetRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/, message: '密码需为 8-64 位且同时包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== resetForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function load() {
  loading.value = true
  try {
    const result = await fetchUsers({
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      status: query.status || undefined,
      roleId: query.roleId ?? undefined,
    })
    rows.value = result.items
    total.value = result.total
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const result = await fetchRoles({ page: 1, pageSize: 100, status: 'ACTIVE' })
    roles.value = result.items
  } catch {
    roles.value = []
  }
}

function search() {
  query.page = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.status = ''
  query.roleId = null
  query.page = 1
  load()
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, { username: '', password: '', displayName: '', status: 'ACTIVE', remark: '', roleIds: [] })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row: User) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    password: '',
    displayName: row.displayName,
    status: row.status,
    remark: row.remark ?? '',
    roleIds: row.roles.map((role) => role.id),
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: UserPayload = {
      displayName: form.displayName.trim(),
      status: form.status,
      remark: form.remark.trim() || undefined,
      roleIds: form.roleIds,
    }
    if (dialogMode.value === 'create') {
      await createUser({ ...payload, username: form.username.trim(), password: form.password })
      ElMessage.success('用户已创建')
    } else if (editingId.value) {
      await updateUser(editingId.value, payload)
      ElMessage.success('用户信息已更新')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: User) {
  const next = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await ElMessageBox.confirm(
      `确认${next === 'ACTIVE' ? '启用' : '停用'}账号「${row.username}」？`,
      next === 'ACTIVE' ? '启用账号' : '停用账号',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  await updateUserStatus(row.id, next)
  ElMessage.success(next === 'ACTIVE' ? '账号已启用' : '账号已停用')
  await load()
}

function openReset(row: User) {
  resetTarget.value = row
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  resetDialogVisible.value = true
  resetFormRef.value?.clearValidate()
}

async function submitReset() {
  const valid = await resetFormRef.value?.validate().catch(() => false)
  if (!valid || !resetTarget.value) return
  resetSubmitting.value = true
  try {
    await resetUserPassword(resetTarget.value.id, resetForm.newPassword)
    ElMessage.success('密码已重置')
    resetDialogVisible.value = false
  } finally {
    resetSubmitting.value = false
  }
}

async function remove(row: User) {
  try {
    await ElMessageBox.confirm(`删除账号「${row.username}」后不可恢复，确认继续？`, '删除账号', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await deleteUser(row.id)
  ElMessage.success('账号已删除')
  await load()
}

onMounted(() => {
  load()
  loadRoles()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-description">维护系统登录账号、角色分配与账号状态</p>
      </div>
      <el-button v-if="auth.hasPerm('user:write')" type="primary" @click="openCreate">新建用户</el-button>
    </div>

    <div
      class="filter-bar card"
      style="border-radius: var(--tax-radius); margin-bottom: 14px; border: 1px solid var(--tax-border)"
    >
      <div class="filter-item">
        <label>关键词</label>
        <el-input
          v-model="query.keyword"
          placeholder="账号或显示名称"
          clearable
          style="width: 200px"
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
      <div class="filter-item">
        <label>角色</label>
        <el-select v-model="query.roleId" placeholder="全部角色" clearable style="width: 160px">
          <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="username" label="登录账号" width="130">
          <template #default="{ row }"
            ><span class="mono">{{ row.username }}</span></template
          >
        </el-table-column>
        <el-table-column prop="displayName" label="显示名称" min-width="140" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.id" size="small" effect="plain" style="margin-right: 4px">{{
              role.roleName
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span class="status-tag" :class="row.status === 'ACTIVE' ? 'status-approved' : 'status-disabled'">
              {{ USER_STATUS[row.status] ?? row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <template v-if="auth.hasPerm('user:write')">
              <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button text type="primary" size="small" @click="toggleStatus(row)">{{
                row.status === 'ACTIVE' ? '停用' : '启用'
              }}</el-button>
              <el-button text type="primary" size="small" @click="openReset(row)">重置密码</el-button>
              <el-button text type="danger" size="small" :disabled="row.id === auth.user?.id" @click="remove(row)"
                >删除</el-button
              >
            </template>
            <span v-else class="text-muted" style="font-size: 12px">只读</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="summary">共 {{ total }} 个账号</span>
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
      :title="dialogMode === 'create' ? '新建用户' : '编辑用户'"
      width="560px"
      :close-on-click-modal="false"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="dialogMode === 'edit'" placeholder="字母开头，3-64 位" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="8-64 位，含字母和数字" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="例如：经办人·张样例" />
        </el-form-item>
        <el-form-item label="账号状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="`${role.roleName}（${role.roleCode}）`"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetDialogVisible" title="重置密码" width="440px" :close-on-click-modal="false">
      <p class="text-muted" style="margin-bottom: 14px; font-size: 13px">
        将重置账号「{{ resetTarget?.username }}」的登录密码，请确认已获得操作授权。
      </p>
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="90px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="8-64 位，含字母和数字" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="resetForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetSubmitting" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createDeclarationType,
  fetchDeclarationTypes,
  updateDeclarationType,
  updateDeclarationTypeStatus,
  type DeclarationTypePayload,
} from '@/api/declarationTypes'
import type { DeclarationType } from '@/types/models'
import { formatDateTime } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<DeclarationType[]>([])
const total = ref(0)
const query = reactive({ keyword: '', enabled: null as boolean | null, page: 1, pageSize: 20 })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ typeCode: '', typeName: '', description: '' })
const rules: FormRules = {
  typeCode: [
    { required: true, message: '请输入类型编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]{2,63}$/, message: '需为大写字母开头的大写字母、数字或下划线', trigger: 'blur' },
  ],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const result = await fetchDeclarationTypes({
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      enabled: query.enabled,
    })
    rows.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.enabled = null
  search()
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, { typeCode: '', typeName: '', description: '' })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row: DeclarationType) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, { typeCode: row.typeCode, typeName: row.typeName, description: row.description ?? '' })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: DeclarationTypePayload = {
      typeCode: form.typeCode.trim(),
      typeName: form.typeName.trim(),
      description: form.description.trim() || undefined,
    }
    if (dialogMode.value === 'create') {
      await createDeclarationType(payload)
      ElMessage.success('申报类型已创建')
    } else if (editingId.value) {
      await updateDeclarationType(editingId.value, payload)
      ElMessage.success('申报类型已更新')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function toggleEnabled(row: DeclarationType) {
  try {
    await ElMessageBox.confirm(
      `确认${row.enabled ? '停用' : '启用'}申报类型「${row.typeName}」？停用后不可用于新建申报。`,
      '变更启用状态',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  await updateDeclarationTypeStatus(row.id, !row.enabled)
  ElMessage.success('启用状态已更新')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">申报类型</h1>
        <p class="page-description">维护教学模拟的申报类型，并与表单模板关联</p>
      </div>
      <el-button v-if="auth.hasPerm('declaration-type:write')" type="primary" @click="openCreate"
        >新建申报类型</el-button
      >
    </div>

    <div
      class="filter-bar card"
      style="border-radius: var(--tax-radius); margin-bottom: 14px; border: 1px solid var(--tax-border)"
    >
      <div class="filter-item">
        <label>关键词</label>
        <el-input
          v-model="query.keyword"
          placeholder="类型编码或名称"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
      </div>
      <div class="filter-item">
        <label>启用状态</label>
        <el-select v-model="query.enabled" placeholder="全部" clearable style="width: 140px">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="typeCode" label="类型编码" width="190">
          <template #default="{ row }"
            ><span class="mono">{{ row.typeCode }}</span></template
          >
        </el-table-column>
        <el-table-column prop="typeName" label="类型名称" width="160" />
        <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="defaultTemplateName" label="当前默认模板" min-width="170">
          <template #default="{ row }">{{ row.defaultTemplateName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="enabled" label="启用状态" width="100">
          <template #default="{ row }">
            <span class="status-tag" :class="row.enabled ? 'status-approved' : 'status-disabled'">{{
              row.enabled ? '启用' : '停用'
            }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="auth.hasPerm('declaration-type:write')">
              <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button text type="primary" size="small" @click="toggleEnabled(row)">{{
                row.enabled ? '停用' : '启用'
              }}</el-button>
            </template>
            <span v-else class="text-muted" style="font-size: 12px">只读</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="summary">共 {{ total }} 个申报类型</span>
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
      :title="dialogMode === 'create' ? '新建申报类型' : '编辑申报类型'"
      width="540px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model="form.typeCode" :disabled="dialogMode === 'edit'" placeholder="例如 MOCK_SPECIAL" />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="form.typeName" placeholder="例如 模拟专项申报" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请注明为教学模拟口径"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

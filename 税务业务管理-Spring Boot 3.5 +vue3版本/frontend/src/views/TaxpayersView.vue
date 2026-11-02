<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createTaxpayer,
  deleteTaxpayer,
  fetchTaxpayerDeclarations,
  fetchTaxpayers,
  updateTaxpayer,
  updateTaxpayerStatus,
  type TaxpayerPayload,
} from '@/api/taxpayers'
import type { Declaration, Taxpayer } from '@/types/models'
import { declarationStatus, formatAmount, formatDateTime, TAXPAYER_TYPE } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<Taxpayer[]>([])
const total = ref(0)
const query = reactive({ keyword: '', type: '', status: '', page: 1, pageSize: 10 })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  taxpayerCode: '',
  taxpayerName: '',
  taxpayerType: 'ENTERPRISE',
  contactName: '',
  contactPhone: '',
  remark: '',
})
const rules: FormRules = {
  taxpayerCode: [
    { required: true, message: '请输入纳税人编号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]{3,64}$/, message: '编号只能包含字母、数字、下划线或短横线', trigger: 'blur' },
  ],
  taxpayerName: [{ required: true, message: '请输入纳税人名称', trigger: 'blur' }],
  taxpayerType: [{ required: true, message: '请选择主体类型', trigger: 'change' }],
  contactPhone: [{ pattern: /^$|^[0-9+\-]{5,40}$/, message: '联系电话格式不正确', trigger: 'blur' }],
}

const drawerVisible = ref(false)
const drawerTaxpayer = ref<Taxpayer | null>(null)
const declLoading = ref(false)
const declRows = ref<Declaration[]>([])
const declTotal = ref(0)
const declPage = reactive({ page: 1, pageSize: 5 })

async function load() {
  loading.value = true
  try {
    const result = await fetchTaxpayers({
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      type: query.type || undefined,
      status: query.status || undefined,
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
  query.type = ''
  query.status = ''
  query.page = 1
  load()
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, {
    taxpayerCode: '',
    taxpayerName: '',
    taxpayerType: 'ENTERPRISE',
    contactName: '',
    contactPhone: '',
    remark: '',
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row: Taxpayer) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    taxpayerCode: row.taxpayerCode,
    taxpayerName: row.taxpayerName,
    taxpayerType: row.taxpayerType,
    contactName: row.contactName ?? '',
    contactPhone: row.contactPhone ?? '',
    remark: row.remark ?? '',
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: TaxpayerPayload = {
      taxpayerCode: form.taxpayerCode.trim(),
      taxpayerName: form.taxpayerName.trim(),
      taxpayerType: form.taxpayerType,
      contactName: form.contactName.trim() || undefined,
      contactPhone: form.contactPhone.trim() || undefined,
      remark: form.remark.trim() || undefined,
    }
    if (dialogMode.value === 'create') {
      await createTaxpayer(payload)
      ElMessage.success('纳税人档案已创建')
    } else if (editingId.value) {
      await updateTaxpayer(editingId.value, payload)
      ElMessage.success('纳税人档案已更新')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: Taxpayer) {
  const next = row.registrationStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await ElMessageBox.confirm(
      `确认${next === 'ACTIVE' ? '启用' : '停用'}纳税人「${row.taxpayerName}」？停用后不能为其新建申报。`,
      '变更登记状态',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  await updateTaxpayerStatus(row.id, next)
  ElMessage.success('登记状态已更新')
  await load()
}

async function remove(row: Taxpayer) {
  try {
    await ElMessageBox.confirm(`确认删除纳税人「${row.taxpayerName}」？`, '删除纳税人', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await deleteTaxpayer(row.id)
  ElMessage.success('纳税人已删除')
  await load()
}

async function openDeclarations(row: Taxpayer) {
  drawerTaxpayer.value = row
  declPage.page = 1
  drawerVisible.value = true
  await loadDeclarations()
}

async function loadDeclarations() {
  if (!drawerTaxpayer.value) return
  declLoading.value = true
  try {
    const result = await fetchTaxpayerDeclarations(drawerTaxpayer.value.id, declPage)
    declRows.value = result.items
    declTotal.value = result.total
  } catch {
    declRows.value = []
    declTotal.value = 0
  } finally {
    declLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">纳税人管理</h1>
        <p class="page-description">维护虚构的纳税人档案，停用主体不可用于新建申报</p>
      </div>
      <el-button v-if="auth.hasPerm('taxpayer:write')" type="primary" @click="openCreate">新建纳税人</el-button>
    </div>

    <div
      class="filter-bar card"
      style="border-radius: var(--tax-radius); margin-bottom: 14px; border: 1px solid var(--tax-border)"
    >
      <div class="filter-item">
        <label>关键词</label>
        <el-input
          v-model="query.keyword"
          placeholder="编号或名称"
          clearable
          style="width: 210px"
          @keyup.enter="search"
        />
      </div>
      <div class="filter-item">
        <label>主体类型</label>
        <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 150px">
          <el-option v-for="(label, value) in TAXPAYER_TYPE" :key="value" :label="label" :value="value" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>登记状态</label>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="正常" value="ACTIVE" />
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
        <el-table-column prop="taxpayerCode" label="纳税人编号" width="140">
          <template #default="{ row }"
            ><span class="mono">{{ row.taxpayerCode }}</span></template
          >
        </el-table-column>
        <el-table-column prop="taxpayerName" label="纳税人名称" min-width="220" />
        <el-table-column prop="taxpayerType" label="主体类型" width="100">
          <template #default="{ row }">{{ TAXPAYER_TYPE[row.taxpayerType] ?? row.taxpayerType }}</template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="registrationStatus" label="登记状态" width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :class="row.registrationStatus === 'ACTIVE' ? 'status-approved' : 'status-disabled'"
            >
              {{ row.registrationStatus === 'ACTIVE' ? '正常' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="auth.hasPerm('declaration:read')"
              text
              type="primary"
              size="small"
              @click="openDeclarations(row)"
              >申报记录</el-button
            >
            <template v-if="auth.hasPerm('taxpayer:write')">
              <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button text type="primary" size="small" @click="toggleStatus(row)">{{
                row.registrationStatus === 'ACTIVE' ? '停用' : '启用'
              }}</el-button>
              <el-button text type="danger" size="small" @click="remove(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="summary">共 {{ total }} 个纳税人</span>
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
      :title="dialogMode === 'create' ? '新建纳税人' : '编辑纳税人'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="纳税人编号" prop="taxpayerCode">
          <el-input v-model="form.taxpayerCode" placeholder="例如 TP20260007" />
        </el-form-item>
        <el-form-item label="纳税人名称" prop="taxpayerName">
          <el-input v-model="form.taxpayerName" placeholder="请使用虚构名称" />
        </el-form-item>
        <el-form-item label="主体类型" prop="taxpayerType">
          <el-radio-group v-model="form.taxpayerType">
            <el-radio value="ENTERPRISE">企业</el-radio>
            <el-radio value="INDIVIDUAL">个人</el-radio>
            <el-radio value="OTHER">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" maxlength="100" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="演示用途，请勿填写真实号码" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawerVisible" :title="`申报记录 · ${drawerTaxpayer?.taxpayerName ?? ''}`" size="720px">
      <el-table v-loading="declLoading" :data="declRows" size="small">
        <el-table-column prop="declarationNo" label="申报编号" width="160">
          <template #default="{ row }"
            ><span class="mono">{{ row.declarationNo }}</span></template
          >
        </el-table-column>
        <el-table-column prop="declarationTypeName" label="申报类型" width="120" />
        <el-table-column label="期间" width="180">
          <template #default="{ row }">{{ row.periodStart }} ~ {{ row.periodEnd }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-tag" :class="declarationStatus(row.status).className">{{
              declarationStatus(row.status).label
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }"
            ><span class="amount">{{ formatAmount(row.declaredAmount) }}</span></template
          >
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <div v-if="!declLoading && !declRows.length" class="empty-state">该纳税人暂无申报记录</div>
      <div class="table-footer" style="border-top: none">
        <span class="summary">共 {{ declTotal }} 条</span>
        <el-pagination
          v-model:current-page="declPage.page"
          v-model:page-size="declPage.pageSize"
          :total="declTotal"
          :page-sizes="[5, 10, 20]"
          layout="sizes, prev, pager, next"
          small
          @current-change="loadDeclarations"
          @size-change="loadDeclarations"
        />
      </div>
    </el-drawer>
  </div>
</template>

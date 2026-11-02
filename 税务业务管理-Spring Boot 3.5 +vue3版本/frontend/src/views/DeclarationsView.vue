<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import {
  createDeclaration,
  fetchDeclaration,
  fetchDeclarations,
  submitDeclaration,
  updateDeclaration,
  withdrawDeclaration,
  type DeclarationPayload,
} from '@/api/declarations'
import { fetchTaxpayerOptions } from '@/api/taxpayers'
import { fetchDeclarationTypeOptions } from '@/api/declarationTypes'
import { fetchTemplate, fetchTemplates } from '@/api/formTemplates'
import type { Declaration, DeclarationType, FieldDef, Taxpayer } from '@/types/models'
import {
  declarationStatus,
  DECLARATION_STATUS,
  formatAmount,
  formatDateTime,
  formatFormValue,
  REVIEW_ACTION,
} from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const rows = ref<Declaration[]>([])
const total = ref(0)
const taxpayers = ref<Taxpayer[]>([])
const types = ref<DeclarationType[]>([])

const query = reactive({
  keyword: '',
  declarationNo: '',
  declarationTypeId: null as number | null,
  status: '',
  page: 1,
  pageSize: 10,
})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const editingDeclaration = ref<Declaration | null>(null)
const submitting = ref(false)
const templateLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  taxpayerId: null as number | null,
  declarationTypeId: null as number | null,
  periodRange: [] as string[],
  declaredAmount: 0,
  formData: {} as Record<string, unknown>,
  templateId: null as number | null,
  fields: [] as FieldDef[],
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<Declaration | null>(null)

const activeTaxpayers = computed(() => taxpayers.value.filter((item) => item.registrationStatus === 'ACTIVE'))
const activeTypes = computed(() => types.value.filter((item) => item.enabled))

async function load() {
  loading.value = true
  try {
    const result = await fetchDeclarations({
      keyword: query.keyword.trim() || undefined,
      declarationNo: query.declarationNo.trim() || undefined,
      declarationTypeId: query.declarationTypeId,
      status: query.status || undefined,
      page: query.page,
      pageSize: query.pageSize,
    })
    rows.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [taxpayerResult, typeResult] = await Promise.all([fetchTaxpayerOptions(), fetchDeclarationTypeOptions()])
    taxpayers.value = taxpayerResult
    types.value = typeResult
  } catch {
    taxpayers.value = []
    types.value = []
  }
}

function search() {
  query.page = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.declarationNo = ''
  query.declarationTypeId = null
  query.status = ''
  query.page = 1
  load()
}

function setFieldValue(code: string, value: unknown) {
  form.formData[code] = value
}

function numberValue(code: string): number | undefined {
  const value = form.formData[code]
  return typeof value === 'number' ? value : undefined
}

function stringValue(code: string): string {
  const value = form.formData[code]
  return value === null || value === undefined ? '' : String(value)
}

function arrayValue(code: string): string[] {
  const value = form.formData[code]
  return Array.isArray(value) ? (value as string[]) : []
}

function boolValue(code: string): boolean {
  return Boolean(form.formData[code])
}

function initialFormData(fields: FieldDef[]): Record<string, unknown> {
  const data: Record<string, unknown> = {}
  fields.forEach((field) => {
    if (field.fieldType === 'BOOLEAN') data[field.fieldCode] = false
    else if (field.fieldType === 'MULTI_SELECT') data[field.fieldCode] = []
    else data[field.fieldCode] = ''
  })
  return data
}

async function loadTemplateForType(typeId: number, resetData: boolean): Promise<void> {
  templateLoading.value = true
  try {
    const result = await fetchTemplates({ declarationTypeId: typeId, status: 'PUBLISHED', page: 1, pageSize: 100 })
    const latest = [...result.items].sort((a, b) => b.versionNo - a.versionNo)[0]
    if (!latest) {
      form.fields = []
      form.templateId = null
      ElMessage.warning('该申报类型尚未发布表单模板，无法录入申报')
      return
    }
    const template = await fetchTemplate(latest.id)
    form.templateId = template.id
    form.fields = template.fields.filter((field) => field.enabled)
    if (resetData) {
      form.formData = initialFormData(form.fields)
    } else {
      const normalized: Record<string, unknown> = {}
      form.fields.forEach((field) => {
        const value = form.formData[field.fieldCode]
        normalized[field.fieldCode] = value ?? initialFormData([field])[field.fieldCode]
      })
      form.formData = normalized
    }
  } finally {
    templateLoading.value = false
  }
}

async function onTypeChange(typeId: number | null) {
  if (!typeId) {
    form.fields = []
    form.templateId = null
    form.formData = {}
    return
  }
  await loadTemplateForType(typeId, true)
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  editingDeclaration.value = null
  Object.assign(form, {
    taxpayerId: null,
    declarationTypeId: null,
    periodRange: [],
    declaredAmount: 0,
    formData: {},
    templateId: null,
    fields: [],
  })
  dialogVisible.value = true
  setTimeout(() => formRef.value?.clearValidate(), 0)
}

async function openEdit(row: Declaration) {
  if (!['DRAFT', 'RETURNED'].includes(row.status)) {
    ElMessage.warning('只有草稿或已退回的申报可以编辑')
    return
  }
  dialogMode.value = 'edit'
  editingId.value = row.id
  detailLoading.value = true
  dialogVisible.value = true
  try {
    const result = await fetchDeclaration(row.id)
    editingDeclaration.value = result
    form.taxpayerId = result.taxpayerId
    form.declarationTypeId = result.declarationTypeId
    form.periodRange = [result.periodStart, result.periodEnd]
    form.declaredAmount = Number(result.declaredAmount ?? 0)
    form.templateId = result.templateId
    form.fields = result.fields.filter((field) => field.enabled)
    form.formData = { ...initialFormData(form.fields), ...(result.formData ?? {}) }
  } finally {
    detailLoading.value = false
  }
}

function validateForm(): string | null {
  if (!form.taxpayerId) return '请选择纳税人'
  if (!form.declarationTypeId) return '请选择申报类型'
  if (!form.periodRange || form.periodRange.length !== 2) return '请选择申报期间'
  if (!form.templateId) return '该申报类型缺少可用模板'
  for (const field of form.fields) {
    if (!field.required) continue
    const value = form.formData[field.fieldCode]
    const empty = value === undefined || value === null || value === '' || (Array.isArray(value) && value.length === 0)
    if (empty) return `请填写必填字段「${field.fieldLabel}」`
  }
  return null
}

async function save(submitAfterSave: boolean) {
  const error = validateForm()
  if (error) {
    ElMessage.warning(error)
    return
  }
  submitting.value = true
  try {
    const payload: DeclarationPayload = {
      taxpayerId: form.taxpayerId as number,
      declarationTypeId: form.declarationTypeId as number,
      periodStart: form.periodRange[0],
      periodEnd: form.periodRange[1],
      declaredAmount: form.declaredAmount ?? 0,
      formData: form.formData,
    }
    let saved: Declaration
    if (dialogMode.value === 'create') {
      saved = await createDeclaration(payload)
      ElMessage.success('申报草稿已创建')
    } else if (editingId.value) {
      saved = await updateDeclaration(editingId.value, payload)
      ElMessage.success('申报草稿已更新')
    } else {
      return
    }
    if (submitAfterSave) {
      await submitDeclaration(saved.id)
      ElMessage.success('申报已提交审核')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function submitRow(row: Declaration) {
  try {
    await ElMessageBox.confirm(`确认提交申报「${row.declarationNo}」进入审核？`, '提交申报', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await submitDeclaration(row.id)
  ElMessage.success('申报已提交')
  await load()
}

async function withdrawRow(row: Declaration) {
  try {
    await ElMessageBox.confirm(`确认撤回申报「${row.declarationNo}」？撤回后回到可编辑状态。`, '撤回申报', {
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await withdrawDeclaration(row.id)
  ElMessage.success('申报已撤回')
  await load()
}

async function openDetail(row: Declaration) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchDeclaration(row.id)
  } finally {
    detailLoading.value = false
  }
}

function goReview(row: Declaration) {
  router.push({ path: '/reviews', query: { keyword: row.declarationNo } })
}

async function applyRouteQuery() {
  if (typeof route.query.status === 'string') query.status = route.query.status
  if (typeof route.query.declarationNo === 'string') query.declarationNo = route.query.declarationNo
  if (route.query.create === '1' && auth.hasPerm('declaration:write')) {
    openCreate()
    const nextQuery = { ...route.query }
    delete nextQuery.create
    await router.replace({ path: route.path, query: nextQuery })
  }
}

onMounted(async () => {
  await loadOptions()
  await applyRouteQuery()
  await load()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">申报管理</h1>
        <p class="page-description">申报草稿保存后提交审核；同一纳税人、类型、期间只允许一条有效申报</p>
      </div>
      <el-button v-if="auth.hasPerm('declaration:write')" type="primary" @click="openCreate">新建申报</el-button>
    </div>

    <div
      class="filter-bar card"
      style="border-radius: var(--tax-radius); margin-bottom: 14px; border: 1px solid var(--tax-border)"
    >
      <div class="filter-item">
        <label>关键词</label>
        <el-input
          v-model="query.keyword"
          placeholder="纳税人或编号"
          clearable
          style="width: 190px"
          @keyup.enter="search"
        />
      </div>
      <div class="filter-item">
        <label>申报编号</label>
        <el-input
          v-model="query.declarationNo"
          placeholder="精确编号"
          clearable
          style="width: 180px"
          @keyup.enter="search"
        />
      </div>
      <div class="filter-item">
        <label>申报类型</label>
        <el-select v-model="query.declarationTypeId" placeholder="全部类型" clearable style="width: 160px">
          <el-option v-for="type in types" :key="type.id" :label="type.typeName" :value="type.id" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>状态</label>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="(item, value) in DECLARATION_STATUS" :key="value" :label="item.label" :value="value" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="declarationNo" label="申报编号" width="165">
          <template #default="{ row }"
            ><span class="mono">{{ row.declarationNo }}</span></template
          >
        </el-table-column>
        <el-table-column prop="taxpayerName" label="纳税人" min-width="170" show-overflow-tooltip />
        <el-table-column prop="declarationTypeName" label="申报类型" width="120" />
        <el-table-column label="申报期间" width="180">
          <template #default="{ row }"
            ><span class="period-text">{{ row.periodStart }} ~ {{ row.periodEnd }}</span></template
          >
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-tag" :class="declarationStatus(row.status).className">{{
              declarationStatus(row.status).label
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="模拟金额" width="125" align="right">
          <template #default="{ row }"
            ><span class="amount">{{ formatAmount(row.declaredAmount) }}</span></template
          >
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <template v-if="auth.hasPerm('declaration:write') && ['DRAFT', 'RETURNED'].includes(row.status)">
              <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            </template>
            <template v-if="auth.hasPerm('declaration:submit') && ['DRAFT', 'RETURNED'].includes(row.status)">
              <el-button text type="success" size="small" @click="submitRow(row)">提交</el-button>
            </template>
            <template v-if="auth.hasPerm('declaration:withdraw') && row.status === 'PENDING_REVIEW'">
              <el-button text type="warning" size="small" @click="withdrawRow(row)">撤回</el-button>
            </template>
            <template v-if="auth.hasPerm('review:write') && row.status === 'PENDING_REVIEW'">
              <el-button text type="primary" size="small" @click="goReview(row)">去审核</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="summary">共 {{ total }} 条申报</span>
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
      <div v-if="!loading && !rows.length" class="empty-state">
        暂无符合条件的申报
        <div v-if="auth.hasPerm('declaration:write')" style="margin-top: 10px">
          <el-button size="small" @click="openCreate">新建申报</el-button>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建申报' : '编辑申报草稿'"
      width="880px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div v-loading="templateLoading || detailLoading">
        <el-form ref="formRef" :model="form" label-width="150px">
          <div class="form-section-title">基本信息</div>
          <div class="form-grid">
            <el-form-item label="纳税人" required>
              <el-select v-model="form.taxpayerId" filterable placeholder="请选择纳税人" style="width: 100%">
                <el-option
                  v-for="item in activeTaxpayers"
                  :key="item.id"
                  :label="`${item.taxpayerName}（${item.taxpayerCode}）`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="申报类型" required>
              <el-select
                v-model="form.declarationTypeId"
                placeholder="请选择申报类型"
                style="width: 100%"
                :disabled="dialogMode === 'edit'"
                @change="onTypeChange"
              >
                <el-option v-for="item in activeTypes" :key="item.id" :label="item.typeName" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="申报期间" required>
              <el-date-picker
                v-model="form.periodRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                range-separator="至"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="模拟金额">
              <el-input-number
                v-model="form.declaredAmount"
                :min="0"
                :precision="2"
                :controls="false"
                style="width: 100%"
              />
            </el-form-item>
          </div>

          <div class="form-section-title" style="margin-top: 8px">
            申报表单
            <span v-if="!form.templateId" class="text-muted" style="font-weight: 400; font-size: 12px"
              >（请先选择申报类型）</span
            >
          </div>

          <div v-if="form.fields.length" class="form-render">
            <div class="form-grid">
              <el-form-item
                v-for="field in form.fields"
                :key="field.fieldCode"
                :label="field.fieldLabel"
                :required="field.required"
                :class="{ 'full-row': ['TEXT', 'MULTI_SELECT', 'RADIO'].includes(field.fieldType) }"
              >
                <el-input
                  v-if="field.fieldType === 'TEXT'"
                  :model-value="stringValue(field.fieldCode)"
                  :maxlength="field.maxLength ?? undefined"
                  show-word-limit
                  :placeholder="field.helpText || '请输入'"
                  @update:model-value="setFieldValue(field.fieldCode, $event)"
                />
                <el-input-number
                  v-else-if="field.fieldType === 'INTEGER'"
                  :model-value="numberValue(field.fieldCode)"
                  :precision="0"
                  :min="field.minValue != null ? Number(field.minValue) : undefined"
                  :max="field.maxValue != null ? Number(field.maxValue) : undefined"
                  :controls="false"
                  style="width: 100%"
                  @update:model-value="setFieldValue(field.fieldCode, $event)"
                />
                <el-input-number
                  v-else-if="field.fieldType === 'DECIMAL'"
                  :model-value="numberValue(field.fieldCode)"
                  :precision="2"
                  :min="field.minValue != null ? Number(field.minValue) : undefined"
                  :max="field.maxValue != null ? Number(field.maxValue) : undefined"
                  :controls="false"
                  style="width: 100%"
                  @update:model-value="setFieldValue(field.fieldCode, $event)"
                />
                <el-date-picker
                  v-else-if="field.fieldType === 'DATE'"
                  :model-value="stringValue(field.fieldCode)"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择日期"
                  style="width: 100%"
                  @update:model-value="setFieldValue(field.fieldCode, $event)"
                />
                <el-radio-group
                  v-else-if="field.fieldType === 'RADIO'"
                  :model-value="stringValue(field.fieldCode)"
                  @update:model-value="setFieldValue(field.fieldCode, $event)"
                >
                  <el-radio v-for="option in field.options" :key="option.value" :value="option.value">{{
                    option.label
                  }}</el-radio>
                </el-radio-group>
                <el-checkbox-group
                  v-else-if="field.fieldType === 'MULTI_SELECT'"
                  :model-value="arrayValue(field.fieldCode)"
                  @update:model-value="setFieldValue(field.fieldCode, $event)"
                >
                  <el-checkbox v-for="option in field.options" :key="option.value" :value="option.value">{{
                    option.label
                  }}</el-checkbox>
                </el-checkbox-group>
                <el-switch
                  v-else-if="field.fieldType === 'BOOLEAN'"
                  :model-value="boolValue(field.fieldCode)"
                  @update:model-value="setFieldValue(field.fieldCode, $event)"
                />
                <div v-if="field.helpText" class="text-muted" style="font-size: 11px; width: 100%">
                  {{ field.helpText }}
                </div>
              </el-form-item>
            </div>
          </div>
          <div v-else class="empty-state" style="padding: 20px 0">选择申报类型后自动加载已发布模板字段</div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="submitting" @click="save(false)">保存草稿</el-button>
        <el-button type="primary" :loading="submitting" @click="save(true)">保存并提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="申报详情" size="720px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <div class="detail-head">
            <div>
              <div class="mono" style="font-size: 15px">{{ detail.declarationNo }}</div>
              <div class="text-muted" style="font-size: 12px; margin-top: 4px">
                创建于 {{ formatDateTime(detail.createdAt) }}
              </div>
            </div>
            <span class="status-tag" :class="declarationStatus(detail.status).className" style="font-size: 14px">
              {{ declarationStatus(detail.status).label }}
            </span>
          </div>

          <div class="form-section-title" style="margin-top: 20px">基本信息</div>
          <dl class="detail-list">
            <dt>纳税人</dt>
            <dd>
              {{ detail.taxpayerName }} <span class="mono text-muted">{{ detail.taxpayerCode }}</span>
            </dd>
            <dt>申报类型</dt>
            <dd>{{ detail.declarationTypeName }}</dd>
            <dt>申报期间</dt>
            <dd>{{ detail.periodStart }} 至 {{ detail.periodEnd }}</dd>
            <dt>模拟金额</dt>
            <dd class="amount">¥ {{ formatAmount(detail.declaredAmount) }}</dd>
            <dt>表单模板</dt>
            <dd>{{ detail.templateName }} V{{ detail.templateVersionNo }}</dd>
            <dt>创建人</dt>
            <dd>{{ detail.createdByName }}</dd>
            <dt>提交时间</dt>
            <dd>{{ formatDateTime(detail.submittedAt) }}</dd>
            <dt>最近审核</dt>
            <dd>{{ formatDateTime(detail.lastReviewedAt) }}</dd>
          </dl>

          <div class="form-section-title" style="margin-top: 22px">表单内容</div>
          <el-table :data="detail.fields" size="small" border>
            <el-table-column prop="fieldLabel" label="字段" width="180" />
            <el-table-column label="填写值" min-width="240">
              <template #default="{ row }">{{ formatFormValue(detail?.formData?.[row.fieldCode]) }}</template>
            </el-table-column>
          </el-table>
          <div v-if="!detail.fields.length" class="empty-state">暂无表单字段</div>

          <div class="form-section-title" style="margin-top: 22px">审核记录</div>
          <div v-if="detail.reviews.length" class="review-timeline">
            <div v-for="review in detail.reviews" :key="review.id" class="review-item">
              <div class="review-dot" :class="review.action === 'APPROVE' ? 'approve' : 'return'" />
              <div class="review-body">
                <div class="review-head">
                  <strong>{{ REVIEW_ACTION[review.action] ?? review.action }}</strong>
                  <span class="text-muted" style="font-size: 12px"
                    >{{ review.reviewerName }} · {{ formatDateTime(review.reviewedAt) }}</span
                  >
                </div>
                <p class="review-comment">{{ review.comment || '（未填写意见）' }}</p>
              </div>
            </div>
          </div>
          <div v-else class="empty-state" style="padding: 18px 0">暂无审核记录</div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--tax-border);
}

.review-timeline {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-item {
  display: flex;
  gap: 12px;
}

.review-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.review-dot.approve {
  background: var(--tax-success);
}

.review-dot.return {
  background: var(--tax-danger);
}

.review-body {
  flex: 1;
  background: var(--tax-surface-soft);
  border: 1px solid var(--tax-border);
  border-radius: 4px;
  padding: 10px 12px;
}

.review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.review-comment {
  margin-top: 4px;
  font-size: 13px;
  color: var(--tax-text-secondary);
  white-space: pre-wrap;
}
</style>

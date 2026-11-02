<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createTemplate,
  disableTemplate,
  fetchTemplate,
  fetchTemplateVersions,
  fetchTemplates,
  newTemplateVersion,
  publishTemplate,
  updateTemplate,
  type TemplatePayload,
} from '@/api/formTemplates'
import { fetchDeclarationTypeOptions } from '@/api/declarationTypes'
import type { DeclarationType, FieldDef, Template } from '@/types/models'
import { FIELD_TYPE, formatDateTime, templateStatus } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<Template[]>([])
const total = ref(0)
const types = ref<DeclarationType[]>([])
const query = reactive({
  keyword: '',
  declarationTypeId: null as number | null,
  status: '',
  page: 1,
  pageSize: 10,
})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const loadingDetail = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  templateCode: '',
  templateName: '',
  declarationTypeId: null as number | null,
  fields: [] as FieldDef[],
})
const rules: FormRules = {
  templateCode: [
    { required: true, message: '请输入模板编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]{3,64}$/, message: '模板编码只能包含字母、数字、下划线或短横线', trigger: 'blur' },
  ],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  declarationTypeId: [{ required: true, message: '请选择申报类型', trigger: 'change' }],
}

const previewVisible = ref(false)
const previewTemplate = ref<Template | null>(null)
const versionsVisible = ref(false)
const versions = ref<Template[]>([])
const versionTemplate = ref<Template | null>(null)
const versionLoading = ref(false)

const needsOptions = (type: string) => type === 'RADIO' || type === 'MULTI_SELECT'
const numericTypes = computed(() => ['INTEGER', 'DECIMAL'])

async function load() {
  loading.value = true
  try {
    const result = await fetchTemplates({
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      declarationTypeId: query.declarationTypeId,
      status: query.status || undefined,
    })
    rows.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadTypes() {
  try {
    types.value = await fetchDeclarationTypeOptions()
  } catch {
    types.value = []
  }
}

function search() {
  query.page = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.declarationTypeId = null
  query.status = ''
  search()
}

function emptyField(): FieldDef {
  return {
    fieldCode: '',
    fieldLabel: '',
    fieldType: 'TEXT',
    required: false,
    minValue: null,
    maxValue: null,
    maxLength: 200,
    options: [],
    helpText: '',
    sortOrder: 0,
    enabled: true,
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, { templateCode: '', templateName: '', declarationTypeId: null, fields: [emptyField()] })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function openEdit(row: Template) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  loadingDetail.value = true
  dialogVisible.value = true
  try {
    const detail = await fetchTemplate(row.id)
    Object.assign(form, {
      templateCode: detail.templateCode,
      templateName: detail.templateName,
      declarationTypeId: detail.declarationTypeId,
      fields: detail.fields.map((field) => ({
        ...field,
        required: field.required,
        enabled: field.enabled,
        options: field.options.map((option) => ({ ...option })),
      })),
    })
  } finally {
    loadingDetail.value = false
  }
}

function addField() {
  form.fields.push(emptyField())
}

function removeField(index: number) {
  form.fields.splice(index, 1)
}

function moveField(index: number, offset: number) {
  const target = index + offset
  if (target < 0 || target >= form.fields.length) return
  const [item] = form.fields.splice(index, 1)
  form.fields.splice(target, 0, item)
}

function addOption(field: FieldDef) {
  field.options.push({ label: '', value: '' })
}

function removeOption(field: FieldDef, index: number) {
  field.options.splice(index, 1)
}

function validateFields(): string | null {
  if (!form.fields.length) return '至少配置一个表单字段'
  const codes = new Set<string>()
  for (const [index, field] of form.fields.entries()) {
    if (!/^[a-zA-Z][a-zA-Z0-9_]{0,63}$/.test(field.fieldCode.trim())) {
      return `第 ${index + 1} 个字段编码不合法（需字母开头，仅含字母、数字、下划线）`
    }
    if (codes.has(field.fieldCode.trim().toLowerCase())) return `字段编码重复：${field.fieldCode}`
    codes.add(field.fieldCode.trim().toLowerCase())
    if (!field.fieldLabel.trim()) return `第 ${index + 1} 个字段缺少标签`
    if (needsOptions(field.fieldType)) {
      if (!field.options.length) return `字段「${field.fieldLabel}」必须配置选项`
      const values = new Set<string>()
      for (const option of field.options) {
        if (!option.label.trim() || !option.value.trim()) return `字段「${field.fieldLabel}」存在空的选项标签或值`
        if (values.has(option.value.trim())) return `字段「${field.fieldLabel}」选项值重复：${option.value}`
        values.add(option.value.trim())
      }
    }
    if (field.minValue != null && field.maxValue != null && Number(field.minValue) > Number(field.maxValue)) {
      return `字段「${field.fieldLabel}」最小值不能大于最大值`
    }
  }
  return null
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const fieldError = validateFields()
  if (fieldError) {
    ElMessage.warning(fieldError)
    return
  }
  submitting.value = true
  try {
    const payload: TemplatePayload = {
      templateCode: form.templateCode.trim(),
      templateName: form.templateName.trim(),
      declarationTypeId: form.declarationTypeId as number,
      fields: form.fields.map((field, index) => ({
        fieldCode: field.fieldCode.trim(),
        fieldLabel: field.fieldLabel.trim(),
        fieldType: field.fieldType,
        required: field.required,
        minValue:
          numericTypes.value.includes(field.fieldType) && field.minValue != null ? Number(field.minValue) : null,
        maxValue:
          numericTypes.value.includes(field.fieldType) && field.maxValue != null ? Number(field.maxValue) : null,
        maxLength: field.fieldType === 'TEXT' && field.maxLength != null ? Number(field.maxLength) : null,
        options: needsOptions(field.fieldType)
          ? field.options.map((option) => ({ label: option.label.trim(), value: option.value.trim() }))
          : [],
        helpText: field.helpText?.trim() || undefined,
        sortOrder: index + 1,
        enabled: field.enabled,
      })),
    }
    if (dialogMode.value === 'create') {
      await createTemplate(payload)
      ElMessage.success('模板草稿已创建')
    } else if (editingId.value) {
      await updateTemplate(editingId.value, payload)
      ElMessage.success('模板草稿已更新')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function preview(row: Template) {
  loadingDetail.value = true
  previewVisible.value = true
  previewTemplate.value = null
  try {
    previewTemplate.value = await fetchTemplate(row.id)
  } finally {
    loadingDetail.value = false
  }
}

async function publish(row: Template) {
  try {
    await ElMessageBox.confirm(
      `发布模板「${row.templateName}」V${row.versionNo}？发布后将不可原位修改，并成为该申报类型的默认模板。`,
      '发布模板',
      { confirmButtonText: '发布', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  await publishTemplate(row.id)
  ElMessage.success('模板已发布')
  await load()
}

async function disable(row: Template) {
  try {
    await ElMessageBox.confirm(`确认停用模板「${row.templateName}」V${row.versionNo}？`, '停用模板', {
      confirmButtonText: '停用',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await disableTemplate(row.id)
  ElMessage.success('模板已停用')
  await load()
}

async function createVersion(row: Template) {
  try {
    await ElMessageBox.confirm(
      `基于「${row.templateName}」V${row.versionNo} 创建新的草稿版本？原版本保持不变。`,
      '新建版本',
      { confirmButtonText: '创建', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  const created = await newTemplateVersion(row.id)
  ElMessage.success(`已创建 V${created.versionNo} 草稿`)
  await load()
  await openEdit(created)
}

async function showVersions(row: Template) {
  versionTemplate.value = row
  versionsVisible.value = true
  versionLoading.value = true
  try {
    versions.value = await fetchTemplateVersions(row.id)
  } finally {
    versionLoading.value = false
  }
}

onMounted(() => {
  load()
  loadTypes()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">表单模板</h1>
        <p class="page-description">模板按版本管理：草稿可编辑，发布后不可原位修改，通过新建版本演进</p>
      </div>
      <el-button v-if="auth.hasPerm('form-template:write')" type="primary" @click="openCreate">新建模板</el-button>
    </div>

    <div
      class="filter-bar card"
      style="border-radius: var(--tax-radius); margin-bottom: 14px; border: 1px solid var(--tax-border)"
    >
      <div class="filter-item">
        <label>关键词</label>
        <el-input
          v-model="query.keyword"
          placeholder="模板编码或名称"
          clearable
          style="width: 210px"
          @keyup.enter="search"
        />
      </div>
      <div class="filter-item">
        <label>申报类型</label>
        <el-select v-model="query.declarationTypeId" placeholder="全部类型" clearable style="width: 180px">
          <el-option v-for="type in types" :key="type.id" :label="type.typeName" :value="type.id" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>状态</label>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="templateCode" label="模板编码" width="150">
          <template #default="{ row }"
            ><span class="mono">{{ row.templateCode }}</span></template
          >
        </el-table-column>
        <el-table-column prop="templateName" label="模板名称" min-width="190" />
        <el-table-column prop="declarationTypeName" label="申报类型" width="140" />
        <el-table-column label="版本" width="80">
          <template #default="{ row }">V{{ row.versionNo }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-tag" :class="templateStatus(row.status).className">{{
              templateStatus(row.status).label
            }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="创建人" width="130" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="preview(row)">预览</el-button>
            <el-button text type="primary" size="small" @click="showVersions(row)">版本</el-button>
            <template v-if="auth.hasPerm('form-template:write')">
              <el-button v-if="row.status === 'DRAFT'" text type="primary" size="small" @click="openEdit(row)"
                >编辑</el-button
              >
              <el-button v-if="row.status === 'DRAFT'" text type="success" size="small" @click="publish(row)"
                >发布</el-button
              >
              <el-button v-if="row.status === 'PUBLISHED'" text type="warning" size="small" @click="disable(row)"
                >停用</el-button
              >
              <el-button v-if="row.status !== 'DRAFT'" text type="primary" size="small" @click="createVersion(row)"
                >新版本</el-button
              >
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="summary">共 {{ total }} 个模板</span>
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
      :title="dialogMode === 'create' ? '新建表单模板' : '编辑模板草稿'"
      width="900px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div v-loading="loadingDetail">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
          <div class="form-grid">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input
                v-model="form.templateCode"
                :disabled="dialogMode === 'edit'"
                placeholder="例如 FT-SPECIAL-01"
              />
            </el-form-item>
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="例如 模拟专项申报表" />
            </el-form-item>
            <el-form-item label="申报类型" prop="declarationTypeId" class="full-row">
              <el-select
                v-model="form.declarationTypeId"
                :disabled="dialogMode === 'edit'"
                placeholder="请选择申报类型"
                style="width: 320px"
              >
                <el-option
                  v-for="type in types"
                  :key="type.id"
                  :label="`${type.typeName}（${type.typeCode}）`"
                  :value="type.id"
                />
              </el-select>
            </el-form-item>
          </div>
        </el-form>

        <div class="field-editor-head" style="margin-top: 4px">
          <div class="form-section-title" style="margin-bottom: 0">表单字段（{{ form.fields.length }}）</div>
          <el-button size="small" @click="addField">添加字段</el-button>
        </div>

        <div v-if="!form.fields.length" class="empty-state" style="padding: 22px 0">尚未配置字段</div>

        <div v-for="(field, index) in form.fields" :key="index" class="field-editor">
          <div class="field-editor-head">
            <span class="text-secondary" style="font-size: 13px">
              字段 {{ index + 1 }}
              <span v-if="field.fieldCode" class="mono text-muted">· {{ field.fieldCode }}</span>
            </span>
            <span class="toolbar-inline">
              <el-button text size="small" :disabled="index === 0" @click="moveField(index, -1)">上移</el-button>
              <el-button text size="small" :disabled="index === form.fields.length - 1" @click="moveField(index, 1)"
                >下移</el-button
              >
              <el-button text type="danger" size="small" @click="removeField(index)">删除</el-button>
            </span>
          </div>

          <div class="form-grid">
            <el-form-item label="字段编码" label-width="82px" required>
              <el-input v-model="field.fieldCode" placeholder="例如 income_amount" />
            </el-form-item>
            <el-form-item label="字段标签" label-width="82px" required>
              <el-input v-model="field.fieldLabel" placeholder="例如 模拟收入金额（元）" />
            </el-form-item>
            <el-form-item label="字段类型" label-width="82px">
              <el-select v-model="field.fieldType" style="width: 100%">
                <el-option v-for="(label, value) in FIELD_TYPE" :key="value" :label="label" :value="value" />
              </el-select>
            </el-form-item>
            <el-form-item label="是否必填" label-width="82px">
              <el-switch v-model="field.required" />
            </el-form-item>
            <template v-if="numericTypes.includes(field.fieldType)">
              <el-form-item label="最小值" label-width="82px">
                <el-input-number v-model="field.minValue" :precision="2" :controls="false" style="width: 100%" />
              </el-form-item>
              <el-form-item label="最大值" label-width="82px">
                <el-input-number v-model="field.maxValue" :precision="2" :controls="false" style="width: 100%" />
              </el-form-item>
            </template>
            <el-form-item v-if="field.fieldType === 'TEXT'" label="最大长度" label-width="82px">
              <el-input-number v-model="field.maxLength" :min="1" :max="5000" style="width: 100%" />
            </el-form-item>
            <el-form-item label="帮助文字" label-width="82px">
              <el-input v-model="field.helpText" maxlength="255" placeholder="可留空" />
            </el-form-item>
          </div>

          <div v-if="needsOptions(field.fieldType)" style="margin-top: 4px">
            <div class="toolbar-inline" style="margin-bottom: 8px">
              <span class="text-muted" style="font-size: 12px">选项配置</span>
              <el-button text size="small" type="primary" @click="addOption(field)">添加选项</el-button>
            </div>
            <div
              v-for="(option, optionIndex) in field.options"
              :key="optionIndex"
              class="toolbar-inline"
              style="margin-bottom: 6px"
            >
              <el-input v-model="option.label" placeholder="选项标签" style="width: 200px" />
              <el-input v-model="option.value" placeholder="选项值" style="width: 180px" />
              <el-button text type="danger" size="small" @click="removeOption(field, optionIndex)">移除</el-button>
            </div>
            <div v-if="!field.options.length" class="text-muted" style="font-size: 12px">
              单选/多选字段至少需要一个选项
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="loadingDetail" @click="submitForm"
          >保存草稿</el-button
        >
      </template>
    </el-dialog>

    <el-drawer v-model="previewVisible" title="模板预览" size="620px">
      <div v-loading="loadingDetail">
        <template v-if="previewTemplate">
          <div class="card" style="padding: 18px; margin-bottom: 16px">
            <h3 style="font-size: 16px; margin-bottom: 8px">{{ previewTemplate.templateName }}</h3>
            <div class="text-muted" style="font-size: 12px">
              {{ previewTemplate.templateCode }} · V{{ previewTemplate.versionNo }} ·
              {{ templateStatus(previewTemplate.status).label }} · {{ previewTemplate.declarationTypeName }}
            </div>
          </div>
          <div class="form-render">
            <div class="form-grid">
              <div v-for="field in previewTemplate.fields" :key="field.id" class="preview-field">
                <label>
                  {{ field.fieldLabel }}
                  <span v-if="field.required" style="color: var(--tax-danger)">*</span>
                </label>
                <div class="preview-input">
                  <template v-if="field.fieldType === 'TEXT'">文本输入 · 最长 {{ field.maxLength ?? '-' }} 字</template>
                  <template v-else-if="field.fieldType === 'INTEGER'">整数输入</template>
                  <template v-else-if="field.fieldType === 'DECIMAL'">金额/小数输入</template>
                  <template v-else-if="field.fieldType === 'DATE'">日期选择</template>
                  <template v-else-if="field.fieldType === 'BOOLEAN'">是 / 否</template>
                  <template v-else>
                    {{ field.options.map((option) => option.label).join(' / ') }}
                  </template>
                </div>
                <div v-if="field.helpText" class="text-muted" style="font-size: 11px; margin-top: 2px">
                  {{ field.helpText }}
                </div>
              </div>
            </div>
            <div v-if="!previewTemplate.fields.length" class="empty-state">该模板暂无字段</div>
          </div>
        </template>
      </div>
    </el-drawer>

    <el-drawer v-model="versionsVisible" :title="`版本记录 · ${versionTemplate?.templateCode ?? ''}`" size="560px">
      <el-table v-loading="versionLoading" :data="versions" size="small">
        <el-table-column label="版本" width="80">
          <template #default="{ row }">V{{ row.versionNo }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-tag" :class="templateStatus(row.status).className">{{
              templateStatus(row.status).label
            }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="创建人" width="120" />
        <el-table-column label="发布时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<style scoped>
.preview-field {
  margin-bottom: 14px;
}

.preview-field > label {
  display: block;
  font-size: 12px;
  color: var(--tax-text-secondary);
  margin-bottom: 4px;
}

.preview-input {
  border: 1px solid var(--tax-border-strong);
  border-radius: 4px;
  background: #fff;
  padding: 7px 10px;
  font-size: 12px;
  color: var(--tax-muted);
  min-height: 32px;
}
</style>

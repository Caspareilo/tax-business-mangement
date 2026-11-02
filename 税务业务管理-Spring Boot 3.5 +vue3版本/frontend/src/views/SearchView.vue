<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportDeclarations, searchDeclarations } from '@/api/search'
import { fetchDeclarationTypeOptions } from '@/api/declarationTypes'
import { fetchDeclaration } from '@/api/declarations'
import type { Declaration, DeclarationType, DeclQuery } from '@/types/models'
import { declarationStatus, DECLARATION_STATUS, formatAmount, formatDateTime, formatFormValue } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const exporting = ref(false)
const rows = ref<Declaration[]>([])
const total = ref(0)
const types = ref<DeclarationType[]>([])

const query = reactive({
  keyword: '',
  declarationNo: '',
  declarationTypeId: null as number | null,
  status: '',
  periodRange: [] as string[],
  minAmount: null as number | null,
  maxAmount: null as number | null,
  sortBy: 'updatedAt',
  sortOrder: 'desc',
  page: 1,
  pageSize: 10,
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<Declaration | null>(null)

const hasFilter = computed(() =>
  Boolean(
    query.keyword ||
    query.declarationNo ||
    query.declarationTypeId ||
    query.status ||
    query.periodRange?.length ||
    query.minAmount != null ||
    query.maxAmount != null,
  ),
)

function buildParams(includePage: boolean): DeclQuery {
  const params: DeclQuery = {}
  if (query.keyword.trim()) params.keyword = query.keyword.trim()
  if (query.declarationNo.trim()) params.declarationNo = query.declarationNo.trim()
  if (query.declarationTypeId) params.declarationTypeId = query.declarationTypeId
  if (query.status) params.status = query.status
  if (query.periodRange?.length === 2) {
    params.periodFrom = query.periodRange[0]
    params.periodTo = query.periodRange[1]
  }
  if (query.minAmount != null) params.minAmount = query.minAmount
  if (query.maxAmount != null) params.maxAmount = query.maxAmount
  params.sortBy = query.sortBy
  params.sortOrder = query.sortOrder
  if (includePage) {
    params.page = query.page
    params.pageSize = query.pageSize
  }
  return params
}

async function load() {
  loading.value = true
  try {
    const result = await searchDeclarations(buildParams(true))
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
  query.declarationNo = ''
  query.declarationTypeId = null
  query.status = ''
  query.periodRange = []
  query.minAmount = null
  query.maxAmount = null
  query.sortBy = 'updatedAt'
  query.sortOrder = 'desc'
  query.page = 1
  load()
}

function downloadBlob(blob: Blob) {
  const stamp = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  const name = `申报导出-${stamp.getFullYear()}${pad(stamp.getMonth() + 1)}${pad(stamp.getDate())}${pad(stamp.getHours())}${pad(stamp.getMinutes())}${pad(stamp.getSeconds())}.csv`
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = name
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(url)
}

async function exportCsv() {
  exporting.value = true
  try {
    const blob = await exportDeclarations(buildParams(false))
    downloadBlob(blob)
    ElMessage.success('导出文件已开始下载（最多 5000 条）')
  } finally {
    exporting.value = false
  }
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

onMounted(() => {
  loadTypes()
  load()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">综合查询</h1>
        <p class="page-description">跨状态组合筛选申报记录，导出结果受当前筛选条件与权限约束</p>
      </div>
      <el-button v-if="auth.hasPerm('declaration:export')" type="primary" :loading="exporting" @click="exportCsv"
        >导出 CSV</el-button
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
          placeholder="编号或纳税人名称"
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
          style="width: 170px"
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
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="(item, value) in DECLARATION_STATUS" :key="value" :label="item.label" :value="value" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>申报期间</label>
        <el-date-picker
          v-model="query.periodRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          style="width: 260px"
        />
      </div>
      <div class="filter-item">
        <label>金额范围</label>
        <div class="toolbar-inline">
          <el-input-number
            v-model="query.minAmount"
            :min="0"
            :controls="false"
            placeholder="最小"
            style="width: 110px"
          />
          <span class="text-muted">-</span>
          <el-input-number
            v-model="query.maxAmount"
            :min="0"
            :controls="false"
            placeholder="最大"
            style="width: 110px"
          />
        </div>
      </div>
      <div class="filter-item">
        <label>排序</label>
        <el-select v-model="query.sortBy" style="width: 150px" @change="search">
          <el-option label="最近更新" value="updatedAt" />
          <el-option label="提交时间" value="submittedAt" />
          <el-option label="申报金额" value="declaredAmount" />
          <el-option label="申报编号" value="declarationNo" />
          <el-option label="创建时间" value="createdAt" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>方向</label>
        <el-select v-model="query.sortOrder" style="width: 100px" @change="search">
          <el-option label="降序" value="desc" />
          <el-option label="升序" value="asc" />
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
        <el-table-column prop="taxpayerCode" label="纳税人编号" width="130">
          <template #default="{ row }"
            ><span class="mono">{{ row.taxpayerCode }}</span></template
          >
        </el-table-column>
        <el-table-column prop="taxpayerName" label="纳税人" min-width="180" show-overflow-tooltip />
        <el-table-column prop="declarationTypeName" label="申报类型" width="130" />
        <el-table-column label="申报期间" width="190">
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
        <el-table-column label="模拟金额" width="130" align="right">
          <template #default="{ row }"
            ><span class="amount">{{ formatAmount(row.declaredAmount) }}</span></template
          >
        </el-table-column>
        <el-table-column prop="createdByName" label="创建人" width="130" />
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="summary"> 共 {{ total }} 条{{ hasFilter ? '（已应用筛选）' : '' }} · 导出上限 5000 条 </span>
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
      <div v-if="!loading && !rows.length" class="empty-state">没有符合筛选条件的申报记录</div>
    </div>

    <el-drawer v-model="detailVisible" title="申报详情" size="680px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <div class="mono" style="font-size: 15px">{{ detail.declarationNo }}</div>
          <dl class="detail-list" style="margin-top: 14px">
            <dt>纳税人</dt>
            <dd>
              {{ detail.taxpayerName }} <span class="mono text-muted">{{ detail.taxpayerCode }}</span>
            </dd>
            <dt>申报类型</dt>
            <dd>{{ detail.declarationTypeName }}</dd>
            <dt>申报期间</dt>
            <dd>{{ detail.periodStart }} 至 {{ detail.periodEnd }}</dd>
            <dt>当前状态</dt>
            <dd>
              <span class="status-tag" :class="declarationStatus(detail.status).className">{{
                declarationStatus(detail.status).label
              }}</span>
            </dd>
            <dt>模拟金额</dt>
            <dd class="amount">¥ {{ formatAmount(detail.declaredAmount) }}</dd>
            <dt>提交时间</dt>
            <dd>{{ formatDateTime(detail.submittedAt) }}</dd>
            <dt>最近审核</dt>
            <dd>{{ formatDateTime(detail.lastReviewedAt) }}</dd>
          </dl>
          <div class="form-section-title" style="margin-top: 20px">表单内容</div>
          <el-table :data="detail.fields" size="small" border>
            <el-table-column prop="fieldLabel" label="字段" width="170" />
            <el-table-column label="填写值" min-width="220">
              <template #default="{ row }">{{ formatFormValue(detail?.formData?.[row.fieldCode]) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

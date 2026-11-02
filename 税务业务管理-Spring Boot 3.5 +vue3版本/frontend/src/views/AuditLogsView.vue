<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { fetchAuditLogs } from '@/api/auditLogs'
import type { AuditLog } from '@/types/models'
import { formatDateTime } from '@/utils/dict'

const loading = ref(false)
const rows = ref<AuditLog[]>([])
const total = ref(0)

const operationTypes = [
  'LOGIN',
  'DECLARATION_CREATE',
  'DECLARATION_UPDATE',
  'DECLARATION_SUBMIT',
  'DECLARATION_WITHDRAW',
  'DECLARATION_REVIEW',
  'DECLARATION_EXPORT',
  'TAXPAYER_CREATE',
  'TAXPAYER_UPDATE',
  'USER_CREATE',
  'USER_UPDATE',
  'ROLE_PERMISSION_ASSIGN',
  'FORM_TEMPLATE_CREATE',
  'FORM_TEMPLATE_PUBLISH',
]

const query = reactive({
  keyword: '',
  operationType: '',
  result: '',
  dateRange: [] as string[],
  page: 1,
  pageSize: 20,
})

const detailVisible = ref(false)
const detail = ref<AuditLog | null>(null)

async function load() {
  loading.value = true
  try {
    const result = await fetchAuditLogs({
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      operationType: query.operationType || undefined,
      result: query.result || undefined,
      from: query.dateRange?.[0] || undefined,
      to: query.dateRange?.[1] || undefined,
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
  query.operationType = ''
  query.result = ''
  query.dateRange = []
  query.page = 1
  load()
}

function openDetail(row: AuditLog) {
  detail.value = row
  detailVisible.value = true
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">操作日志</h1>
        <p class="page-description">记录登录、申报流转、审核与导出等敏感操作；日志不包含密码等敏感内容</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div
      class="filter-bar card"
      style="border-radius: var(--tax-radius); margin-bottom: 14px; border: 1px solid var(--tax-border)"
    >
      <div class="filter-item">
        <label>关键词</label>
        <el-input
          v-model="query.keyword"
          placeholder="摘要或操作人"
          clearable
          style="width: 200px"
          @keyup.enter="search"
        />
      </div>
      <div class="filter-item">
        <label>操作类型</label>
        <el-select
          v-model="query.operationType"
          placeholder="全部类型"
          clearable
          filterable
          allow-create
          style="width: 200px"
        >
          <el-option v-for="type in operationTypes" :key="type" :label="type" :value="type" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>结果</label>
        <el-select v-model="query.result" placeholder="全部" clearable style="width: 120px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAIL" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>操作时间</label>
        <el-date-picker
          v-model="query.dateRange"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          range-separator="至"
          style="width: 330px"
        />
      </div>
      <div class="filter-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="140" />
        <el-table-column prop="operationType" label="操作类型" width="200">
          <template #default="{ row }"
            ><span class="mono">{{ row.operationType }}</span></template
          >
        </el-table-column>
        <el-table-column prop="targetType" label="对象类型" width="110" />
        <el-table-column prop="summary" label="操作摘要" min-width="280" show-overflow-tooltip />
        <el-table-column prop="result" label="结果" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="row.result === 'SUCCESS' ? 'success' : 'danger'">{{
              row.result
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="summary">共 {{ total }} 条日志</span>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="search"
        />
      </div>
      <div v-if="!loading && !rows.length" class="empty-state">暂无操作日志</div>
    </div>

    <el-drawer v-model="detailVisible" title="日志详情" size="520px">
      <template v-if="detail">
        <dl class="detail-list">
          <dt>日志编号</dt>
          <dd class="mono">{{ detail.id }}</dd>
          <dt>操作时间</dt>
          <dd>{{ formatDateTime(detail.createdAt) }}</dd>
          <dt>操作人</dt>
          <dd>
            {{ detail.operatorName || '-'
            }}<span v-if="detail.operatorId" class="text-muted">（ID {{ detail.operatorId }}）</span>
          </dd>
          <dt>操作类型</dt>
          <dd class="mono">{{ detail.operationType }}</dd>
          <dt>对象</dt>
          <dd>{{ detail.targetType || '-' }} {{ detail.targetId ? `#${detail.targetId}` : '' }}</dd>
          <dt>结果</dt>
          <dd>{{ detail.result }}</dd>
          <dt>追踪号</dt>
          <dd class="mono">{{ detail.traceId || '-' }}</dd>
          <dt>摘要</dt>
          <dd style="white-space: pre-wrap">{{ detail.summary || '-' }}</dd>
        </dl>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { approveDeclaration, fetchPendingReviews, fetchReviewHistory, returnDeclaration } from '@/api/reviews'
import { fetchDeclaration } from '@/api/declarations'
import { fetchDeclarationTypeOptions } from '@/api/declarationTypes'
import type { Declaration, DeclarationType, ReviewHistory } from '@/types/models'
import { declarationStatus, formatAmount, formatDateTime, formatFormValue, REVIEW_ACTION } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const activeTab = ref('pending')
const types = ref<DeclarationType[]>([])

const pendingLoading = ref(false)
const pendingRows = ref<Declaration[]>([])
const pendingTotal = ref(0)
const pendingQuery = reactive({
  keyword: typeof route.query.keyword === 'string' ? route.query.keyword : '',
  typeId: null as number | null,
  page: 1,
  pageSize: 10,
})

const historyLoading = ref(false)
const historyRows = ref<ReviewHistory[]>([])
const historyTotal = ref(0)
const historyQuery = reactive({
  keyword: '',
  action: '',
  reviewerId: null as number | null,
  dateRange: [] as string[],
  page: 1,
  pageSize: 10,
})

const actionVisible = ref(false)
const actionMode = ref<'approve' | 'return'>('approve')
const actionTarget = ref<Declaration | null>(null)
const actionComment = ref('')
const actionSubmitting = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<Declaration | null>(null)

async function loadPending() {
  pendingLoading.value = true
  try {
    const result = await fetchPendingReviews({
      page: pendingQuery.page,
      pageSize: pendingQuery.pageSize,
      keyword: pendingQuery.keyword.trim() || undefined,
      typeId: pendingQuery.typeId,
    })
    pendingRows.value = result.items
    pendingTotal.value = result.total
  } finally {
    pendingLoading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const result = await fetchReviewHistory({
      page: historyQuery.page,
      pageSize: historyQuery.pageSize,
      keyword: historyQuery.keyword.trim() || undefined,
      action: historyQuery.action || undefined,
      reviewerId: historyQuery.reviewerId,
      from: historyQuery.dateRange?.[0] || undefined,
      to: historyQuery.dateRange?.[1] || undefined,
    })
    historyRows.value = result.items
    historyTotal.value = result.total
  } finally {
    historyLoading.value = false
  }
}

async function loadTypes() {
  try {
    types.value = await fetchDeclarationTypeOptions()
  } catch {
    types.value = []
  }
}

function searchPending() {
  pendingQuery.page = 1
  loadPending()
}

function searchHistory() {
  historyQuery.page = 1
  loadHistory()
}

function resetPendingFilters() {
  pendingQuery.keyword = ''
  pendingQuery.typeId = null
  searchPending()
}

function resetHistoryFilters() {
  historyQuery.keyword = ''
  historyQuery.action = ''
  historyQuery.dateRange = []
  searchHistory()
}

function openAction(row: Declaration, mode: 'approve' | 'return') {
  actionTarget.value = row
  actionMode.value = mode
  actionComment.value = ''
  actionVisible.value = true
}

async function submitAction() {
  if (!actionTarget.value) return
  if (actionMode.value === 'return' && !actionComment.value.trim()) {
    ElMessage.warning('退回修改时必须填写退回原因')
    return
  }
  actionSubmitting.value = true
  try {
    if (actionMode.value === 'approve') {
      await approveDeclaration(actionTarget.value.id, actionComment.value.trim())
      ElMessage.success('审核已通过')
    } else {
      await returnDeclaration(actionTarget.value.id, actionComment.value.trim())
      ElMessage.success('申报已退回')
    }
    actionVisible.value = false
    await Promise.all([loadPending(), loadHistory()])
  } finally {
    actionSubmitting.value = false
  }
}

async function openDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchDeclaration(id)
  } finally {
    detailLoading.value = false
  }
}

function onTabChange(name: string | number) {
  if (name === 'history' && !historyRows.value.length) {
    loadHistory()
  }
}

onMounted(() => {
  loadTypes()
  loadPending()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">审核管理</h1>
        <p class="page-description">审核待处理的申报，审批意见与退回原因会写入审核记录和操作日志</p>
      </div>
      <el-button :loading="pendingLoading" @click="loadPending">刷新待办</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="待审核申报" name="pending">
        <div
          class="filter-bar card"
          style="border-radius: var(--tax-radius); margin-bottom: 14px; border: 1px solid var(--tax-border)"
        >
          <div class="filter-item">
            <label>关键词</label>
            <el-input
              v-model="pendingQuery.keyword"
              placeholder="申报编号或纳税人"
              clearable
              style="width: 220px"
              @keyup.enter="searchPending"
            />
          </div>
          <div class="filter-item">
            <label>申报类型</label>
            <el-select v-model="pendingQuery.typeId" placeholder="全部类型" clearable style="width: 170px">
              <el-option v-for="type in types" :key="type.id" :label="type.typeName" :value="type.id" />
            </el-select>
          </div>
          <div class="filter-actions">
            <el-button @click="resetPendingFilters">重置</el-button>
            <el-button type="primary" @click="searchPending">查询</el-button>
          </div>
        </div>

        <div class="table-card">
          <el-table v-loading="pendingLoading" :data="pendingRows" style="width: 100%">
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
            <el-table-column label="模拟金额" width="120" align="right">
              <template #default="{ row }"
                ><span class="amount">{{ formatAmount(row.declaredAmount) }}</span></template
              >
            </el-table-column>
            <el-table-column prop="createdByName" label="提交人" width="120" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="openDetail(row.id)">查看</el-button>
                <template v-if="auth.hasPerm('review:write')">
                  <el-button text type="success" size="small" @click="openAction(row, 'approve')">通过</el-button>
                  <el-button text type="danger" size="small" @click="openAction(row, 'return')">退回</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-footer">
            <span class="summary">待审核 {{ pendingTotal }} 条</span>
            <el-pagination
              v-model:current-page="pendingQuery.page"
              v-model:page-size="pendingQuery.pageSize"
              :total="pendingTotal"
              :page-sizes="[10, 20, 50]"
              layout="sizes, prev, pager, next"
              background
              @current-change="loadPending"
              @size-change="searchPending"
            />
          </div>
          <div v-if="!pendingLoading && !pendingRows.length" class="empty-state">暂无待审核申报</div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="审核历史" name="history">
        <div
          class="filter-bar card"
          style="border-radius: var(--tax-radius); margin-bottom: 14px; border: 1px solid var(--tax-border)"
        >
          <div class="filter-item">
            <label>关键词</label>
            <el-input
              v-model="historyQuery.keyword"
              placeholder="申报编号或纳税人"
              clearable
              style="width: 200px"
              @keyup.enter="searchHistory"
            />
          </div>
          <div class="filter-item">
            <label>审核动作</label>
            <el-select v-model="historyQuery.action" placeholder="全部" clearable style="width: 130px">
              <el-option label="审核通过" value="APPROVE" />
              <el-option label="退回修改" value="RETURN" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>审核时间</label>
            <el-date-picker
              v-model="historyQuery.dateRange"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              range-separator="至"
              style="width: 340px"
            />
          </div>
          <div class="filter-actions">
            <el-button @click="resetHistoryFilters">重置</el-button>
            <el-button type="primary" @click="searchHistory">查询</el-button>
          </div>
        </div>

        <div class="table-card">
          <el-table v-loading="historyLoading" :data="historyRows" style="width: 100%">
            <el-table-column prop="declarationNo" label="申报编号" width="165">
              <template #default="{ row }"
                ><span class="mono">{{ row.declarationNo }}</span></template
              >
            </el-table-column>
            <el-table-column prop="taxpayerName" label="纳税人" min-width="170" show-overflow-tooltip />
            <el-table-column prop="declarationTypeName" label="申报类型" width="120" />
            <el-table-column label="审核动作" width="110">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="row.action === 'APPROVE' ? 'success' : 'danger'">
                  {{ REVIEW_ACTION[row.action] ?? row.action }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reviewerName" label="审核人" width="130" />
            <el-table-column prop="comment" label="审核意见" min-width="220" show-overflow-tooltip />
            <el-table-column label="审核时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.reviewedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="openDetail(row.declarationId)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-footer">
            <span class="summary">共 {{ historyTotal }} 条审核记录</span>
            <el-pagination
              v-model:current-page="historyQuery.page"
              v-model:page-size="historyQuery.pageSize"
              :total="historyTotal"
              :page-sizes="[10, 20, 50]"
              layout="sizes, prev, pager, next"
              background
              @current-change="loadHistory"
              @size-change="searchHistory"
            />
          </div>
          <div v-if="!historyLoading && !historyRows.length" class="empty-state">暂无审核记录</div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="actionVisible"
      :title="actionMode === 'approve' ? '审核通过' : '退回修改'"
      width="520px"
      :close-on-click-modal="false"
    >
      <p class="text-muted" style="font-size: 13px; margin-bottom: 12px">
        申报编号：<span class="mono">{{ actionTarget?.declarationNo }}</span> · {{ actionTarget?.taxpayerName }}
      </p>
      <el-form label-position="top">
        <el-form-item
          :label="actionMode === 'return' ? '退回原因（必填）' : '审核意见（选填）'"
          :required="actionMode === 'return'"
        >
          <el-input
            v-model="actionComment"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            :placeholder="actionMode === 'return' ? '请说明需要补充或修改的内容' : '可填写审核说明'"
          />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="actionMode === 'return'"
        type="warning"
        :closable="false"
        show-icon
        title="退回后经办人可修改并重新提交，请确保意见清晰。"
      />
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button
          :type="actionMode === 'approve' ? 'primary' : 'danger'"
          :loading="actionSubmitting"
          @click="submitAction"
        >
          {{ actionMode === 'approve' ? '确认通过' : '确认退回' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="申报详情" size="700px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <div class="detail-head">
            <div>
              <div class="mono" style="font-size: 15px">{{ detail.declarationNo }}</div>
              <div class="text-muted" style="font-size: 12px; margin-top: 4px">{{ detail.taxpayerName }}</div>
            </div>
            <span class="status-tag" :class="declarationStatus(detail.status).className">{{
              declarationStatus(detail.status).label
            }}</span>
          </div>
          <dl class="detail-list" style="margin-top: 16px">
            <dt>申报类型</dt>
            <dd>{{ detail.declarationTypeName }}</dd>
            <dt>申报期间</dt>
            <dd>{{ detail.periodStart }} 至 {{ detail.periodEnd }}</dd>
            <dt>模拟金额</dt>
            <dd class="amount">¥ {{ formatAmount(detail.declaredAmount) }}</dd>
            <dt>提交人</dt>
            <dd>{{ detail.createdByName }}</dd>
            <dt>提交时间</dt>
            <dd>{{ formatDateTime(detail.submittedAt) }}</dd>
          </dl>
          <div class="form-section-title" style="margin-top: 20px">表单内容</div>
          <el-table :data="detail.fields" size="small" border>
            <el-table-column prop="fieldLabel" label="字段" width="170" />
            <el-table-column label="填写值" min-width="220">
              <template #default="{ row }">{{ formatFormValue(detail?.formData?.[row.fieldCode]) }}</template>
            </el-table-column>
          </el-table>
          <div class="form-section-title" style="margin-top: 20px">历史审核</div>
          <div v-if="detail.reviews.length">
            <div v-for="review in detail.reviews" :key="review.id" class="history-item">
              <el-tag size="small" effect="plain" :type="review.action === 'APPROVE' ? 'success' : 'danger'">
                {{ REVIEW_ACTION[review.action] ?? review.action }}
              </el-tag>
              <span style="font-size: 13px">{{ review.comment || '（未填写意见）' }}</span>
              <span class="text-muted" style="font-size: 12px"
                >{{ review.reviewerName }} · {{ formatDateTime(review.reviewedAt) }}</span
              >
            </div>
          </div>
          <div v-else class="empty-state" style="padding: 16px 0">暂无审核记录</div>
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
  padding-bottom: 12px;
  border-bottom: 1px solid var(--tax-border);
}

.history-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--tax-border);
}

.history-item:last-child {
  border-bottom: none;
}
</style>

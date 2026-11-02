<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import { fetchDashboard } from '@/api/statistics'
import type { Dashboard } from '@/types/models'
import { declarationStatus, formatAmount, formatDateTime } from '@/utils/dict'
import EChart from '@/components/EChart.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(true)
const loadError = ref('')
const data = ref<Dashboard | null>(null)
const trendMetric = ref<'counts' | 'amounts'>('counts')

const statusOrder = ['DRAFT', 'PENDING_REVIEW', 'RETURNED', 'APPROVED', 'WITHDRAWN']
const statusPalette = ['#bfbfbf', '#1890ff', '#ff7875', '#52c41a', '#8c8c8c']
const statusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  RETURNED: '已退回',
  APPROVED: '审核通过',
  WITHDRAWN: '已撤回',
}

const overview = computed(() => data.value?.overview)
const totalCount = computed(() => overview.value?.total ?? 0)
const pendingPercent = computed(() =>
  totalCount.value ? ((overview.value?.pending ?? 0) / totalCount.value) * 100 : 0,
)

const statusStats = computed(() =>
  statusOrder.map((status, index) => ({
    status,
    label: statusLabels[status],
    count: data.value?.byStatus.find((item) => item.status === status)?.count ?? 0,
    color: statusPalette[index],
  })),
)

const rankedTypes = computed(() => [...(data.value?.byType ?? [])].sort((a, b) => Number(b.amount) - Number(a.amount)))

const trendOption = computed<EChartsOption>(() => {
  const trend = data.value?.trend ?? []
  const amounts = trendMetric.value === 'amounts'
  return {
    animationDuration: 350,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: amounts ? 'line' : 'shadow' },
      formatter: (params: unknown) => {
        const list = Array.isArray(params) ? params : [params]
        const item = list[0] as { axisValue?: string; value?: number }
        const value = Number(item?.value ?? 0)
        return `${item?.axisValue ?? ''}<br/>${amounts ? '申报金额' : '申报数量'}：${
          amounts ? `¥${formatAmount(value)}` : `${value} 笔`
        }`
      },
    },
    grid: { left: 54, right: 28, top: 26, bottom: 38 },
    xAxis: {
      type: 'category',
      data: trend.map((item) => item.month),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e8e8e8' } },
      axisLabel: { color: '#8c8c8c', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#8c8c8c',
        fontSize: 11,
        formatter: (value: number) => (amounts && value >= 10000 ? `¥${value / 10000}万` : String(value)),
      },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [
      amounts
        ? {
            type: 'line',
            smooth: 0.25,
            symbol: 'circle',
            symbolSize: 6,
            data: trend.map((item) => Number(item.amount)),
            lineStyle: { width: 2, color: '#52c41a' },
            itemStyle: { color: '#52c41a', borderColor: '#ffffff', borderWidth: 1 },
            areaStyle: { color: 'rgba(82, 196, 26, 0.10)' },
          }
        : {
            type: 'bar',
            data: trend.map((item) => item.count),
            barMaxWidth: 42,
            itemStyle: { color: '#4da3ff', borderRadius: [5, 5, 0, 0] },
          },
    ],
  }
})

function sparklineOption(values: number[], color: string, fill: string): EChartsOption {
  return {
    animation: false,
    grid: { left: 0, right: 0, top: 3, bottom: 0 },
    xAxis: { type: 'category', show: false, boundaryGap: false },
    yAxis: { type: 'value', show: false, min: 'dataMin' },
    series: [
      {
        type: 'line',
        data: values,
        symbol: 'none',
        smooth: 0.35,
        lineStyle: { width: 1.5, color },
        areaStyle: { color: fill },
      },
    ],
  }
}

const countSparkOption = computed(() =>
  sparklineOption(
    (data.value?.trend ?? []).map((item) => item.count),
    '#69b1ff',
    'rgba(24, 144, 255, 0.12)',
  ),
)
const amountSparkOption = computed(() =>
  sparklineOption(
    (data.value?.trend ?? []).map((item) => Number(item.amount)),
    '#73d13d',
    'rgba(82, 196, 26, 0.10)',
  ),
)

const statusOption = computed<EChartsOption>(() => ({
  animationDuration: 350,
  tooltip: {
    trigger: 'item',
    formatter: '{b}<br/>{c} 笔（{d}%）',
  },
  series: [
    {
      type: 'pie',
      radius: ['66%', '83%'],
      center: ['50%', '50%'],
      itemStyle: { borderColor: '#ffffff', borderWidth: 2 },
      label: { show: false },
      data: statusStats.value.map((item) => ({
        name: item.label,
        value: item.count,
        itemStyle: { color: item.color },
      })),
    },
  ],
}))

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    data.value = await fetchDashboard()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '工作台数据加载失败'
  } finally {
    loading.value = false
  }
}

function openDeclaration(no: string) {
  router.push({ path: '/declarations', query: { declarationNo: no } })
}

function openStatus(status: string) {
  router.push({ path: '/declarations', query: { status } })
}

onMounted(load)
</script>

<template>
  <div class="page dashboard-page">
    <header class="page-header dashboard-page-header">
      <div>
        <h1 class="page-title">分析页</h1>
        <p class="page-description">
          {{ auth.user?.displayName }}，以下数据按当前账号的数据权限实时汇总，金额均为课程演示模拟金额。
        </p>
      </div>
      <div class="toolbar-inline">
        <el-button :loading="loading" @click="load">刷新数据</el-button>
        <el-button
          v-if="auth.hasPerm('declaration:write')"
          type="primary"
          @click="router.push('/declarations?create=1')"
        >
          新建申报
        </el-button>
      </div>
    </header>

    <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" style="margin-bottom: 16px">
      <template #default>
        <el-button size="small" style="margin-left: 8px" @click="load">重试</el-button>
      </template>
    </el-alert>

    <div v-loading="loading" class="dashboard-body">
      <section class="dashboard-kpis" aria-label="核心业务指标">
        <article class="metric-card">
          <div class="metric-head">
            <span>申报业务总量</span>
            <span class="metric-info" title="当前账号可见范围内的全部申报记录数">i</span>
          </div>
          <div class="metric-value">{{ overview?.total ?? '-' }}</div>
          <div class="metric-visual">
            <EChart v-if="data" :option="countSparkOption" height="42px" />
            <div class="metric-caption">
              <span>近 6 个月</span>
              <strong>当前账号可见范围</strong>
            </div>
          </div>
        </article>

        <article class="metric-card">
          <div class="metric-head">
            <span>待审核业务</span>
            <span class="metric-info" title="当前可见范围内状态为待审核的申报">i</span>
          </div>
          <div class="metric-value">{{ overview?.pending ?? '-' }}</div>
          <div class="metric-visual">
            <div class="metric-progress" role="progressbar" aria-label="待审核业务占申报总量">
              <span :style="{ width: `${pendingPercent.toFixed(1)}%` }" />
            </div>
            <div class="metric-caption">
              <span>占申报总量</span>
              <strong>{{ pendingPercent.toFixed(1) }}%</strong>
            </div>
          </div>
        </article>

        <article class="metric-card">
          <div class="metric-head">
            <span>审核通过</span>
            <span class="metric-info" title="通过率 = 审核通过 ÷（审核通过 + 已退回）">i</span>
          </div>
          <div class="metric-value">{{ overview?.approved ?? '-' }}</div>
          <div class="metric-visual">
            <div class="metric-progress is-green" role="progressbar" aria-label="审核通过率">
              <span :style="{ width: `${overview?.approvalRate ?? 0}%` }" />
            </div>
            <div class="metric-caption">
              <span>审核结果中的通过率</span>
              <strong>{{ overview?.approvalRate != null ? `${overview.approvalRate}%` : '-' }}</strong>
            </div>
          </div>
        </article>

        <article class="metric-card">
          <div class="metric-head">
            <span>申报金额合计</span>
            <span class="metric-info" title="当前可见范围内申报单的模拟金额字段合计">i</span>
          </div>
          <div class="metric-value is-amount">
            {{ formatAmount(overview?.totalAmount) }}
            <span class="metric-unit">元</span>
          </div>
          <div class="metric-visual">
            <EChart v-if="data" :option="amountSparkOption" height="42px" />
            <div class="metric-caption">
              <span>近 6 个月</span>
              <strong>模拟金额</strong>
            </div>
          </div>
        </article>
      </section>

      <section class="analysis-panel" aria-labelledby="trendHeading">
        <div class="analysis-head">
          <div class="analysis-tabs" role="tablist" aria-label="趋势指标">
            <button
              class="analysis-tab"
              :class="{ active: trendMetric === 'counts' }"
              type="button"
              role="tab"
              :aria-selected="trendMetric === 'counts'"
              @click="trendMetric = 'counts'"
            >
              申报数量
            </button>
            <button
              class="analysis-tab"
              :class="{ active: trendMetric === 'amounts' }"
              type="button"
              role="tab"
              :aria-selected="trendMetric === 'amounts'"
              @click="trendMetric = 'amounts'"
            >
              申报金额
            </button>
          </div>
          <div class="analysis-meta">
            <span id="trendHeading">统计区间</span>
            <span class="analysis-meta-tag">近 6 个月</span>
          </div>
        </div>
        <div class="analysis-body">
          <div class="analysis-chart">
            <EChart v-if="data" :option="trendOption" height="318px" />
            <div v-else class="empty-state">暂无趋势数据</div>
          </div>
          <aside class="analysis-side" aria-label="申报类型金额排名">
            <h2 class="analysis-side-title">申报类型金额排名</h2>
            <ol class="rank-list">
              <li v-for="(item, index) in rankedTypes" :key="item.typeName" class="rank-item">
                <span class="rank-number">{{ index + 1 }}</span>
                <span class="rank-content">
                  <span class="rank-label" :title="item.typeName">{{ item.typeName }}</span>
                  <span class="rank-meta">{{ item.count }} 笔申报</span>
                </span>
                <span class="rank-value">¥{{ formatAmount(item.amount) }}</span>
              </li>
              <li v-if="!rankedTypes.length" class="empty-state">暂无可统计的申报类型金额</li>
            </ol>
          </aside>
        </div>
      </section>

      <div class="dashboard-lower">
        <section class="card dashboard-table-card" aria-labelledby="recentHeading">
          <div class="card-header">
            <h2 class="card-title" id="recentHeading">待办申报</h2>
            <el-button text type="primary" @click="openStatus('PENDING_REVIEW')">查看全部</el-button>
          </div>
          <el-table :data="data?.todos ?? []" size="small" style="width: 100%">
            <el-table-column prop="declarationNo" label="申报编号" width="154">
              <template #default="{ row }">
                <button class="table-link" type="button" @click="openDeclaration(row.declarationNo)">
                  {{ row.declarationNo }}
                </button>
              </template>
            </el-table-column>
            <el-table-column prop="taxpayerName" label="纳税人" min-width="160" show-overflow-tooltip />
            <el-table-column prop="declarationTypeName" label="申报类型" min-width="130" show-overflow-tooltip />
            <el-table-column label="状态" width="96">
              <template #default="{ row }">
                <span class="status-tag" :class="declarationStatus(row.status).className">
                  {{ declarationStatus(row.status).label }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.declaredAmount) }}</template>
            </el-table-column>
            <el-table-column label="提交时间" width="145">
              <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
            </el-table-column>
          </el-table>
          <div v-if="!loading && !data?.todos?.length" class="empty-state">当前没有待审核申报</div>
        </section>

        <section class="card status-panel" aria-labelledby="statusHeading">
          <div class="card-header">
            <h2 class="card-title" id="statusHeading">申报状态分布</h2>
            <span class="analysis-meta-tag">共 {{ totalCount }} 笔</span>
          </div>
          <div class="status-body">
            <div class="status-chart-wrap">
              <EChart v-if="data" :option="statusOption" height="188px" />
              <div class="donut-center" aria-hidden="true">
                <strong>{{ totalCount }}</strong>
                <span>申报总量</span>
              </div>
            </div>
            <div class="status-legend" aria-label="状态图例">
              <div v-for="item in statusStats" :key="item.status" class="status-legend-item">
                <span class="status-swatch" :style="{ background: item.color }" />
                <span class="status-legend-name">{{ item.label }}</span>
                <span class="status-legend-value">{{ item.count }}</span>
              </div>
            </div>
            <div class="queue-list" aria-label="我的工作事项">
              <button class="queue-link" type="button" @click="openStatus('DRAFT')">
                <span class="queue-label">草稿</span>
                <span class="queue-value">{{ overview?.draft ?? 0 }}</span>
              </button>
              <button class="queue-link" type="button" @click="openStatus('RETURNED')">
                <span class="queue-label">已退回</span>
                <span class="queue-value">{{ overview?.returned ?? 0 }}</span>
              </button>
              <button class="queue-link" type="button" @click="openStatus('WITHDRAWN')">
                <span class="queue-label">已撤回</span>
                <span class="queue-value">{{ overview?.withdrawn ?? 0 }}</span>
              </button>
              <button class="queue-link" type="button" @click="openStatus('PENDING_REVIEW')">
                <span class="queue-label">待审核</span>
                <span class="queue-value">{{ overview?.pending ?? 0 }}</span>
              </button>
            </div>
          </div>
        </section>
      </div>

      <section v-if="auth.hasPerm('audit:read')" class="card dashboard-log-card">
        <div class="card-header">
          <h2 class="card-title">最近操作</h2>
          <el-button text type="primary" @click="router.push('/audit-logs')">查看日志</el-button>
        </div>
        <el-table :data="data?.recentLogs?.items ?? []" size="small">
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="140" />
          <el-table-column prop="operationType" label="操作类型" width="190" />
          <el-table-column prop="summary" label="说明" min-width="260" show-overflow-tooltip />
          <el-table-column prop="result" label="结果" width="90">
            <template #default="{ row }">
              <span class="status-tag" :class="row.result === 'SUCCESS' ? 'status-approved' : 'status-returned'">
                {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!loading && !data?.recentLogs?.items?.length" class="empty-state">暂无操作记录</div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.dashboard-body {
  min-height: 320px;
}

.table-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--tax-primary);
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  cursor: pointer;
}

.dashboard-log-card {
  margin-top: 16px;
}

.queue-link {
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}

@media (max-width: 520px) {
  .dashboard-page-header .toolbar-inline {
    width: 100%;
  }
}
</style>

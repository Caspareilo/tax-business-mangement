<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import {
  fetchByStatus,
  fetchByTaxpayerType,
  fetchByType,
  fetchOverview,
  fetchTrend,
  type StatsQuery,
} from '@/api/statistics'
import { fetchDeclarationTypeOptions } from '@/api/declarationTypes'
import type { DeclarationType, Overview, StatusStat, TaxpayerTypeStat, TrendStat, TypeStat } from '@/types/models'
import { DECLARATION_STATUS, TAXPAYER_TYPE, formatAmount } from '@/utils/dict'
import EChart from '@/components/EChart.vue'

const loading = ref(false)
const loadError = ref('')
const types = ref<DeclarationType[]>([])
const filters = reactive({
  dateRange: [] as string[],
  typeId: null as number | null,
  months: 6,
})

const overview = ref<Overview | null>(null)
const trend = ref<TrendStat[]>([])
const byStatus = ref<StatusStat[]>([])
const byType = ref<TypeStat[]>([])
const byTaxpayerType = ref<TaxpayerTypeStat[]>([])

const palette = ['#1890ff', '#52c41a', '#faad14', '#ff4d4f', '#8c8c8c', '#69b1ff']

const statusLabels: Record<string, string> = Object.fromEntries(
  Object.entries(DECLARATION_STATUS).map(([key, value]) => [key, value.label]),
)

const trendOption = computed<EChartsOption>(() => ({
  color: ['#1890ff', '#52c41a'],
  tooltip: { trigger: 'axis' },
  legend: { data: ['申报数量', '模拟金额'], left: 'center', top: 0, textStyle: { color: '#5f696e', fontSize: 12 } },
  grid: { left: 50, right: 64, top: 44, bottom: 32 },
  xAxis: {
    type: 'category',
    data: trend.value.map((item) => item.month),
    axisLine: { lineStyle: { color: '#e8e8e8' } },
    axisTick: { show: false },
    axisLabel: { color: '#8c8c8c', fontSize: 12 },
  },
  yAxis: [
    {
      type: 'value',
      name: '数量',
      nameTextStyle: { color: '#8c8c8c', fontSize: 12 },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      axisLabel: { color: '#8c8c8c', fontSize: 12 },
    },
    {
      type: 'value',
      name: '金额',
      nameTextStyle: { color: '#8c8c8c', fontSize: 12 },
      splitLine: { show: false },
      axisLabel: {
        color: '#8c8c8c',
        fontSize: 12,
        formatter: (value: number) => (value >= 10000 ? `${value / 10000}万` : String(value)),
      },
    },
  ],
  series: [
    {
      name: '申报数量',
      type: 'line',
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2 },
      data: trend.value.map((item) => item.count),
    },
    {
      name: '模拟金额',
      type: 'bar',
      yAxisIndex: 1,
      barWidth: 20,
      itemStyle: { color: '#73d13d', borderRadius: [2, 2, 0, 0] },
      data: trend.value.map((item) => Number(item.amount)),
    },
  ],
}))

const statusOption = computed<EChartsOption>(() => ({
  color: palette,
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 条（{d}%）' },
  legend: { bottom: 0, left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: '#5f696e', fontSize: 12 } },
  series: [
    {
      type: 'pie',
      radius: ['45%', '68%'],
      center: ['50%', '44%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: byStatus.value.map((item) => ({ name: statusLabels[item.status] ?? item.status, value: item.count })),
    },
  ],
}))

const typeOption = computed<EChartsOption>(() => ({
  color: ['#1890ff'],
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}<br/>申报 {c} 条' },
  grid: { left: 100, right: 30, top: 10, bottom: 24 },
  xAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f0f0f0' } },
    axisLabel: { color: '#8c8c8c', fontSize: 12 },
  },
  yAxis: {
    type: 'category',
    data: byType.value.map((item) => item.typeName),
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#e8e8e8' } },
    axisLabel: { color: '#5f696e', fontSize: 12 },
  },
  series: [
    {
      type: 'bar',
      barWidth: 16,
      itemStyle: { color: '#4da3ff', borderRadius: [0, 3, 3, 0] },
      data: byType.value.map((item) => item.count),
    },
  ],
}))

const taxpayerTypeOption = computed<EChartsOption>(() => ({
  color: ['#1890ff', '#52c41a', '#faad14'],
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 条（{d}%）' },
  legend: { bottom: 0, left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: '#5f696e', fontSize: 12 } },
  series: [
    {
      type: 'pie',
      radius: ['0%', '62%'],
      center: ['50%', '42%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { color: '#5f696e', fontSize: 12 },
      data: byTaxpayerType.value.map((item) => ({
        name: TAXPAYER_TYPE[item.taxpayerType] ?? item.taxpayerType,
        value: item.count,
      })),
    },
  ],
}))

function params(): StatsQuery {
  return {
    from: filters.dateRange?.[0] || undefined,
    to: filters.dateRange?.[1] || undefined,
    typeId: filters.typeId,
    months: filters.months,
  }
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [overviewResult, trendResult, statusResult, typeResult, taxpayerTypeResult] = await Promise.all([
      fetchOverview(params()),
      fetchTrend(params()),
      fetchByStatus(params()),
      fetchByType(params()),
      fetchByTaxpayerType(params()),
    ])
    overview.value = overviewResult
    trend.value = trendResult
    byStatus.value = statusResult
    byType.value = typeResult
    byTaxpayerType.value = taxpayerTypeResult
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '统计数据加载失败'
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

function reset() {
  filters.dateRange = []
  filters.typeId = null
  filters.months = 6
  load()
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
        <h1 class="page-title">统计分析</h1>
        <p class="page-description">按申报期间与类型筛选，统计结果来自业务库实时聚合，金额仅用于教学演示</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div
      class="filter-bar card"
      style="border-radius: var(--tax-radius); margin-bottom: 14px; border: 1px solid var(--tax-border)"
    >
      <div class="filter-item">
        <label>申报期间</label>
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          style="width: 280px"
        />
      </div>
      <div class="filter-item">
        <label>申报类型</label>
        <el-select v-model="filters.typeId" placeholder="全部类型" clearable style="width: 190px">
          <el-option v-for="type in types" :key="type.id" :label="type.typeName" :value="type.id" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>趋势范围</label>
        <el-select v-model="filters.months" style="width: 120px">
          <el-option label="近 6 个月" :value="6" />
          <el-option label="近 12 个月" :value="12" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" @click="load">应用筛选</el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom: 14px"
    />

    <div v-loading="loading">
      <div class="kpi-grid">
        <div class="kpi-card">
          <div class="kpi-label">申报总量</div>
          <div class="kpi-value">{{ overview?.total ?? '-' }}</div>
          <div class="kpi-hint">符合条件的全部申报</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">审核通过</div>
          <div class="kpi-value" style="color: var(--tax-success)">{{ overview?.approved ?? '-' }}</div>
          <div class="kpi-hint">通过率 {{ overview?.approvalRate != null ? `${overview.approvalRate}%` : '-' }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">待审核 / 已退回</div>
          <div class="kpi-value">
            {{ overview?.pending ?? '-' }}
            <span style="font-size: 16px; color: var(--tax-muted); font-weight: 400"
              >/ {{ overview?.returned ?? '-' }}</span
            >
          </div>
          <div class="kpi-hint">待处理与需修改的申报</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">模拟金额合计</div>
          <div class="kpi-value" style="font-size: 22px">¥ {{ formatAmount(overview?.totalAmount) }}</div>
          <div class="kpi-hint">非真实税额口径</div>
        </div>
      </div>

      <div class="section-grid">
        <div class="card">
          <div class="card-header">
            <span class="card-title">申报趋势</span>
            <span class="text-muted" style="font-size: 12px">按申报期间起始月份汇总</span>
          </div>
          <div class="card-body">
            <EChart v-if="trend.length" :option="trendOption" height="320px" />
            <div v-else class="empty-state">暂无趋势数据</div>
          </div>
        </div>
        <div class="card">
          <div class="card-header">
            <span class="card-title">状态构成</span>
          </div>
          <div class="card-body">
            <EChart v-if="byStatus.length" :option="statusOption" height="320px" />
            <div v-else class="empty-state">暂无状态数据</div>
          </div>
        </div>
      </div>

      <div class="section-grid">
        <div class="card">
          <div class="card-header">
            <span class="card-title">申报类型分布</span>
          </div>
          <div class="card-body">
            <EChart v-if="byType.length" :option="typeOption" height="280px" />
            <div v-else class="empty-state">暂无类型数据</div>
          </div>
        </div>
        <div class="card">
          <div class="card-header">
            <span class="card-title">主体类型构成</span>
          </div>
          <div class="card-body">
            <EChart v-if="byTaxpayerType.length" :option="taxpayerTypeOption" height="280px" />
            <div v-else class="empty-state">暂无主体类型数据</div>
          </div>
        </div>
      </div>

      <div class="card" style="margin-top: 16px">
        <div class="card-header">
          <span class="card-title">类型明细</span>
        </div>
        <el-table :data="byType" size="small">
          <el-table-column prop="typeName" label="申报类型" min-width="200" />
          <el-table-column prop="count" label="申报数量" width="140" align="right" />
          <el-table-column label="模拟金额合计" width="200" align="right">
            <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="数量占比" min-width="180">
            <template #default="{ row }">
              <el-progress
                :percentage="overview?.total ? Math.round((row.count / overview.total) * 100) : 0"
                :stroke-width="8"
                color="#1890ff"
              />
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!loading && !byType.length" class="empty-state">暂无统计数据</div>
      </div>
    </div>
  </div>
</template>

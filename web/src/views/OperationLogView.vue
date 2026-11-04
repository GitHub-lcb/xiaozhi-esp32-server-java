<template>
  <div class="operation-log-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <a-space wrap>
          <a-input
            v-model:value="query.module"
            :placeholder="t('operationLog.modulePlaceholder')"
            allow-clear
            style="width: 160px"
            @press-enter="handleSearch"
          />
          <a-input
            v-model:value="query.operation"
            :placeholder="t('operationLog.operationPlaceholder')"
            allow-clear
            style="width: 160px"
            @press-enter="handleSearch"
          />
          <a-input-number
            v-model:value="query.userId"
            :min="1"
            :precision="0"
            :placeholder="t('operationLog.userIdPlaceholder')"
            style="width: 140px"
          />
          <a-select
            v-model:value="query.success"
            allow-clear
            :placeholder="t('operationLog.resultPlaceholder')"
            style="width: 140px"
          >
            <a-select-option :value="true">{{ t('operationLog.success') }}</a-select-option>
            <a-select-option :value="false">{{ t('operationLog.failure') }}</a-select-option>
          </a-select>
          <a-range-picker
            v-model:value="timeRange"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 380px"
          />
          <a-button type="primary" @click="handleSearch">{{ t('common.search') }}</a-button>
          <a-button @click="handleReset">{{ t('operationLog.reset') }}</a-button>
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1180 }"
        row-key="id"
        size="middle"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'user'">
            <span>{{ userLabel(record) }}</span>
          </template>
          <template v-else-if="column.key === 'success'">
            <a-tag :color="record.success ? 'green' : 'red'">{{ resultLabel(record.success) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">
              {{ t('operationLog.detail') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="detailVisible" :title="t('operationLog.detailTitle')" :footer="null" width="760px">
      <a-descriptions v-if="detail" :column="2" bordered size="small">
        <a-descriptions-item :label="t('operationLog.id')">{{ detail.id }}</a-descriptions-item>
        <a-descriptions-item :label="t('operationLog.user')">{{ userLabel(detail) }}</a-descriptions-item>
        <a-descriptions-item :label="t('operationLog.module')">{{ detail.module || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="t('operationLog.operation')">
          {{ detail.operation || '-' }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('operationLog.method')">{{ detail.method || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="t('operationLog.costMs')">{{ detail.costMs ?? '-' }}</a-descriptions-item>
        <a-descriptions-item :label="t('operationLog.createTime')">
          {{ detail.createTime || '-' }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('operationLog.result')">
          <a-tag :color="detail.success ? 'green' : 'red'">{{ resultLabel(detail.success) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="t('operationLog.url')" :span="2">
          {{ detail.url || '-' }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('operationLog.handler')" :span="2">
          {{ detail.handler || '-' }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('operationLog.params')" :span="2">
          <pre class="detail-pre">{{ detail.params || '-' }}</pre>
        </a-descriptions-item>
        <a-descriptions-item v-if="detail.errorMsg" :label="t('operationLog.errorMsg')" :span="2">
          <pre class="detail-pre detail-pre-error">{{ detail.errorMsg }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { queryOperationLogs } from '@/services/operationLog'
import type { OperationLog } from '@/types/operationLog'

const { t } = useI18n()

const rows = ref<OperationLog[]>([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref<OperationLog | null>(null)
// a-range-picker 配 value-format 后回传的就是字符串数组，直接当 startTime / endTime 用
const timeRange = ref<[string, string] | null>(null)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  module: '',
  operation: '',
  userId: undefined as number | undefined,
  success: undefined as boolean | undefined,
})

const columns = computed(() => [
  { title: t('operationLog.createTime'), dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: t('operationLog.user'), key: 'user', width: 140 },
  { title: t('operationLog.module'), dataIndex: 'module', key: 'module', width: 120 },
  { title: t('operationLog.operation'), dataIndex: 'operation', key: 'operation', width: 160, ellipsis: true },
  { title: t('operationLog.method'), dataIndex: 'method', key: 'method', width: 100 },
  { title: t('operationLog.url'), dataIndex: 'url', key: 'url', ellipsis: true },
  { title: t('operationLog.result'), key: 'success', width: 90 },
  { title: t('operationLog.costMs'), dataIndex: 'costMs', key: 'costMs', width: 110 },
  { title: t('operationLog.actionLabel'), key: 'action', width: 90, fixed: 'right' as const },
])

const pagination = computed<TablePaginationConfig>(() => ({
  current: query.pageNo,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => t('table.total', { total: value }),
}))

/** 用户名优先，缺失时退化成用户 ID，再缺就显示占位符 */
function userLabel(record: OperationLog): string {
  if (record.username) return record.username
  if (record.userId !== undefined && record.userId !== null) return String(record.userId)
  return '-'
}

function resultLabel(success?: boolean): string {
  return success ? t('operationLog.success') : t('operationLog.failure')
}

async function loadData() {
  loading.value = true
  try {
    const response = await queryOperationLogs({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      // 空串和 null 不带给后端，避免把模糊条件拼成 WHERE module LIKE '%%'
      module: query.module.trim() || undefined,
      operation: query.operation.trim() || undefined,
      userId: query.userId ?? undefined,
      success: query.success,
      startTime: timeRange.value?.[0],
      endTime: timeRange.value?.[1],
    })
    rows.value = response.data?.list || []
    total.value = response.data?.total || 0
  } catch (error) {
    message.error(t('operationLog.loadFailed'))
  } finally {
    loading.value = false
  }
}

/** 点搜索回到第一页，否则筛完停在越界页会看到空表 */
function handleSearch() {
  query.pageNo = 1
  loadData()
}

function handleReset() {
  query.module = ''
  query.operation = ''
  query.userId = undefined
  query.success = undefined
  timeRange.value = null
  query.pageNo = 1
  loadData()
}

function onTableChange(p: TablePaginationConfig) {
  query.pageNo = p.current || 1
  query.pageSize = p.pageSize || 10
  loadData()
}

function openDetail(record: OperationLog) {
  detail.value = record
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.operation-log-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.operation-log-view .detail-pre {
  margin: 0;
  padding: 8px;
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.6;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 4px;
}

.operation-log-view .detail-pre-error {
  color: #cf1322;
  background: rgba(207, 19, 34, 0.06);
}
</style>

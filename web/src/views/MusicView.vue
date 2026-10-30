<template>
  <div class="music-view">
    <p class="muted page-tip">{{ t('music.tip') }}</p>

    <!-- 统计概览 -->
    <a-card :bordered="false" class="stats-card">
      <a-row :gutter="16">
        <a-col :xs="12" :md="6">
          <a-statistic :title="t('music.totalSongs')" :value="stats.total" />
        </a-col>
        <a-col :xs="12" :md="6">
          <a-statistic :title="t('music.enabledSongs')" :value="stats.enabled" />
          <div class="stat-extra">{{ t('music.disabledSongs') }} {{ stats.disabled }}</div>
        </a-col>
        <a-col :xs="12" :md="6">
          <a-statistic :title="t('music.totalPlays')" :value="stats.totalPlayCount" />
          <div class="stat-extra">{{ t('music.todayPlays') }} {{ stats.todayPlayCount }}</div>
        </a-col>
        <a-col :xs="12" :md="6">
          <a-statistic :title="t('music.totalSize')" :value="formatBytes(stats.totalSizeBytes)" />
          <div class="stat-extra">{{ t('music.totalDuration') }} {{ formatSecondsToClock(stats.totalDurationSec) }}</div>
        </a-col>
      </a-row>
    </a-card>

    <a-card :bordered="false" class="music-card">
      <!-- 工具条 -->
      <div class="toolbar">
        <a-input
          v-model:value="query.keyword"
          :placeholder="t('music.searchPlaceholder')"
          allow-clear
          class="search-input"
          @press-enter="handleSearch"
        />
        <a-select v-model:value="query.state" class="state-select" :options="stateOptions" @change="handleSearch" />
        <a-button type="primary" @click="handleSearch">{{ t('common.search') }}</a-button>
        <a-button :loading="loading" @click="refresh">{{ t('music.refresh') }}</a-button>
        <a-button v-if="canSync" :loading="syncing" @click="handleSync">{{ t('music.sync') }}</a-button>
        <div v-if="canUpload" class="toolbar-right">
          <span class="muted">{{ t('music.uploadTip') }}</span>
          <a-upload
            accept=".mp3"
            multiple
            :show-upload-list="false"
            :before-upload="beforeUpload"
            :custom-request="customUpload"
          >
            <a-button type="primary" :loading="uploading">{{ t('music.upload') }}</a-button>
          </a-upload>
        </div>
      </div>

      <!-- 加载失败：错误态 + 重试 -->
      <a-alert v-if="loadError" type="error" show-icon class="load-error" :message="t('music.loadFailed')">
        <template #action>
          <a-button size="small" @click="refresh">{{ t('music.retry') }}</a-button>
        </template>
      </a-alert>

      <a-table
        v-else
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1200 }"
        row-key="musicId"
        size="middle"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <div>{{ record.title || record.fileName }}</div>
            <div class="muted" :title="t('music.fileName')">{{ record.fileName }}</div>
          </template>
          <template v-else-if="column.key === 'duration'">
            {{ formatSecondsToClock(record.durationSec) }}
          </template>
          <template v-else-if="column.key === 'size'">
            {{ formatBytes(record.sizeBytes) }}
          </template>
          <template v-else-if="column.key === 'state'">
            <a-tag :color="record.state === '1' ? 'green' : 'default'">
              {{ record.state === '1' ? t('music.stateEnabled') : t('music.stateDisabled') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-if="canPreview"
                type="link"
                size="small"
                :loading="previewLoadingId === record.musicId"
                @click="openPreview(record)"
              >
                {{ t('music.preview') }}
              </a-button>
              <a-button v-if="canUpdate" type="link" size="small" @click="openEdit(record)">
                {{ t('music.edit') }}
              </a-button>
              <a-button v-if="canUpdate" type="link" size="small" @click="toggleState(record)">
                {{ record.state === '1' ? t('music.disable') : t('music.enable') }}
              </a-button>
              <a-popconfirm v-if="canDelete" :title="t('music.deleteConfirm')" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger>{{ t('music.delete') }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
        <template #emptyText>
          <a-empty :description="emptyText" />
        </template>
      </a-table>

      <!-- 点播记录 -->
      <a-divider orientation="left">{{ t('music.recentPlays') }}</a-divider>

      <a-alert v-if="playsError" type="error" show-icon :message="t('music.playsLoadFailed')">
        <template #action>
          <a-button size="small" @click="loadPlays">{{ t('music.retry') }}</a-button>
        </template>
      </a-alert>

      <a-table
        v-else
        :columns="playColumns"
        :data-source="plays"
        :loading="playsLoading"
        :pagination="playPagination"
        :scroll="{ x: 800 }"
        row-key="logId"
        size="small"
        @change="onPlayTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'device'">
            <div>{{ record.deviceName || record.deviceId || '-' }}</div>
            <div class="muted">{{ record.deviceId }}</div>
          </template>
          <template v-else-if="column.key === 'songName'">
            {{ record.musicTitle || record.songName || '-' }}
          </template>
        </template>
        <template #emptyText>
          <a-empty :description="t('music.playsEmpty')" />
        </template>
      </a-table>
    </a-card>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="editVisible"
      :title="t('music.editTitle')"
      :confirm-loading="saving"
      :ok-text="t('common.save')"
      :cancel-text="t('common.cancel')"
      @ok="saveEdit"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('music.title')" required>
          <a-input v-model:value="editForm.title" :maxlength="100" show-count />
        </a-form-item>
        <a-form-item :label="t('music.artist')">
          <a-input v-model:value="editForm.artist" :maxlength="100" />
        </a-form-item>
        <a-form-item :label="t('music.aliases')" :extra="t('music.aliasesTip')">
          <a-input v-model:value="editForm.aliases" :maxlength="300" />
        </a-form-item>
        <a-form-item :label="t('music.state')">
          <a-switch v-model:checked="editForm.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 试听弹窗：关闭即停 -->
    <a-modal v-model:open="previewVisible" :title="t('music.previewTitle')" :footer="null" destroy-on-close>
      <div class="preview-title">{{ previewName }}</div>
      <audio v-if="previewUrl" controls autoplay :src="previewUrl" class="audio-player" @error="onAudioError"></audio>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { TableColumnsType, TablePaginationConfig, UploadProps } from 'ant-design-vue'
import {
  deleteMusic,
  getMusicAudioUrl,
  queryMusic,
  queryMusicPlays,
  queryMusicStats,
  syncMusic,
  updateMusic,
  uploadMusic,
} from '@/services/music'
import type { MusicFile, MusicPlayLog, MusicSortField, MusicStats } from '@/types/music'
import { useUserStore } from '@/store/user'
import { formatBytes, formatSecondsToClock } from '@/utils/format'
import { getResourceUrl } from '@/utils/resource'

const { t } = useI18n()
const userStore = useUserStore()

// 写操作按钮按权限键控制显隐，admin 在 store 里自动返回 true
const canPreview = computed(() => userStore.hasPermission('system:file:api:preview'))
const canUpdate = computed(() => userStore.hasPermission('system:file:api:update'))
const canDelete = computed(() => userStore.hasPermission('system:file:api:delete'))
const canSync = computed(() => userStore.hasPermission('system:file:api:sync'))
const canUpload = computed(() => userStore.hasPermission('system:file:api:upload'))

// ---------- 列表 ----------
const rows = ref<MusicFile[]>([])
const total = ref(0)
const loading = ref(false)
const loadError = ref(false)
const query = reactive<{
  pageNo: number
  pageSize: number
  keyword: string
  state: string
  sortBy: MusicSortField
  sortOrder: 'asc' | 'desc'
}>({ pageNo: 1, pageSize: 10, keyword: '', state: '', sortBy: 'title', sortOrder: 'asc' })

const stats = reactive<MusicStats>({
  total: 0,
  enabled: 0,
  disabled: 0,
  totalPlayCount: 0,
  totalDurationSec: 0,
  totalSizeBytes: 0,
  todayPlayCount: 0,
})

const stateOptions = computed(() => [
  { label: t('music.stateAll'), value: '' },
  { label: t('music.stateEnabled'), value: '1' },
  { label: t('music.stateDisabled'), value: '0' },
])

const hasFilters = computed(() => Boolean(query.keyword.trim() || query.state))
const emptyText = computed(() => (hasFilters.value ? t('music.emptySearch') : t('music.empty')))

// 列 key 与后端 sortBy 白名单的映射：更新时间列按 createTime 排序
const SORT_COLUMN_TO_FIELD: Record<string, MusicSortField> = {
  title: 'title',
  duration: 'durationSec',
  playCount: 'playCount',
  updateTime: 'createTime',
}

function sortOrderFor(columnKey: string) {
  if (SORT_COLUMN_TO_FIELD[columnKey] !== query.sortBy) return null
  return query.sortOrder === 'asc' ? ('ascend' as const) : ('descend' as const)
}

const columns = computed<TableColumnsType>(() => [
  {
    title: t('music.title'),
    dataIndex: 'title',
    key: 'title',
    width: 220,
    sorter: true,
    sortOrder: sortOrderFor('title'),
  },
  { title: t('music.artist'), dataIndex: 'artist', key: 'artist', width: 140, ellipsis: true },
  { title: t('music.aliases'), dataIndex: 'aliases', key: 'aliases', width: 160, ellipsis: true },
  {
    title: t('music.duration'),
    dataIndex: 'durationSec',
    key: 'duration',
    width: 100,
    sorter: true,
    sortOrder: sortOrderFor('duration'),
  },
  { title: t('music.size'), dataIndex: 'sizeBytes', key: 'size', width: 100 },
  {
    title: t('music.playCount'),
    dataIndex: 'playCount',
    key: 'playCount',
    width: 100,
    sorter: true,
    sortOrder: sortOrderFor('playCount'),
  },
  { title: t('music.state'), dataIndex: 'state', key: 'state', width: 100 },
  {
    title: t('music.updateTime'),
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: 180,
    sorter: true,
    sortOrder: sortOrderFor('updateTime'),
  },
  { title: t('music.action'), key: 'action', width: 260, fixed: 'right' },
])

const pagination = computed<TablePaginationConfig>(() => ({
  current: query.pageNo,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (count: number) => t('table.total', { total: count }),
  pageSizeOptions: ['10', '20', '50', '100'],
}))

async function loadData() {
  loading.value = true
  loadError.value = false
  try {
    const response = await queryMusic({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      state: query.state || undefined,
      sortBy: query.sortBy,
      sortOrder: query.sortOrder,
    })
    rows.value = response.data?.list || []
    total.value = response.data?.total || 0
  } catch {
    rows.value = []
    total.value = 0
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const response = await queryMusicStats()
    if (response.data) Object.assign(stats, response.data)
  } catch {
    // 统计失败不阻塞主列表，卡片保留上一次的数字
  }
}

function handleSearch() {
  query.pageNo = 1
  return loadData()
}

function onTableChange(p: TablePaginationConfig, _filters: unknown, sorter: unknown) {
  query.pageNo = p.current || 1
  query.pageSize = p.pageSize || 10
  const single = (Array.isArray(sorter) ? sorter[0] : sorter) as
    | { columnKey?: string; order?: string }
    | undefined
  const field = single?.columnKey ? SORT_COLUMN_TO_FIELD[single.columnKey] : undefined
  if (field && single?.order) {
    query.sortBy = field
    query.sortOrder = single.order === 'ascend' ? 'asc' : 'desc'
  } else {
    query.sortBy = 'title'
    query.sortOrder = 'asc'
  }
  return loadData()
}

// ---------- 点播记录 ----------
const plays = ref<MusicPlayLog[]>([])
const playsTotal = ref(0)
const playsLoading = ref(false)
const playsError = ref(false)
const playQuery = reactive({ pageNo: 1, pageSize: 10 })

const playColumns = computed<TableColumnsType>(() => [
  { title: t('music.playTime'), dataIndex: 'createTime', key: 'playTime', width: 180 },
  { title: t('music.device'), key: 'device', width: 220 },
  { title: t('music.keyword'), dataIndex: 'keyword', key: 'keyword', ellipsis: true },
  { title: t('music.songName'), key: 'songName', width: 180, ellipsis: true },
])

const playPagination = computed<TablePaginationConfig>(() => ({
  current: playQuery.pageNo,
  pageSize: playQuery.pageSize,
  total: playsTotal.value,
  showSizeChanger: true,
  showTotal: (count: number) => t('table.total', { total: count }),
  pageSizeOptions: ['10', '20', '50', '100'],
}))

async function loadPlays() {
  playsLoading.value = true
  playsError.value = false
  try {
    const response = await queryMusicPlays({
      pageNo: playQuery.pageNo,
      pageSize: playQuery.pageSize,
    })
    plays.value = response.data?.list || []
    playsTotal.value = response.data?.total || 0
  } catch {
    plays.value = []
    playsTotal.value = 0
    playsError.value = true
  } finally {
    playsLoading.value = false
  }
}

function onPlayTableChange(p: TablePaginationConfig) {
  playQuery.pageNo = p.current || 1
  playQuery.pageSize = p.pageSize || 10
  return loadPlays()
}

/** 列表、统计、点播记录一起刷新；单接口失败各自兜底，不互相影响 */
async function refresh() {
  await Promise.all([loadData(), loadStats(), loadPlays()])
}

// ---------- 同步目录 ----------
const syncing = ref(false)

async function handleSync() {
  syncing.value = true
  try {
    const response = await syncMusic()
    const result = response.data || { added: 0, removed: 0, total: 0 }
    message.success(t('music.syncSuccess', { added: result.added, removed: result.removed }))
    await refresh()
  } catch {
    message.error(t('music.syncFailed'))
  } finally {
    syncing.value = false
  }
}

// ---------- 上传 ----------
const uploadPending = ref(0)
const uploading = computed(() => uploadPending.value > 0)
let uploadChain: Promise<void> = Promise.resolve()

function isMp3(name: string) {
  return name.toLowerCase().endsWith('.mp3')
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  if (!isMp3(file.name)) {
    message.warning(t('music.uploadInvalid'))
    return false
  }
  return true
}

async function uploadOne(file: File) {
  uploadPending.value += 1
  try {
    await uploadMusic(file)
    message.success(`${file.name} ${t('music.uploadSuccess')}`)
    await refresh()
    return true
  } catch {
    message.error(`${file.name} ${t('music.uploadFailed')}`)
    return false
  } finally {
    uploadPending.value -= 1
  }
}

// AntD 多选时每个文件都会调一次 customRequest，这里串成队列逐首上传
const customUpload: UploadProps['customRequest'] = (options) => {
  const file = options.file as File
  uploadChain = uploadChain.then(async () => {
    const ok = await uploadOne(file)
    if (ok) {
      options.onSuccess?.({})
    } else {
      options.onError?.(new Error('upload failed'))
    }
  })
}

// ---------- 编辑 / 上下架 / 删除 ----------
const editVisible = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const editForm = reactive({ title: '', artist: '', aliases: '', enabled: true })

function openEdit(record: MusicFile) {
  editingId.value = record.musicId
  editForm.title = record.title || record.fileName
  editForm.artist = record.artist || ''
  editForm.aliases = record.aliases || ''
  editForm.enabled = record.state === '1'
  editVisible.value = true
}

function buildUpdate(record: MusicFile, state: string) {
  return updateMusic(record.musicId, {
    title: record.title || record.fileName,
    artist: record.artist,
    aliases: record.aliases,
    state,
  })
}

async function saveEdit() {
  const title = editForm.title.trim()
  if (!title) {
    message.warning(t('music.titleRequired'))
    return
  }
  if (editingId.value === undefined) return
  saving.value = true
  try {
    await updateMusic(editingId.value, {
      title,
      artist: editForm.artist.trim() || undefined,
      aliases: editForm.aliases.trim() || undefined,
      state: editForm.enabled ? '1' : '0',
    })
    message.success(t('music.saveSuccess'))
    editVisible.value = false
    await refresh()
  } catch {
    message.error(t('music.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function toggleState(record: MusicFile) {
  const nextState = record.state === '1' ? '0' : '1'
  try {
    await buildUpdate(record, nextState)
    message.success(nextState === '1' ? t('music.enabledSuccess') : t('music.disabledSuccess'))
    await refresh()
  } catch {
    message.error(t('music.saveFailed'))
  }
}

async function handleDelete(record: MusicFile) {
  try {
    await deleteMusic(record.musicId)
    message.success(t('music.deleteSuccess'))
    await refresh()
  } catch {
    message.error(t('music.deleteFailed'))
  }
}

// ---------- 试听 ----------
const previewVisible = ref(false)
const previewLoadingId = ref<number | null>(null)
const previewUrl = ref('')
const previewName = ref('')

async function openPreview(record: MusicFile) {
  previewLoadingId.value = record.musicId
  previewName.value = record.title || record.fileName
  try {
    const response = await getMusicAudioUrl(record.musicId)
    const url = response.data?.url
    if (!url) throw new Error('empty audio url')
    previewUrl.value = getResourceUrl(url) || ''
    previewVisible.value = true
  } catch {
    message.error(t('music.previewFailed'))
  } finally {
    previewLoadingId.value = null
  }
}

function onAudioError() {
  message.warning(t('music.notSupported'))
}

// 关闭弹窗（含点遮罩/ESC）时清空 src，避免音频在后台继续播放
watch(previewVisible, (visible) => {
  if (!visible) previewUrl.value = ''
})

onMounted(refresh)
</script>

<style scoped>
.music-view .page-tip {
  margin: 0 0 12px;
}

.music-view .stats-card {
  margin-bottom: 16px;
}

.music-view .stat-extra {
  margin-top: 8px;
  color: #8c8c8c;
  font-size: 12px;
}

.music-view .toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.music-view .search-input {
  width: 240px;
}

.music-view .toolbar-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.music-view .state-select {
  width: 140px;
}

.music-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}

.music-view .load-error {
  margin-bottom: 16px;
}

.music-view .preview-title {
  margin-bottom: 12px;
  font-weight: 500;
}

.music-view .audio-player {
  width: 100%;
}
</style>

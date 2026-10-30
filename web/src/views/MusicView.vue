<template>
  <div class="music-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <span class="muted">{{ t('music.tip') }}</span>
        <a-upload
          :show-upload-list="false"
          accept=".mp3"
          :before-upload="beforeUpload"
          :custom-request="customUpload"
        >
          <a-button type="primary" :loading="uploading">{{ t('music.upload') }}</a-button>
        </a-upload>
      </div>

      <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="fileName" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'size'">{{ formatSize(record.sizeBytes) }}</template>
          <template v-else-if="column.key === 'duration'">{{ formatDuration(record.durationSec) }}</template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm :title="t('music.deleteConfirm')" @confirm="remove(record.fileName)">
              <a-button type="link" size="small" danger>{{ t('music.delete') }}</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <a-empty v-if="!loading && rows.length === 0" :description="t('music.empty')" />

      <a-divider orientation="left">{{ t('music.recentPlays') }}</a-divider>
      <a-table
        :columns="playColumns"
        :data-source="plays"
        :loading="playsLoading"
        row-key="logId"
        size="small"
        :pagination="false"
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import { deleteMusic, listMusic, listMusicPlays, uploadMusic, type MusicFile, type MusicPlayLog } from '@/services/music'

const { t } = useI18n()
const rows = ref<MusicFile[]>([])
const plays = ref<MusicPlayLog[]>([])
const playsLoading = ref(false)
const playColumns = computed(() => [
  { title: t('music.playTime'), dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: t('music.device'), dataIndex: 'deviceId', key: 'deviceId', width: 180 },
  { title: t('music.keyword'), dataIndex: 'keyword', key: 'keyword' },
  { title: t('music.songName'), dataIndex: 'songName', key: 'songName' },
])
const loading = ref(false)
const uploading = ref(false)

const columns = computed(() => [
  { title: t('music.title'), dataIndex: 'title', key: 'title' },
  { title: t('music.artist'), dataIndex: 'artist', key: 'artist', width: 140 },
  { title: t('music.duration'), dataIndex: 'durationSec', key: 'duration', width: 100 },
  { title: t('music.size'), dataIndex: 'sizeBytes', key: 'size', width: 100 },
  { title: t('music.playCount'), dataIndex: 'playCount', key: 'playCount', width: 90 },
  { title: t('music.action'), key: 'action', width: 100 },
])

function formatDuration(seconds?: number) {
  if (!seconds) return '-'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m + ':' + String(s).padStart(2, '0')
}

function formatSize(size: number) {
  if (!size) return '-'
  return size > 1024 * 1024 ? (size / 1024 / 1024).toFixed(1) + ' MB' : (size / 1024).toFixed(0) + ' KB'
}

async function loadData() {
  loading.value = true
  playsLoading.value = true
  try {
    const response = await listMusic()
    rows.value = response.data || []
    const playResponse = await listMusicPlays(10)
    plays.value = playResponse.data || []
  } catch (error) {
    message.error(t('music.loadFailed'))
  } finally {
    loading.value = false
    playsLoading.value = false
  }
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  if (!file.name.toLowerCase().endsWith('.mp3')) {
    message.warning(t('music.onlyMp3'))
    return false
  }
  return true
}

const customUpload: UploadProps['customRequest'] = async (options) => {
  uploading.value = true
  try {
    await uploadMusic(options.file as File)
    message.success(t('music.uploaded'))
    await loadData()
    options.onSuccess?.({})
  } catch (error) {
    message.error(t('music.uploadFailed'))
    options.onError?.(error as Error)
  } finally {
    uploading.value = false
  }
}

async function remove(name: string) {
  try {
    await deleteMusic(name)
    message.success(t('music.deleted'))
    await loadData()
  } catch (error) {
    message.error(t('music.deleteFailed'))
  }
}

onMounted(loadData)
</script>

<style scoped>
.music-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.music-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

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

      <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="name" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'size'">{{ formatSize(record.size) }}</template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm :title="t('music.deleteConfirm')" @confirm="remove(record.name)">
              <a-button type="link" size="small" danger>{{ t('music.delete') }}</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <a-empty v-if="!loading && rows.length === 0" :description="t('music.empty')" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import { deleteMusic, listMusic, uploadMusic, type MusicFile } from '@/services/music'

const { t } = useI18n()
const rows = ref<MusicFile[]>([])
const loading = ref(false)
const uploading = ref(false)

const columns = computed(() => [
  { title: t('music.name'), dataIndex: 'name', key: 'name' },
  { title: t('music.size'), dataIndex: 'size', key: 'size', width: 120 },
  { title: t('music.updateTime'), dataIndex: 'updateTime', key: 'updateTime', width: 200 },
  { title: t('music.action'), key: 'action', width: 100 },
])

function formatSize(size: number) {
  if (!size) return '-'
  return size > 1024 * 1024 ? (size / 1024 / 1024).toFixed(1) + ' MB' : (size / 1024).toFixed(0) + ' KB'
}

async function loadData() {
  loading.value = true
  try {
    const response = await listMusic()
    rows.value = response.data || []
  } catch (error) {
    message.error(t('music.loadFailed'))
  } finally {
    loading.value = false
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

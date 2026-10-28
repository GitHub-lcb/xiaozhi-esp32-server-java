<template>
  <div class="voice-clone-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <span class="muted">{{ t('voiceClone.tip') }}</span>
        <a-button type="primary" @click="openModal">{{ t('voiceClone.create') }}</a-button>
      </div>

      <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="cloneId" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'state'">
            <a-tag :color="record.state === '1' ? 'green' : record.state === '2' ? 'red' : 'blue'">
              {{ stateLabel(record.state) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'voiceId'">
            <span v-if="record.voiceId">{{ record.voiceId }}</span>
            <span v-else class="muted">{{ record.errorMsg || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm :title="t('voiceClone.deleteConfirm')" @confirm="remove(record.cloneId)">
              <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalVisible" :title="t('voiceClone.create')" :confirm-loading="submitting" @ok="submit">
      <a-form layout="vertical">
        <a-form-item :label="t('voiceClone.voiceName')" required>
          <a-input v-model:value="form.voiceName" :maxlength="50" :placeholder="t('voiceClone.voiceNameTip')" />
        </a-form-item>
        <a-form-item :label="t('voiceClone.sample')" required>
          <a-upload :show-upload-list="true" :max-count="1" :before-upload="capture">
            <a-button>{{ t('voiceClone.chooseFile') }}</a-button>
          </a-upload>
          <div class="muted">{{ t('voiceClone.sampleTip') }}</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { createVoiceClone, deleteVoiceClone, queryVoiceClones } from '@/services/voiceClone'
import type { VoiceClone } from '@/types/voiceClone'

const { t } = useI18n()
const rows = ref<VoiceClone[]>([])
const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const sample = ref<File | null>(null)
const form = reactive({ voiceName: '' })

const columns = computed(() => [
  { title: t('voiceClone.voiceName'), dataIndex: 'voiceName', key: 'voiceName', width: 180 },
  { title: t('voiceClone.provider'), dataIndex: 'provider', key: 'provider', width: 120 },
  { title: t('voiceClone.voiceId'), key: 'voiceId', ellipsis: true },
  { title: t('voiceClone.stateLabel'), key: 'state', width: 110 },
  { title: t('voiceClone.createTime'), dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: t('voiceClone.action'), key: 'action', width: 90 },
])

function stateLabel(state?: string) {
  if (state === '1') return t('voiceClone.state.ready')
  if (state === '2') return t('voiceClone.state.failed')
  return t('voiceClone.state.processing')
}

async function loadData() {
  loading.value = true
  try {
    const response = await queryVoiceClones()
    rows.value = response.data || []
  } catch (error) {
    message.error(t('voiceClone.loadFailed'))
  } finally {
    loading.value = false
  }
}

function openModal() {
  form.voiceName = ''
  sample.value = null
  modalVisible.value = true
}

function capture(file: File) {
  sample.value = file
  return false
}

async function submit() {
  if (!form.voiceName || !sample.value) {
    message.warning(t('voiceClone.requiredTip'))
    return
  }
  submitting.value = true
  try {
    await createVoiceClone(form.voiceName, sample.value)
    message.success(t('voiceClone.createSuccess'))
    modalVisible.value = false
    await loadData()
  } catch (error) {
    message.error(t('voiceClone.createFailed'))
  } finally {
    submitting.value = false
  }
}

async function remove(cloneId?: number) {
  if (!cloneId) return
  try {
    await deleteVoiceClone(cloneId)
    message.success(t('voiceClone.deleteSuccess'))
    await loadData()
  } catch (error) {
    message.error(t('voiceClone.deleteFailed'))
  }
}

onMounted(loadData)
</script>

<style scoped>
.voice-clone-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.voice-clone-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

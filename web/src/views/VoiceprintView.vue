<template>
  <div class="voiceprint-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <a-space>
          <a-button type="primary" @click="openRegister">{{ t('voiceprint.register') }}</a-button>
          <a-upload :show-upload-list="false" :before-upload="handleMatch">
            <a-button :loading="matching">{{ t('voiceprint.matchTest') }}</a-button>
          </a-upload>
          <span class="muted">{{ t('voiceprint.tip') }}</span>
        </a-space>
        <a-tag v-if="matchResult" :color="matchResult.matched ? 'green' : 'orange'">
          {{ matchText }}
        </a-tag>
      </div>

      <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="voiceprintId" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'state'">
            <a-tag :color="record.state === '1' ? 'green' : 'default'">
              {{ record.state === '1' ? t('voiceprint.state.enabled') : t('voiceprint.state.disabled') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm :title="t('voiceprint.deleteConfirm')" @confirm="remove(record.voiceprintId)">
              <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="registerVisible" :title="t('voiceprint.register')" :confirm-loading="registering" @ok="submitRegister">
      <a-form layout="vertical">
        <a-form-item :label="t('voiceprint.memberName')" required>
          <a-input v-model:value="memberName" :maxlength="50" :placeholder="t('voiceprint.memberNameTip')" />
        </a-form-item>
        <a-form-item :label="t('voiceprint.sample')" required>
          <a-upload :show-upload-list="true" :max-count="1" :before-upload="captureSample">
            <a-button>{{ t('voiceprint.chooseFile') }}</a-button>
          </a-upload>
          <div class="muted">{{ t('voiceprint.sampleTip') }}</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { deleteVoiceprint, matchVoiceprint, queryVoiceprints, registerVoiceprint } from '@/services/voiceprint'
import type { Voiceprint, VoiceprintMatch } from '@/types/voiceprint'

const { t } = useI18n()

const rows = ref<Voiceprint[]>([])
const loading = ref(false)
const matching = ref(false)
const registering = ref(false)
const registerVisible = ref(false)
const memberName = ref('')
const sample = ref<File | null>(null)
const matchResult = ref<VoiceprintMatch | null>(null)

const columns = computed(() => [
  { title: t('voiceprint.memberName'), dataIndex: 'memberName', key: 'memberName', width: 180 },
  { title: t('voiceprint.sampleSeconds'), dataIndex: 'sampleSeconds', key: 'sampleSeconds', width: 120 },
  { title: t('voiceprint.stateLabel'), key: 'state', width: 100 },
  { title: t('voiceprint.createTime'), dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: t('voiceprint.actionLabel'), key: 'action', width: 100 },
])

const matchText = computed(() => {
  const result = matchResult.value
  if (!result) return ''
  if (!result.matched) return t('voiceprint.unknown', { score: result.score ?? 0 })
  return t('voiceprint.matched', { name: result.memberName, score: result.score ?? 0 })
})

async function loadData() {
  loading.value = true
  try {
    const response = await queryVoiceprints()
    rows.value = response.data || []
  } catch (error) {
    message.error(t('voiceprint.loadFailed'))
  } finally {
    loading.value = false
  }
}

function openRegister() {
  memberName.value = ''
  sample.value = null
  registerVisible.value = true
}

function captureSample(file: File) {
  sample.value = file
  return false
}

async function submitRegister() {
  if (!memberName.value || !sample.value) {
    message.warning(t('voiceprint.requiredTip'))
    return
  }
  registering.value = true
  try {
    await registerVoiceprint(memberName.value, sample.value)
    message.success(t('voiceprint.registerSuccess'))
    registerVisible.value = false
    await loadData()
  } catch (error) {
    message.error(t('voiceprint.registerFailed'))
  } finally {
    registering.value = false
  }
}

async function handleMatch(file: File) {
  matching.value = true
  try {
    const response = await matchVoiceprint(file)
    matchResult.value = response.data
  } catch (error) {
    message.error(t('voiceprint.matchFailed'))
  } finally {
    matching.value = false
  }
  return false
}

async function remove(voiceprintId: number) {
  try {
    await deleteVoiceprint(voiceprintId)
    message.success(t('voiceprint.deleteSuccess'))
    await loadData()
  } catch (error) {
    message.error(t('voiceprint.deleteFailed'))
  }
}

onMounted(loadData)
</script>

<style scoped>
.voiceprint-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.voiceprint-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

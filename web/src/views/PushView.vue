<template>
  <div class="push-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <a-space>
          <a-input
            v-model:value="query.title"
            :placeholder="t('push.titlePlaceholder')"
            allow-clear
            style="width: 220px"
            @press-enter="loadData"
          />
          <a-select v-model:value="query.state" allow-clear :placeholder="t('push.statePlaceholder')" style="width: 150px">
            <a-select-option value="0">{{ t('push.state.pending') }}</a-select-option>
            <a-select-option value="1">{{ t('push.state.sent') }}</a-select-option>
          </a-select>
          <a-button type="primary" @click="loadData">{{ t('common.search') }}</a-button>
        </a-space>
        <a-button type="primary" @click="openModal">{{ t('push.create') }}</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="pushId"
        size="middle"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'state'">
            <a-tag :color="record.state === '1' ? 'green' : 'blue'">{{ stateLabel(record.state) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'target'">
            <a-tag>{{ record.targetType === 'user' ? t('push.targetType.user') : t('push.targetType.device') }}</a-tag>
            <span class="muted">{{ record.targetValue }}</span>
          </template>
          <template v-else-if="column.key === 'result'">
            <span v-if="record.state === '1'">
              {{ t('push.sentCount') }} {{ record.sentCount ?? 0 }} /
              {{ t('push.offlineCount') }} {{ record.offlineCount ?? 0 }}
            </span>
            <span v-else class="muted">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-popconfirm
                v-if="record.state === '0'"
                :title="t('push.sendConfirm')"
                @confirm="send(record.pushId)"
              >
                <a-button type="link" size="small">{{ t('push.send') }}</a-button>
              </a-popconfirm>
              <a-popconfirm
                v-if="record.state === '0'"
                :title="t('push.deleteConfirm')"
                @confirm="remove(record.pushId)"
              >
                <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalVisible"
      :title="t('push.create')"
      :confirm-loading="submitting"
      @ok="submit"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item :label="t('push.title')" required>
          <a-input v-model:value="form.title" :maxlength="100" show-count />
        </a-form-item>
        <a-form-item :label="t('push.content')" required>
          <a-textarea v-model:value="form.content" :rows="4" :maxlength="1000" show-count />
        </a-form-item>
        <a-form-item :label="t('push.targetTypeLabel')">
          <a-radio-group v-model:value="form.targetType">
            <a-radio value="device">{{ t('push.targetType.device') }}</a-radio>
            <a-radio value="user">{{ t('push.targetType.user') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="form.targetType === 'user' ? t('push.userIds') : t('push.deviceIds')" required>
          <a-textarea v-model:value="form.targetValue" :rows="2" :placeholder="t('push.targetTip')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { addPushMessage, deletePushMessage, queryPushMessages, sendPushMessage } from '@/services/push'
import type { PushMessage } from '@/types/push'

const { t } = useI18n()

const rows = ref<PushMessage[]>([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)

const query = reactive({ pageNo: 1, pageSize: 10, title: '', state: undefined as string | undefined })

const form = reactive<Partial<PushMessage>>({
  title: '',
  content: '',
  targetType: 'device',
  targetValue: '',
})

const columns = computed(() => [
  { title: t('push.title'), dataIndex: 'title', key: 'title', width: 180 },
  { title: t('push.content'), dataIndex: 'content', key: 'content', ellipsis: true },
  { title: t('push.target'), key: 'target', width: 260 },
  { title: t('push.stateLabel'), key: 'state', width: 100 },
  { title: t('push.result'), key: 'result', width: 180 },
  { title: t('push.createTime'), dataIndex: 'sentTime', key: 'sentTime', width: 170 },
  { title: t('push.actionLabel'), key: 'action', width: 160 },
])

const pagination = computed<TablePaginationConfig>(() => ({
  current: query.pageNo,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: true,
}))

function stateLabel(state?: string) {
  return state === '1' ? t('push.state.sent') : t('push.state.pending')
}

async function loadData() {
  loading.value = true
  try {
    const response = await queryPushMessages({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      title: query.title || undefined,
      state: query.state,
    })
    rows.value = response.data?.list || []
    total.value = response.data?.total || 0
  } catch (error) {
    message.error(t('push.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onTableChange(p: TablePaginationConfig) {
  query.pageNo = p.current || 1
  query.pageSize = p.pageSize || 10
  loadData()
}

function openModal() {
  form.title = ''
  form.content = ''
  form.targetType = 'device'
  form.targetValue = ''
  modalVisible.value = true
}

async function submit() {
  if (!form.title || !form.content || !form.targetValue) {
    message.warning(t('push.requiredTip'))
    return
  }
  submitting.value = true
  try {
    await addPushMessage({ ...form })
    message.success(t('push.saveSuccess'))
    modalVisible.value = false
    await loadData()
  } catch (error) {
    message.error(t('push.saveFailed'))
  } finally {
    submitting.value = false
  }
}

async function send(pushId?: number) {
  if (!pushId) return
  try {
    await sendPushMessage(pushId)
    message.success(t('push.sendSuccess'))
    await loadData()
  } catch (error) {
    message.error(t('push.sendFailed'))
  }
}

async function remove(pushId?: number) {
  if (!pushId) return
  try {
    await deletePushMessage(pushId)
    message.success(t('push.deleteSuccess'))
    await loadData()
  } catch (error) {
    message.error(t('push.deleteFailed'))
  }
}

onMounted(loadData)
</script>

<style scoped>
.push-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.push-view .muted {
  margin-left: 8px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>

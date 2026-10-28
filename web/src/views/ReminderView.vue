<template>
  <div class="reminder-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <a-space>
          <a-input
            v-model:value="query.deviceId"
            :placeholder="t('reminder.deviceIdPlaceholder')"
            allow-clear
            style="width: 220px"
            @press-enter="loadData"
          />
          <a-select v-model:value="query.state" allow-clear :placeholder="t('reminder.statePlaceholder')" style="width: 150px">
            <a-select-option value="0">{{ t('reminder.state.pending') }}</a-select-option>
            <a-select-option value="1">{{ t('reminder.state.fired') }}</a-select-option>
            <a-select-option value="2">{{ t('reminder.state.canceled') }}</a-select-option>
            <a-select-option value="3">{{ t('reminder.state.missed') }}</a-select-option>
          </a-select>
          <a-button type="primary" @click="loadData">{{ t('common.search') }}</a-button>
        </a-space>
        <a-button type="primary" @click="openModal">{{ t('reminder.create') }}</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="reminderId"
        size="middle"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'repeatType'">
            <a-tag>{{ repeatLabel(record.repeatType) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'state'">
            <a-tag :color="stateColor(record.state)">{{ stateLabel(record.state) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-popconfirm
                v-if="record.state === '0'"
                :title="t('reminder.cancelConfirm')"
                @confirm="cancel(record.reminderId)"
              >
                <a-button type="link" size="small">{{ t('reminder.cancel') }}</a-button>
              </a-popconfirm>
              <a-popconfirm :title="t('reminder.deleteConfirm')" @confirm="remove(record.reminderId)">
                <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalVisible"
      :title="t('reminder.create')"
      :confirm-loading="submitting"
      @ok="submit"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item :label="t('reminder.deviceId')" required>
          <a-input v-model:value="form.deviceId" :placeholder="t('reminder.deviceIdTip')" />
        </a-form-item>
        <a-form-item :label="t('reminder.title')" required>
          <a-input v-model:value="form.title" :maxlength="100" show-count />
        </a-form-item>
        <a-form-item :label="t('reminder.content')" required>
          <a-textarea v-model:value="form.content" :rows="3" :maxlength="500" show-count />
        </a-form-item>
        <a-form-item :label="t('reminder.remindTime')" required>
          <a-date-picker
            v-model:value="remindTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item :label="t('reminder.repeatType')">
          <a-radio-group v-model:value="form.repeatType">
            <a-radio value="once">{{ t('reminder.repeat.once') }}</a-radio>
            <a-radio value="daily">{{ t('reminder.repeat.daily') }}</a-radio>
            <a-radio value="weekday">{{ t('reminder.repeat.weekday') }}</a-radio>
          </a-radio-group>
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
import { addReminder, cancelReminder, deleteReminder, queryReminders } from '@/services/reminder'
import type { Reminder } from '@/types/reminder'

const { t } = useI18n()

const rows = ref<Reminder[]>([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const remindTime = ref<string | null>(null)

const query = reactive({ pageNo: 1, pageSize: 10, deviceId: '', state: undefined as string | undefined })
const form = reactive<Partial<Reminder>>({ deviceId: '', title: '', content: '', repeatType: 'once' })

const columns = computed(() => [
  { title: t('reminder.deviceId'), dataIndex: 'deviceId', key: 'deviceId', width: 180 },
  { title: t('reminder.title'), dataIndex: 'title', key: 'title', width: 160 },
  { title: t('reminder.content'), dataIndex: 'content', key: 'content', ellipsis: true },
  { title: t('reminder.remindTime'), dataIndex: 'remindTime', key: 'remindTime', width: 170 },
  { title: t('reminder.repeatType'), key: 'repeatType', width: 110 },
  { title: t('reminder.stateLabel'), key: 'state', width: 100 },
  { title: t('reminder.actionLabel'), key: 'action', width: 150 },
])

const pagination = computed<TablePaginationConfig>(() => ({
  current: query.pageNo,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: true,
}))

function repeatLabel(type?: string) {
  if (type === 'daily') return t('reminder.repeat.daily')
  if (type === 'weekday') return t('reminder.repeat.weekday')
  return t('reminder.repeat.once')
}

function stateLabel(state?: string) {
  switch (state) {
    case '1':
      return t('reminder.state.fired')
    case '2':
      return t('reminder.state.canceled')
    case '3':
      return t('reminder.state.missed')
    default:
      return t('reminder.state.pending')
  }
}

function stateColor(state?: string) {
  switch (state) {
    case '1':
      return 'green'
    case '2':
      return 'default'
    case '3':
      return 'red'
    default:
      return 'blue'
  }
}

async function loadData() {
  loading.value = true
  try {
    const response = await queryReminders({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      deviceId: query.deviceId || undefined,
      state: query.state,
    })
    rows.value = response.data?.list || []
    total.value = response.data?.total || 0
  } catch (error) {
    message.error(t('reminder.loadFailed'))
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
  form.deviceId = ''
  form.title = ''
  form.content = ''
  form.repeatType = 'once'
  remindTime.value = null
  modalVisible.value = true
}

async function submit() {
  if (!form.deviceId || !form.title || !form.content || !remindTime.value) {
    message.warning(t('reminder.requiredTip'))
    return
  }
  submitting.value = true
  try {
    await addReminder({ ...form, remindTime: remindTime.value })
    message.success(t('reminder.saveSuccess'))
    modalVisible.value = false
    await loadData()
  } catch (error) {
    message.error(t('reminder.saveFailed'))
  } finally {
    submitting.value = false
  }
}

async function cancel(reminderId?: number) {
  if (!reminderId) return
  try {
    await cancelReminder(reminderId)
    message.success(t('reminder.saveSuccess'))
    await loadData()
  } catch (error) {
    message.error(t('reminder.saveFailed'))
  }
}

async function remove(reminderId?: number) {
  if (!reminderId) return
  try {
    await deleteReminder(reminderId)
    message.success(t('reminder.deleteSuccess'))
    await loadData()
  } catch (error) {
    message.error(t('reminder.deleteFailed'))
  }
}

onMounted(loadData)
</script>

<style scoped>
.reminder-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
</style>

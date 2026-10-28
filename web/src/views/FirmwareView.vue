<template>
  <div class="firmware-view">
    <a-card :bordered="false">
      <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
        <!-- 固件版本 -->
        <a-tab-pane key="firmware" :tab="t('firmware.tab.firmware')">
          <div class="toolbar">
            <a-space>
              <a-input
                v-model:value="firmwareQuery.version"
                :placeholder="t('firmware.versionPlaceholder')"
                allow-clear
                style="width: 200px"
                @press-enter="loadFirmwares"
              />
              <a-select
                v-model:value="firmwareQuery.state"
                :placeholder="t('firmware.statePlaceholder')"
                allow-clear
                style="width: 140px"
              >
                <a-select-option value="1">{{ t('firmware.state.enabled') }}</a-select-option>
                <a-select-option value="0">{{ t('firmware.state.disabled') }}</a-select-option>
              </a-select>
              <a-button type="primary" @click="loadFirmwares">{{ t('common.search') }}</a-button>
            </a-space>
            <a-button type="primary" @click="openFirmwareModal()">
              {{ t('firmware.addFirmware') }}
            </a-button>
          </div>
          <a-table
            :columns="firmwareColumns"
            :data-source="firmwares"
            :loading="loading"
            :pagination="firmwarePagination"
            row-key="firmwareId"
            size="middle"
            @change="onFirmwareTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'version'">
                <a-tag color="blue">{{ record.version }}</a-tag>
              </template>
              <template v-else-if="column.key === 'fileSize'">
                {{ formatSize(record.fileSize) }}
              </template>
              <template v-else-if="column.key === 'state'">
                <a-tag :color="record.state === '1' ? 'green' : 'default'">
                  {{ record.state === '1' ? t('firmware.state.enabled') : t('firmware.state.disabled') }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="openFirmwareModal(record)">
                    {{ t('common.edit') }}
                  </a-button>
                  <a-popconfirm :title="t('firmware.deleteConfirm')" @confirm="removeFirmware(record.firmwareId)">
                    <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 升级任务 -->
        <a-tab-pane key="release" :tab="t('firmware.tab.release')">
          <div class="toolbar">
            <a-input
              v-model:value="releaseQuery.releaseName"
              :placeholder="t('firmware.releaseNamePlaceholder')"
              allow-clear
              style="width: 220px"
              @press-enter="loadReleases"
            />
            <a-button type="primary" @click="loadReleases">{{ t('common.search') }}</a-button>
            <a-button type="primary" @click="openReleaseModal()">{{ t('firmware.addRelease') }}</a-button>
          </div>
          <a-table
            :columns="releaseColumns"
            :data-source="releases"
            :loading="loading"
            :pagination="releasePagination"
            row-key="releaseId"
            size="middle"
            @change="onReleaseTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'strategy'">
                <a-tag>{{ strategyLabel(record) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'window'">
                <span class="muted">{{ windowLabel(record) }}</span>
              </template>
              <template v-else-if="column.key === 'state'">
                <a-tag :color="stateColor(record.state)">{{ stateLabel(record.state) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button
                    v-if="record.state !== '1' && record.state !== '3'"
                    type="link"
                    size="small"
                    @click="changeState(record, '1')"
                  >
                    {{ t('firmware.action.publish') }}
                  </a-button>
                  <a-button
                    v-if="record.state === '1'"
                    type="link"
                    size="small"
                    @click="changeState(record, '2')"
                  >
                    {{ t('firmware.action.pause') }}
                  </a-button>
                  <a-button
                    v-if="record.state !== '3'"
                    type="link"
                    size="small"
                    @click="changeState(record, '3')"
                  >
                    {{ t('firmware.action.finish') }}
                  </a-button>
                  <a-button
                    v-if="record.state !== '3'"
                    type="link"
                    size="small"
                    @click="openReleaseModal(record)"
                  >
                    {{ t('common.edit') }}
                  </a-button>
                  <a-popconfirm :title="t('firmware.deleteConfirm')" @confirm="removeRelease(record.releaseId)">
                    <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 升级记录 -->
        <a-tab-pane key="log" :tab="t('firmware.tab.log')">
          <div class="toolbar">
            <a-input
              v-model:value="logQuery.deviceId"
              :placeholder="t('firmware.deviceIdPlaceholder')"
              allow-clear
              style="width: 220px"
              @press-enter="loadLogs"
            />
            <a-select
              v-model:value="logQuery.status"
              :placeholder="t('firmware.statusPlaceholder')"
              allow-clear
              style="width: 160px"
            >
              <a-select-option value="0">{{ t('firmware.logStatus.delivered') }}</a-select-option>
              <a-select-option value="1">{{ t('firmware.logStatus.success') }}</a-select-option>
              <a-select-option value="2">{{ t('firmware.logStatus.failed') }}</a-select-option>
            </a-select>
            <a-button type="primary" @click="loadLogs">{{ t('common.search') }}</a-button>
          </div>
          <a-table
            :columns="logColumns"
            :data-source="logs"
            :loading="loading"
            :pagination="logPagination"
            row-key="logId"
            size="middle"
            @change="onLogTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'versionChange'">
                <span>{{ record.fromVersion || '-' }}</span>
                <span class="arrow">→</span>
                <a-tag color="blue">{{ record.toVersion || '-' }}</a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="logStatusColor(record.status)">{{ logStatusLabel(record.status) }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 固件表单 -->
    <a-modal
      v-model:open="firmwareModalVisible"
      :title="firmwareForm.firmwareId ? t('firmware.editFirmware') : t('firmware.addFirmware')"
      :confirm-loading="submitting"
      @ok="submitFirmware"
    >
      <a-form :model="firmwareForm" layout="vertical">
        <a-form-item :label="t('firmware.version')" required>
          <a-input v-model:value="firmwareForm.version" placeholder="1.2.3" />
        </a-form-item>
        <a-form-item :label="t('firmware.chipModel')">
          <a-input v-model:value="firmwareForm.chipModel" :placeholder="t('firmware.chipModelTip')" />
        </a-form-item>
        <a-form-item :label="t('firmware.fileUrl')" required>
          <a-input v-model:value="firmwareForm.fileUrl" placeholder="https://.../firmware.bin" />
        </a-form-item>
        <a-form-item :label="t('firmware.md5')">
          <a-input v-model:value="firmwareForm.md5" />
        </a-form-item>
        <a-form-item :label="t('firmware.releaseNotes')">
          <a-textarea v-model:value="firmwareForm.releaseNotes" :rows="3" />
        </a-form-item>
        <a-form-item :label="t('firmware.stateLabel')">
          <a-switch
            :checked="firmwareForm.state === '1'"
            @change="(checked: boolean) => (firmwareForm.state = checked ? '1' : '0')"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 升级任务表单 -->
    <a-modal
      v-model:open="releaseModalVisible"
      :title="releaseForm.releaseId ? t('firmware.editRelease') : t('firmware.addRelease')"
      :confirm-loading="submitting"
      @ok="submitRelease"
    >
      <a-form :model="releaseForm" layout="vertical">
        <a-form-item :label="t('firmware.releaseName')" required>
          <a-input v-model:value="releaseForm.releaseName" />
        </a-form-item>
        <a-form-item :label="t('firmware.targetFirmware')" required>
          <a-select v-model:value="releaseForm.firmwareId" :placeholder="t('firmware.targetFirmwareTip')">
            <a-select-option v-for="item in firmwareOptions" :key="item.firmwareId" :value="item.firmwareId">
              {{ item.version }}<span v-if="item.chipModel"> · {{ item.chipModel }}</span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="t('firmware.strategyLabel')">
          <a-radio-group v-model:value="releaseForm.strategy">
            <a-radio value="all">{{ t('firmware.strategy.all') }}</a-radio>
            <a-radio value="percent">{{ t('firmware.strategy.percent') }}</a-radio>
            <a-radio value="whitelist">{{ t('firmware.strategy.whitelist') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="releaseForm.strategy === 'percent'" :label="t('firmware.percent')">
          <a-input-number v-model:value="releaseForm.percent" :min="1" :max="100" style="width: 100%" />
        </a-form-item>
        <a-form-item v-if="releaseForm.strategy === 'whitelist'" :label="t('firmware.whitelist')">
          <a-textarea
            v-model:value="releaseForm.whitelist"
            :rows="3"
            :placeholder="t('firmware.whitelistTip')"
          />
        </a-form-item>
        <a-form-item :label="t('firmware.timeWindow')">
          <a-range-picker
            v-model:value="timeWindow"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
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
import {
  addFirmware,
  addRelease,
  changeReleaseState,
  deleteFirmware,
  deleteRelease,
  queryFirmwares,
  queryReleases,
  queryUpgradeLogs,
  updateFirmware,
  updateRelease,
} from '@/services/firmware'
import type { Firmware, FirmwareRelease, FirmwareUpgradeLog } from '@/types/firmware'

const { t } = useI18n()

const activeTab = ref('firmware')
const loading = ref(false)
const submitting = ref(false)

const firmwares = ref<Firmware[]>([])
const releases = ref<FirmwareRelease[]>([])
const logs = ref<FirmwareUpgradeLog[]>([])
const firmwareOptions = ref<Firmware[]>([])

const firmwareQuery = reactive({ pageNo: 1, pageSize: 10, version: '', state: undefined as string | undefined })
const releaseQuery = reactive({ pageNo: 1, pageSize: 10, releaseName: '' })
const logQuery = reactive({ pageNo: 1, pageSize: 10, deviceId: '', status: undefined as string | undefined })

const total = reactive({ firmware: 0, release: 0, log: 0 })

const firmwareModalVisible = ref(false)
const releaseModalVisible = ref(false)
const firmwareForm = reactive<Partial<Firmware>>({ state: '1' })
const releaseForm = reactive<Partial<FirmwareRelease>>({ strategy: 'all' })
const timeWindow = ref<[string, string] | null>(null)

const firmwareColumns = computed(() => [
  { title: t('firmware.version'), dataIndex: 'version', key: 'version', width: 130 },
  { title: t('firmware.chipModel'), dataIndex: 'chipModel', key: 'chipModel', width: 140 },
  { title: t('firmware.fileUrl'), dataIndex: 'fileUrl', key: 'fileUrl', ellipsis: true },
  { title: t('firmware.fileSize'), dataIndex: 'fileSize', key: 'fileSize', width: 110 },
  { title: t('firmware.stateLabel'), dataIndex: 'state', key: 'state', width: 100 },
  { title: t('firmware.createTime'), dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: t('firmware.actionLabel'), key: 'action', width: 140 },
])

const releaseColumns = computed(() => [
  { title: t('firmware.releaseName'), dataIndex: 'releaseName', key: 'releaseName', width: 180 },
  { title: t('firmware.targetVersion'), dataIndex: 'firmwareVersion', key: 'firmwareVersion', width: 120 },
  { title: t('firmware.strategyLabel'), key: 'strategy', width: 160 },
  { title: t('firmware.timeWindow'), key: 'window', width: 260 },
  { title: t('firmware.stateLabel'), key: 'state', width: 100 },
  { title: t('firmware.actionLabel'), key: 'action', width: 260 },
])

const logColumns = computed(() => [
  { title: t('firmware.deviceId'), dataIndex: 'deviceId', key: 'deviceId', width: 180 },
  { title: t('firmware.versionChange'), key: 'versionChange', width: 220 },
  { title: t('firmware.releaseName'), dataIndex: 'releaseId', key: 'releaseId', width: 120 },
  { title: t('firmware.statusLabel'), key: 'status', width: 110 },
  { title: t('firmware.ip'), dataIndex: 'ip', key: 'ip', width: 140 },
  { title: t('firmware.createTime'), dataIndex: 'createTime', key: 'createTime', width: 170 },
])

const firmwarePagination = computed<TablePaginationConfig>(() => ({
  current: firmwareQuery.pageNo,
  pageSize: firmwareQuery.pageSize,
  total: total.firmware,
  showSizeChanger: true,
}))
const releasePagination = computed<TablePaginationConfig>(() => ({
  current: releaseQuery.pageNo,
  pageSize: releaseQuery.pageSize,
  total: total.release,
  showSizeChanger: true,
}))
const logPagination = computed<TablePaginationConfig>(() => ({
  current: logQuery.pageNo,
  pageSize: logQuery.pageSize,
  total: total.log,
  showSizeChanger: true,
}))

function formatSize(size?: number) {
  if (!size || size <= 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

function stateLabel(state?: string) {
  switch (state) {
    case '1':
      return t('firmware.releaseState.active')
    case '2':
      return t('firmware.releaseState.paused')
    case '3':
      return t('firmware.releaseState.finished')
    default:
      return t('firmware.releaseState.pending')
  }
}

function stateColor(state?: string) {
  switch (state) {
    case '1':
      return 'green'
    case '2':
      return 'orange'
    case '3':
      return 'default'
    default:
      return 'blue'
  }
}

function strategyLabel(record: FirmwareRelease) {
  if (record.strategy === 'percent') return `${t('firmware.strategy.percent')} ${record.percent ?? 0}%`
  if (record.strategy === 'whitelist') return t('firmware.strategy.whitelist')
  return t('firmware.strategy.all')
}

function windowLabel(record: FirmwareRelease) {
  if (!record.startTime && !record.endTime) return t('firmware.window.always')
  return `${record.startTime || '-'} ~ ${record.endTime || '-'}`
}

function logStatusLabel(status: string) {
  if (status === '1') return t('firmware.logStatus.success')
  if (status === '2') return t('firmware.logStatus.failed')
  return t('firmware.logStatus.delivered')
}

function logStatusColor(status: string) {
  if (status === '1') return 'green'
  if (status === '2') return 'red'
  return 'blue'
}

async function loadFirmwares() {
  loading.value = true
  try {
    const page = await queryFirmwares({
      pageNo: firmwareQuery.pageNo,
      pageSize: firmwareQuery.pageSize,
      version: firmwareQuery.version || undefined,
      state: firmwareQuery.state,
    })
    firmwares.value = page.data?.list || []
    total.firmware = page.data?.total || 0
  } catch (error) {
    message.error(t('firmware.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function loadFirmwareOptions() {
  const page = await queryFirmwares({ pageNo: 1, pageSize: 200, state: '1' })
  firmwareOptions.value = page.data?.list || []
}

async function loadReleases() {
  loading.value = true
  try {
    const page = await queryReleases({
      pageNo: releaseQuery.pageNo,
      pageSize: releaseQuery.pageSize,
      releaseName: releaseQuery.releaseName || undefined,
    })
    releases.value = page.data?.list || []
    total.release = page.data?.total || 0
  } catch (error) {
    message.error(t('firmware.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  loading.value = true
  try {
    const page = await queryUpgradeLogs({
      pageNo: logQuery.pageNo,
      pageSize: logQuery.pageSize,
      deviceId: logQuery.deviceId || undefined,
      status: logQuery.status,
    })
    logs.value = page.data?.list || []
    total.log = page.data?.total || 0
  } catch (error) {
    message.error(t('firmware.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onTabChange(key: string) {
  if (key === 'firmware') loadFirmwares()
  else if (key === 'release') {
    loadFirmwareOptions()
    loadReleases()
  } else loadLogs()
}

function onFirmwareTableChange(pagination: TablePaginationConfig) {
  firmwareQuery.pageNo = pagination.current || 1
  firmwareQuery.pageSize = pagination.pageSize || 10
  loadFirmwares()
}

function onReleaseTableChange(pagination: TablePaginationConfig) {
  releaseQuery.pageNo = pagination.current || 1
  releaseQuery.pageSize = pagination.pageSize || 10
  loadReleases()
}

function onLogTableChange(pagination: TablePaginationConfig) {
  logQuery.pageNo = pagination.current || 1
  logQuery.pageSize = pagination.pageSize || 10
  loadLogs()
}

function openFirmwareModal(record?: Firmware) {
  Object.keys(firmwareForm).forEach((key) => delete (firmwareForm as Record<string, unknown>)[key])
  Object.assign(firmwareForm, record ?? { state: '1' })
  firmwareModalVisible.value = true
}

async function submitFirmware() {
  if (!firmwareForm.version || !firmwareForm.fileUrl) {
    message.warning(t('firmware.requiredTip'))
    return
  }
  submitting.value = true
  try {
    if (firmwareForm.firmwareId) {
      await updateFirmware({ ...firmwareForm })
    } else {
      await addFirmware({ ...firmwareForm })
    }
    message.success(t('firmware.saveSuccess'))
    firmwareModalVisible.value = false
    await loadFirmwares()
    await loadFirmwareOptions()
  } catch (error) {
    message.error(t('firmware.saveFailed'))
  } finally {
    submitting.value = false
  }
}

async function removeFirmware(firmwareId?: number) {
  if (!firmwareId) return
  try {
    await deleteFirmware(firmwareId)
    message.success(t('firmware.deleteSuccess'))
    await loadFirmwares()
  } catch (error) {
    message.error(t('firmware.deleteFailed'))
  }
}

async function openReleaseModal(record?: FirmwareRelease) {
  await loadFirmwareOptions()
  Object.keys(releaseForm).forEach((key) => delete (releaseForm as Record<string, unknown>)[key])
  Object.assign(releaseForm, record ?? { strategy: 'all' })
  timeWindow.value = record?.startTime || record?.endTime
    ? [record.startTime || '', record.endTime || '']
    : null
  releaseModalVisible.value = true
}

async function submitRelease() {
  if (!releaseForm.releaseName || !releaseForm.firmwareId) {
    message.warning(t('firmware.requiredTip'))
    return
  }
  const payload: Partial<FirmwareRelease> = {
    ...releaseForm,
    startTime: timeWindow.value?.[0] || null,
    endTime: timeWindow.value?.[1] || null,
  }
  submitting.value = true
  try {
    if (releaseForm.releaseId) {
      await updateRelease(payload)
    } else {
      await addRelease(payload)
    }
    message.success(t('firmware.saveSuccess'))
    releaseModalVisible.value = false
    await loadReleases()
  } catch (error) {
    message.error(t('firmware.saveFailed'))
  } finally {
    submitting.value = false
  }
}

async function changeState(record: FirmwareRelease, state: string) {
  if (!record.releaseId) return
  try {
    await changeReleaseState(record.releaseId, state)
    message.success(t('firmware.saveSuccess'))
    await loadReleases()
  } catch (error) {
    message.error(t('firmware.saveFailed'))
  }
}

async function removeRelease(releaseId?: number) {
  if (!releaseId) return
  try {
    await deleteRelease(releaseId)
    message.success(t('firmware.deleteSuccess'))
    await loadReleases()
  } catch (error) {
    message.error(t('firmware.deleteFailed'))
  }
}

onMounted(() => {
  loadFirmwares()
  loadFirmwareOptions()
})
</script>

<style scoped>
.firmware-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.firmware-view .arrow {
  margin: 0 8px;
  color: #8c8c8c;
}

.firmware-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

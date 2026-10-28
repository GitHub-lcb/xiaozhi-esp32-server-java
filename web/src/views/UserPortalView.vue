<template>
  <div class="portal-view">
    <div class="portal-header">
      <h2>{{ t('portal.title') }}</h2>
      <span class="muted">{{ t('portal.subtitle') }}</span>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col v-for="device in devices" :key="device.deviceId" :xs="24" :sm="12" :lg="8" :xl="6">
          <a-card class="device-card" :bordered="false" hoverable>
            <div class="device-card__head">
              <a-avatar :size="44" class="device-card__avatar">{{ (device.deviceName || device.deviceId).slice(0, 1) }}</a-avatar>
              <div>
                <div class="device-card__name">{{ device.deviceName || device.deviceId }}</div>
                <a-tag :color="device.state === '1' ? 'green' : device.state === '2' ? 'blue' : 'default'">
                  {{ stateLabel(device.state) }}
                </a-tag>
              </div>
            </div>

            <div class="device-card__meta">
              <div>{{ t('portal.role') }}：{{ device.roleName || '-' }}</div>
              <div>{{ t('portal.channel') }}：{{ channelLabel(device.channel) }}</div>
              <div>{{ t('portal.firmware') }}：{{ device.version || '-' }}</div>
            </div>

            <div class="device-card__actions">
              <a-button size="small" type="primary" @click="runSelfCheck(device)">{{ t('portal.selfCheck') }}</a-button>
              <a-button size="small" @click="sendGreeting(device)">{{ t('portal.greeting') }}</a-button>
            </div>
          </a-card>
        </a-col>
      </a-row>
      <a-empty v-if="!loading && devices.length === 0" :description="t('portal.empty')" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { queryDevices, selfCheckDevice } from '@/services/device'
import { addPushMessage, sendPushMessage } from '@/services/push'
import type { Device } from '@/types/device'

const { t } = useI18n()
const devices = ref<Device[]>([])
const loading = ref(false)

function stateLabel(state?: string) {
  if (state === '1') return t('device.onlineStatus')
  if (state === '2') return t('device.standbyStatus')
  return t('device.offlineStatus')
}

function channelLabel(channel?: string) {
  if (!channel) return '-'
  return channel === 'mqtt' ? 'MQTT' : 'WebSocket'
}

async function loadDevices() {
  loading.value = true
  try {
    const response = await queryDevices({ pageNo: 1, pageSize: 50 })
    devices.value = response.data?.list || []
  } catch (error) {
    message.error(t('portal.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function runSelfCheck(device: Device) {
  try {
    const response = await selfCheckDevice(device.deviceId)
    const summary = response.data?.summary
    message.success(t('portal.selfCheckDone', { summary: summary || '-' }))
  } catch (error) {
    message.error(t('portal.selfCheckFailed'))
  }
}

async function sendGreeting(device: Device) {
  try {
    const created = await addPushMessage({
      title: t('portal.greetingTitle'),
      content: t('portal.greetingContent'),
      targetType: 'device',
      targetValue: device.deviceId,
    })
    const pushId = (created.data as { pushId?: number } | undefined)?.pushId
    if (pushId) {
      await sendPushMessage(pushId)
      message.success(t('portal.greetingSent'))
    }
  } catch (error) {
    message.error(t('portal.greetingFailed'))
  }
}

onMounted(loadDevices)
</script>

<style scoped>
.portal-view {
  padding: 24px;
}

.portal-header {
  margin-bottom: 20px;
}

.portal-header h2 {
  margin: 0 0 4px;
}

.device-card__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.device-card__avatar {
  background: #1677ff;
  font-size: 18px;
}

.device-card__name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.device-card__meta {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.9;
  margin-bottom: 12px;
}

.device-card__actions {
  display: flex;
  gap: 8px;
}

.muted {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

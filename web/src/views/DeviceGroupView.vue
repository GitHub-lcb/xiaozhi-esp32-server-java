<template>
  <div class="group-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <span class="muted">{{ t('deviceGroup.tip') }}</span>
        <a-space>
          <a-input v-model:value="groupName" :placeholder="t('deviceGroup.nameTip')" style="width: 200px" />
          <a-button type="primary" :loading="creating" @click="create">{{ t('deviceGroup.create') }}</a-button>
        </a-space>
      </div>

      <a-space class="toolbar">
        <a-input-number v-model:value="groupId" :min="1" :placeholder="t('deviceGroup.groupIdTip')" style="width: 160px" />
        <a-button :loading="loading" @click="load">{{ t('deviceGroup.load') }}</a-button>
        <a-input v-model:value="newDeviceId" :placeholder="t('deviceGroup.deviceIdTip')" style="width: 240px" />
        <a-button :disabled="!groupId" @click="addMember">{{ t('deviceGroup.addMember') }}</a-button>
      </a-space>

      <a-list :data-source="deviceIds" :locale="{ emptyText: t('deviceGroup.empty') }" size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <span>{{ item }}</span>
            <template #actions>
              <a-popconfirm :title="t('deviceGroup.removeConfirm')" @confirm="removeMember(item)">
                <a-button type="link" size="small" danger>{{ t('deviceGroup.remove') }}</a-button>
              </a-popconfirm>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { addGroupMember, createDeviceGroup, queryDeviceGroup, removeGroupMember } from '@/services/deviceGroup'

const { t } = useI18n()
const groupName = ref('')
const groupId = ref<number | undefined>(undefined)
const newDeviceId = ref('')
const deviceIds = ref<string[]>([])
const loading = ref(false)
const creating = ref(false)

async function create() {
  if (!groupName.value) {
    message.warning(t('deviceGroup.nameRequired'))
    return
  }
  creating.value = true
  try {
    const response = await createDeviceGroup(groupName.value)
    groupId.value = response.data
    message.success(t('deviceGroup.created', { id: response.data }))
    await load()
  } catch (error) {
    message.error(t('deviceGroup.createFailed'))
  } finally {
    creating.value = false
  }
}

async function load() {
  if (!groupId.value) return
  loading.value = true
  try {
    const response = await queryDeviceGroup(groupId.value)
    deviceIds.value = response.data?.[0]?.deviceIds || []
  } catch (error) {
    message.error(t('deviceGroup.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function addMember() {
  if (!groupId.value || !newDeviceId.value) {
    message.warning(t('deviceGroup.deviceRequired'))
    return
  }
  try {
    await addGroupMember(groupId.value, newDeviceId.value)
    newDeviceId.value = ''
    await load()
  } catch (error) {
    message.error(t('deviceGroup.addFailed'))
  }
}

async function removeMember(deviceId: string) {
  if (!groupId.value) return
  try {
    await removeGroupMember(groupId.value, deviceId)
    await load()
  } catch (error) {
    message.error(t('deviceGroup.removeFailed'))
  }
}
</script>

<style scoped>
.group-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.group-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

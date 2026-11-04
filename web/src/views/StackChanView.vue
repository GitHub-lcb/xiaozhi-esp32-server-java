<template>
  <div class="stackchan-view">
    <p class="muted page-tip">{{ t('stackChan.tip') }}</p>

    <a-tabs v-model:activeKey="activeTab" class="stackchan-tabs">
      <!-- 设备 -->
      <a-tab-pane key="devices" :tab="t('stackChan.tab.devices')">
        <div class="toolbar">
          <a-button :loading="deviceLoading" @click="loadDevices">{{ t('stackChan.refresh') }}</a-button>
          <a-button type="link" @click="goDevicePage">{{ t('stackChan.deviceManage') }}</a-button>
        </div>
        <a-table
          :columns="deviceColumns"
          :data-source="devices"
          :loading="deviceLoading"
          :pagination="false"
          :scroll="{ x: 1100 }"
          row-key="deviceId"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'deviceId'">
              <a-tooltip :title="record.deviceId">
                <span class="mono">{{ record.deviceId }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.key === 'online'">
              <a-tag :color="record.channelOnline ? 'green' : 'default'">
                {{ record.channelOnline ? t('stackChan.online') : t('stackChan.offline') }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'roleName'">{{ record.roleName || '-' }}</template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="openDeviceEdit(record)">{{ t('stackChan.edit') }}</a-button>
                <a-popconfirm :title="t('stackChan.rebootConfirm')" @confirm="rebootDevice(record)">
                  <a-button type="link" size="small">{{ t('stackChan.reboot') }}</a-button>
                </a-popconfirm>
                <a-popconfirm :title="t('stackChan.unbindConfirm')" @confirm="unbindDevice(record)">
                  <a-button type="link" size="small" danger>{{ t('stackChan.unbind') }}</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
          <template #emptyText><a-empty :description="t('stackChan.deviceEmpty')" /></template>
        </a-table>
      </a-tab-pane>

      <!-- 跳舞 -->
      <a-tab-pane key="dances" :tab="t('stackChan.tab.dances')">
        <div class="toolbar">
          <a-button type="primary" @click="openDanceCreate">{{ t('stackChan.addDance') }}</a-button>
          <a-button :loading="danceLoading" @click="loadDances">{{ t('stackChan.refresh') }}</a-button>
        </div>
        <a-table
          :columns="danceColumns"
          :data-source="dances"
          :loading="danceLoading"
          :pagination="false"
          :scroll="{ x: 1000 }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'mac'"><span class="mono">{{ record.mac }}</span></template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="openDanceEdit(record)">{{ t('stackChan.edit') }}</a-button>
                <a-popconfirm :title="t('stackChan.deleteDanceConfirm')" @confirm="removeDance(record)">
                  <a-button type="link" size="small" danger>{{ t('stackChan.delete') }}</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
          <template #emptyText><a-empty :description="t('stackChan.noDances')" /></template>
        </a-table>
      </a-tab-pane>

      <!-- 社区 -->
      <a-tab-pane key="community" :tab="t('stackChan.tab.community')">
        <div class="toolbar">
          <a-button :loading="postLoading" @click="loadPosts">{{ t('stackChan.refresh') }}</a-button>
        </div>
        <a-table
          :columns="postColumns"
          :data-source="posts"
          :loading="postLoading"
          :pagination="false"
          :scroll="{ x: 1100 }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'mac'"><span class="mono">{{ record.mac }}</span></template>
            <template v-else-if="column.key === 'contentText'">
              <div class="post-content">{{ record.contentText || '-' }}</div>
              <a v-if="record.contentImage" :href="record.contentImage" target="_blank" rel="noopener">
                {{ t('stackChan.image') }}
              </a>
            </template>
            <template v-else-if="column.key === 'comments'">
              <div v-if="record.postCommentList?.length">
                <div v-for="comment in record.postCommentList" :key="comment.id" class="comment-line">
                  {{ comment.mac }}: {{ comment.content }}
                </div>
              </div>
              <span v-else class="muted">-</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-popconfirm :title="t('stackChan.deletePostConfirm')" @confirm="removePost(record)">
                <a-button type="link" size="small" danger>{{ t('stackChan.delete') }}</a-button>
              </a-popconfirm>
            </template>
          </template>
          <template #emptyText><a-empty :description="t('stackChan.noPosts')" /></template>
        </a-table>
      </a-tab-pane>

      <!-- 全景 -->
      <a-tab-pane key="pano" :tab="t('stackChan.tab.pano')">
        <div class="toolbar">
          <a-button type="primary" @click="openPanoCreate">{{ t('stackChan.addPano') }}</a-button>
          <a-button :loading="panoLoading" @click="loadPanos">{{ t('stackChan.refresh') }}</a-button>
        </div>
        <a-table
          :columns="panoColumns"
          :data-source="panos"
          :loading="panoLoading"
          :pagination="false"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'mac'"><span class="mono">{{ record.mac }}</span></template>
            <template v-else-if="column.key === 'panoUrl'">
              <a :href="record.panoUrl" target="_blank" rel="noopener">{{ record.panoUrl }}</a>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-popconfirm :title="t('stackChan.deletePanoConfirm')" @confirm="removePano(record)">
                <a-button type="link" size="small" danger>{{ t('stackChan.delete') }}</a-button>
              </a-popconfirm>
            </template>
          </template>
          <template #emptyText><a-empty :description="t('stackChan.noPano')" /></template>
        </a-table>
      </a-tab-pane>

      <!-- 好友 -->
      <a-tab-pane key="friends" :tab="t('stackChan.tab.friends')">
        <div class="toolbar">
          <a-button :loading="friendLoading" @click="loadFriends">{{ t('stackChan.refresh') }}</a-button>
        </div>
        <a-table
          :columns="friendColumns"
          :data-source="friends"
          :loading="friendLoading"
          :pagination="false"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'macA'"><span class="mono">{{ record.macA }}</span></template>
            <template v-else-if="column.key === 'macB'"><span class="mono">{{ record.macB }}</span></template>
            <template v-else-if="column.key === 'action'">
              <a-popconfirm :title="t('stackChan.deleteFriendConfirm')" @confirm="removeFriend(record)">
                <a-button type="link" size="small" danger>{{ t('stackChan.delete') }}</a-button>
              </a-popconfirm>
            </template>
          </template>
          <template #emptyText><a-empty :description="t('stackChan.noFriends')" /></template>
        </a-table>
      </a-tab-pane>

      <!-- App Store -->
      <a-tab-pane key="apps" :tab="t('stackChan.tab.apps')">
        <div class="toolbar">
          <a-button type="primary" @click="openCreateApp">{{ t('stackChan.addApp') }}</a-button>
          <a-button :loading="loading" @click="loadApps">{{ t('stackChan.refresh') }}</a-button>
        </div>
        <a-table
          :columns="appColumns"
          :data-source="rows"
          :loading="loading"
          :pagination="false"
          :scroll="{ x: 1100 }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'icon'">
              <img v-if="record.appIconUrl" :src="record.appIconUrl" class="app-icon" alt="" />
              <span v-else class="muted">-</span>
            </template>
            <template v-else-if="column.key === 'state'">
              <a-tag :color="record.state === 1 ? 'green' : 'default'">
                {{ record.state === 1 ? t('stackChan.enabled') : t('stackChan.disabled') }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'firmwareUrl'">
              <a v-if="record.firmwareUrl" :href="record.firmwareUrl" target="_blank" rel="noopener">
                {{ t('stackChan.download') }}
              </a>
              <span v-else class="muted">-</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="openEditApp(record)">{{ t('stackChan.edit') }}</a-button>
                <a-button type="link" size="small" @click="toggleAppState(record)">
                  {{ record.state === 1 ? t('stackChan.disabled') : t('stackChan.enabled') }}
                </a-button>
                <a-popconfirm :title="t('stackChan.deleteConfirm')" @confirm="removeApp(record)">
                  <a-button type="link" size="small" danger>{{ t('stackChan.delete') }}</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
          <template #emptyText><a-empty :description="t('stackChan.empty')" /></template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <!-- 设备编辑 -->
    <a-modal v-model:open="deviceModalOpen" :title="t('stackChan.editDevice')" :confirm-loading="deviceSaving" @ok="saveDevice">
      <a-form layout="vertical">
        <a-form-item :label="t('stackChan.deviceId')"><a-input :value="deviceForm.deviceId" disabled /></a-form-item>
        <a-form-item :label="t('stackChan.deviceName')">
          <a-input v-model:value="deviceForm.deviceName" :maxlength="100" />
        </a-form-item>
        <a-form-item :label="t('stackChan.role')">
          <a-select v-model:value="deviceForm.roleId" :options="roleOptions" :placeholder="t('stackChan.rolePlaceholder')" allow-clear />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 舞蹈编辑 -->
    <a-modal
      v-model:open="danceModalOpen"
      :title="editingDanceId ? t('stackChan.editDance') : t('stackChan.addDance')"
      :confirm-loading="danceSaving"
      width="640px"
      @ok="saveDance"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('stackChan.mac')" required>
          <a-input v-model:value="danceForm.mac" :disabled="!!editingDanceId" placeholder="aa:bb:cc:dd:ee:ff" />
        </a-form-item>
        <a-form-item :label="t('stackChan.danceName')" required>
          <a-input v-model:value="danceForm.danceName" :maxlength="128" />
        </a-form-item>
        <a-form-item :label="t('stackChan.musicUrl')">
          <a-input v-model:value="danceForm.musicUrl" placeholder="file/music/stackchan_music.mp3" />
        </a-form-item>
        <a-form-item :label="t('stackChan.danceData')" required>
          <a-textarea v-model:value="danceForm.danceDataText" :rows="8" class="mono" />
          <div class="muted form-hint">{{ t('stackChan.danceDataHint') }}</div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 全景编辑 -->
    <a-modal v-model:open="panoModalOpen" :title="t('stackChan.addPano')" :confirm-loading="panoSaving" @ok="savePano">
      <a-form layout="vertical">
        <a-form-item :label="t('stackChan.mac')" required>
          <a-input v-model:value="panoForm.mac" placeholder="aa:bb:cc:dd:ee:ff" />
        </a-form-item>
        <a-form-item :label="t('stackChan.panoUrl')" required>
          <a-input v-model:value="panoForm.url" placeholder="https://..." />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- App 编辑 -->
    <a-modal
      v-model:open="appModalOpen"
      :title="editingAppId ? t('stackChan.editApp') : t('stackChan.addApp')"
      :confirm-loading="appSaving"
      @ok="saveApp"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('stackChan.appName')" required>
          <a-input v-model:value="appForm.appName" :maxlength="128" />
        </a-form-item>
        <a-form-item :label="t('stackChan.description')">
          <a-textarea v-model:value="appForm.description" :rows="3" :maxlength="500" />
        </a-form-item>
        <a-form-item :label="t('stackChan.iconUrl')">
          <a-input v-model:value="appForm.appIconUrl" placeholder="https://..." />
          <a-upload :show-upload-list="false" accept=".png,.jpg,.jpeg,.webp" :before-upload="beforeUploadIcon">
            <a-button size="small" :loading="uploadingIcon" class="upload-btn">{{ t('stackChan.uploadIcon') }}</a-button>
          </a-upload>
        </a-form-item>
        <a-form-item :label="t('stackChan.firmwareUrl')">
          <a-input v-model:value="appForm.firmwareUrl" placeholder="https://..." />
          <a-upload :show-upload-list="false" accept=".bin" :before-upload="beforeUploadFirmware">
            <a-button size="small" :loading="uploadingFirmware" class="upload-btn">{{ t('stackChan.uploadFirmware') }}</a-button>
          </a-upload>
        </a-form-item>
        <a-form-item :label="t('stackChan.state')">
          <a-select v-model:value="appForm.state" :options="stateOptions" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import {
  createStackChanApp,
  createStackChanDance,
  createStackChanPano,
  deleteStackChanApp,
  deleteStackChanDance,
  deleteStackChanFriend,
  deleteStackChanPano,
  deleteStackChanPost,
  queryStackChanApps,
  queryStackChanDances,
  queryStackChanFriends,
  queryStackChanPanos,
  queryStackChanPosts,
  updateStackChanApp,
  updateStackChanDance,
  uploadStackChanFile,
  type StackChanApp,
  type StackChanDance,
  type StackChanFriend,
  type StackChanPano,
  type StackChanPost,
} from '@/services/stackChan'
import { deleteDevice, queryDevices, sendDeviceCommand, updateDevice } from '@/services/device'
import { queryRoles } from '@/services/role'
import type { Device } from '@/types/device'
import type { Role } from '@/types/role'

const { t } = useI18n()
const router = useRouter()
const activeTab = ref('devices')

// ==================== 设备 ====================
const devices = ref<Device[]>([])
const roles = ref<Role[]>([])
const deviceLoading = ref(false)
const deviceModalOpen = ref(false)
const deviceSaving = ref(false)
const deviceForm = reactive({ deviceId: '', deviceName: '', roleId: undefined as number | undefined })

const roleOptions = computed(() => roles.value.map(role => ({ label: role.roleName, value: role.roleId })))

const deviceColumns = computed(() => [
  { title: t('stackChan.deviceId'), key: 'deviceId', width: 190 },
  { title: t('stackChan.deviceName'), dataIndex: 'deviceName', key: 'deviceName', width: 140 },
  { title: t('stackChan.role'), key: 'roleName', width: 140 },
  { title: t('stackChan.type'), dataIndex: 'type', key: 'type', width: 160 },
  { title: t('stackChan.state'), key: 'online', width: 90 },
  { title: t('stackChan.createTime'), dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: t('stackChan.actions'), key: 'action', width: 190 },
])

function isPhysicalDevice(device: Device) {
  return /^([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}$/.test(device.deviceId || '')
}

async function loadDevices() {
  deviceLoading.value = true
  try {
    const res = await queryDevices({ pageNo: 1, pageSize: 200 })
    devices.value = (res.data?.list ?? []).filter(isPhysicalDevice)
  } catch (error) {
    message.error((error as Error).message || t('stackChan.loadFailed'))
  } finally {
    deviceLoading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await queryRoles({ pageNo: 1, pageSize: 100 })
    roles.value = res.data?.list ?? []
  } catch {
    roles.value = []
  }
}

function openDeviceEdit(device: Device) {
  Object.assign(deviceForm, {
    deviceId: device.deviceId,
    deviceName: device.deviceName ?? '',
    roleId: device.roleId ?? undefined,
  })
  deviceModalOpen.value = true
}

async function saveDevice() {
  deviceSaving.value = true
  try {
    await updateDevice({
      deviceId: deviceForm.deviceId,
      deviceName: deviceForm.deviceName,
      roleId: deviceForm.roleId,
    })
    message.success(t('stackChan.saveSuccess'))
    deviceModalOpen.value = false
    await loadDevices()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.saveFailed'))
  } finally {
    deviceSaving.value = false
  }
}

async function rebootDevice(device: Device) {
  try {
    await sendDeviceCommand(device.deviceId, { action: 'reboot' })
    message.success(t('stackChan.rebootSent'))
  } catch (error) {
    message.error((error as Error).message || t('stackChan.operationFailed'))
  }
}

async function unbindDevice(device: Device) {
  try {
    await deleteDevice(device.deviceId)
    message.success(t('stackChan.unbindSuccess'))
    await loadDevices()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.operationFailed'))
  }
}

function goDevicePage() {
  router.push('/device')
}

// ==================== 跳舞 ====================
const dances = ref<StackChanDance[]>([])
const danceLoading = ref(false)
const danceModalOpen = ref(false)
const danceSaving = ref(false)
const editingDanceId = ref<number | null>(null)
const danceForm = reactive({ mac: '', danceName: '', musicUrl: '', danceDataText: '[]' })

const danceColumns = computed(() => [
  { title: t('stackChan.mac'), key: 'mac', width: 190 },
  { title: t('stackChan.danceName'), dataIndex: 'danceName', key: 'danceName', width: 180 },
  { title: t('stackChan.musicUrl'), dataIndex: 'musicUrl', key: 'musicUrl', ellipsis: true },
  { title: t('stackChan.actions'), key: 'action', width: 140 },
])

async function loadDances() {
  danceLoading.value = true
  try {
    const res = await queryStackChanDances()
    dances.value = res.data ?? []
  } catch (error) {
    message.error((error as Error).message || t('stackChan.loadFailed'))
  } finally {
    danceLoading.value = false
  }
}

function openDanceCreate() {
  editingDanceId.value = null
  Object.assign(danceForm, { mac: '', danceName: '', musicUrl: '', danceDataText: '[]' })
  danceModalOpen.value = true
}

function openDanceEdit(record: StackChanDance) {
  editingDanceId.value = record.id ?? null
  Object.assign(danceForm, {
    mac: record.mac ?? '',
    danceName: record.danceName ?? '',
    musicUrl: record.musicUrl ?? '',
    danceDataText: JSON.stringify(record.danceData ?? [], null, 2),
  })
  danceModalOpen.value = true
}

async function saveDance() {
  let danceData: unknown
  try {
    danceData = JSON.parse(danceForm.danceDataText)
  } catch {
    message.warning(t('stackChan.invalidDanceData'))
    return
  }
  if (!Array.isArray(danceData)) {
    message.warning(t('stackChan.invalidDanceData'))
    return
  }
  if (!danceForm.mac.trim() || !danceForm.danceName.trim()) {
    message.warning(t('stackChan.requiredFields'))
    return
  }
  danceSaving.value = true
  try {
    if (editingDanceId.value) {
      await updateStackChanDance({ id: editingDanceId.value, danceName: danceForm.danceName, musicUrl: danceForm.musicUrl, danceData })
    } else {
      await createStackChanDance({ mac: danceForm.mac, danceName: danceForm.danceName, musicUrl: danceForm.musicUrl, danceData })
    }
    message.success(t('stackChan.saveSuccess'))
    danceModalOpen.value = false
    await loadDances()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.saveFailed'))
  } finally {
    danceSaving.value = false
  }
}

async function removeDance(record: StackChanDance) {
  if (!record.id) return
  try {
    await deleteStackChanDance(record.id)
    message.success(t('stackChan.deleteSuccess'))
    await loadDances()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.deleteFailed'))
  }
}

// ==================== 社区 ====================
const posts = ref<StackChanPost[]>([])
const postLoading = ref(false)

const postColumns = computed(() => [
  { title: t('stackChan.mac'), key: 'mac', width: 190 },
  { title: t('stackChan.content'), key: 'contentText' },
  { title: t('stackChan.comments'), key: 'comments', width: 280 },
  { title: t('stackChan.createTime'), dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: t('stackChan.actions'), key: 'action', width: 90 },
])

async function loadPosts() {
  postLoading.value = true
  try {
    const res = await queryStackChanPosts(1, 50)
    posts.value = res.data ?? []
  } catch (error) {
    message.error((error as Error).message || t('stackChan.loadFailed'))
  } finally {
    postLoading.value = false
  }
}

async function removePost(record: StackChanPost) {
  if (!record.id) return
  try {
    await deleteStackChanPost(record.id)
    message.success(t('stackChan.deleteSuccess'))
    await loadPosts()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.deleteFailed'))
  }
}

// ==================== 全景 ====================
const panos = ref<StackChanPano[]>([])
const panoLoading = ref(false)
const panoModalOpen = ref(false)
const panoSaving = ref(false)
const panoForm = reactive({ mac: '', url: '' })

const panoColumns = computed(() => [
  { title: t('stackChan.mac'), key: 'mac', width: 190 },
  { title: t('stackChan.panoUrl'), key: 'panoUrl' },
  { title: t('stackChan.createTime'), dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: t('stackChan.actions'), key: 'action', width: 90 },
])

async function loadPanos() {
  panoLoading.value = true
  try {
    const res = await queryStackChanPanos()
    panos.value = res.data ?? []
  } catch (error) {
    message.error((error as Error).message || t('stackChan.loadFailed'))
  } finally {
    panoLoading.value = false
  }
}

function openPanoCreate() {
  Object.assign(panoForm, { mac: '', url: '' })
  panoModalOpen.value = true
}

async function savePano() {
  if (!panoForm.mac.trim() || !panoForm.url.trim()) {
    message.warning(t('stackChan.requiredFields'))
    return
  }
  panoSaving.value = true
  try {
    await createStackChanPano({ mac: panoForm.mac, url: panoForm.url })
    message.success(t('stackChan.saveSuccess'))
    panoModalOpen.value = false
    await loadPanos()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.saveFailed'))
  } finally {
    panoSaving.value = false
  }
}

async function removePano(record: StackChanPano) {
  if (!record.id) return
  try {
    await deleteStackChanPano(record.id)
    message.success(t('stackChan.deleteSuccess'))
    await loadPanos()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.deleteFailed'))
  }
}

// ==================== 好友 ====================
const friends = ref<StackChanFriend[]>([])
const friendLoading = ref(false)

const friendColumns = computed(() => [
  { title: t('stackChan.macA'), key: 'macA', width: 220 },
  { title: t('stackChan.macB'), key: 'macB', width: 220 },
  { title: t('stackChan.createTime'), dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: t('stackChan.actions'), key: 'action', width: 90 },
])

async function loadFriends() {
  friendLoading.value = true
  try {
    const res = await queryStackChanFriends()
    friends.value = res.data ?? []
  } catch (error) {
    message.error((error as Error).message || t('stackChan.loadFailed'))
  } finally {
    friendLoading.value = false
  }
}

async function removeFriend(record: StackChanFriend) {
  if (!record.id) return
  try {
    await deleteStackChanFriend(record.id)
    message.success(t('stackChan.deleteSuccess'))
    await loadFriends()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.deleteFailed'))
  }
}

// ==================== App Store ====================
const rows = ref<StackChanApp[]>([])
const loading = ref(false)
const appModalOpen = ref(false)
const appSaving = ref(false)
const editingAppId = ref<number | null>(null)
const uploadingIcon = ref(false)
const uploadingFirmware = ref(false)
const appForm = reactive<StackChanApp>({ appName: '', description: '', appIconUrl: '', firmwareUrl: '', state: 1 })

const stateOptions = computed(() => [
  { value: 1, label: t('stackChan.enabled') },
  { value: 0, label: t('stackChan.disabled') },
])

const appColumns = computed(() => [
  { title: t('stackChan.icon'), key: 'icon', width: 80 },
  { title: t('stackChan.appName'), dataIndex: 'appName', key: 'appName' },
  { title: t('stackChan.description'), dataIndex: 'description', key: 'description', ellipsis: true },
  { title: t('stackChan.firmwareUrl'), key: 'firmwareUrl', width: 100 },
  { title: t('stackChan.state'), key: 'state', width: 90 },
  { title: t('stackChan.actions'), key: 'action', width: 190 },
])

async function loadApps() {
  loading.value = true
  try {
    const res = await queryStackChanApps()
    rows.value = res.data ?? []
  } catch (error) {
    message.error((error as Error).message || t('stackChan.loadFailed'))
  } finally {
    loading.value = false
  }
}

function resetAppForm() {
  Object.assign(appForm, { appName: '', description: '', appIconUrl: '', firmwareUrl: '', state: 1 })
}

function openCreateApp() {
  editingAppId.value = null
  resetAppForm()
  appModalOpen.value = true
}

function openEditApp(record: StackChanApp) {
  editingAppId.value = record.id ?? null
  Object.assign(appForm, {
    appName: record.appName,
    description: record.description ?? '',
    appIconUrl: record.appIconUrl ?? '',
    firmwareUrl: record.firmwareUrl ?? '',
    state: record.state ?? 1,
  })
  appModalOpen.value = true
}

async function saveApp() {
  if (!appForm.appName.trim()) {
    message.warning(t('stackChan.appNameRequired'))
    return
  }
  appSaving.value = true
  try {
    if (editingAppId.value) {
      await updateStackChanApp({ ...appForm, id: editingAppId.value })
    } else {
      await createStackChanApp({ ...appForm })
    }
    message.success(t('stackChan.saveSuccess'))
    appModalOpen.value = false
    await loadApps()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.saveFailed'))
  } finally {
    appSaving.value = false
  }
}

async function toggleAppState(record: StackChanApp) {
  if (!record.id) return
  try {
    await updateStackChanApp({ ...record, state: record.state === 1 ? 0 : 1 })
    message.success(t('stackChan.saveSuccess'))
    await loadApps()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.saveFailed'))
  }
}

async function removeApp(record: StackChanApp) {
  if (!record.id) return
  try {
    await deleteStackChanApp(record.id)
    message.success(t('stackChan.deleteSuccess'))
    await loadApps()
  } catch (error) {
    message.error((error as Error).message || t('stackChan.deleteFailed'))
  }
}

const beforeUploadIcon: UploadProps['beforeUpload'] = async (file) => {
  uploadingIcon.value = true
  try {
    const res = await uploadStackChanFile(file as File, 'icons')
    appForm.appIconUrl = res.data?.url ?? ''
    message.success(t('stackChan.uploadSuccess'))
  } catch (error) {
    message.error((error as Error).message || t('stackChan.uploadFailed'))
  } finally {
    uploadingIcon.value = false
  }
  return false
}

const beforeUploadFirmware: UploadProps['beforeUpload'] = async (file) => {
  uploadingFirmware.value = true
  try {
    const res = await uploadStackChanFile(file as File, 'firmware')
    appForm.firmwareUrl = res.data?.url ?? ''
    message.success(t('stackChan.uploadSuccess'))
  } catch (error) {
    message.error((error as Error).message || t('stackChan.uploadFailed'))
  } finally {
    uploadingFirmware.value = false
  }
  return false
}

onMounted(async () => {
  await Promise.all([loadDevices(), loadRoles(), loadDances(), loadPosts(), loadPanos(), loadFriends(), loadApps()])
})
</script>

<style scoped>
.stackchan-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.page-tip {
  margin: 0;
  font-size: 13px;
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}
.app-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
}
.upload-btn {
  margin-top: 8px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}
.post-content {
  white-space: pre-wrap;
}
.comment-line {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
}
.form-hint {
  margin-top: 6px;
  font-size: 12px;
}
.muted {
  color: rgba(0, 0, 0, 0.45);
}
</style>

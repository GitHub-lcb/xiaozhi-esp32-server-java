<template>
  <div class="plugin-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <span class="muted">{{ t('plugin.tip') }}</span>
        <a-button @click="loadData">{{ t('plugin.refresh') }}</a-button>
      </div>

      <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'enabled'">
            <a-switch
              :checked="record.enabled"
              :loading="toggling === record.id"
              @change="(checked: boolean) => toggle(record, checked)"
            />
          </template>
        </template>
      </a-table>
      <a-alert v-if="!loading && rows.length === 0" type="info" :message="t('plugin.empty')" show-icon />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { queryPlugins, togglePlugin, type PluginInfo } from '@/services/plugin'

const { t } = useI18n()
const rows = ref<PluginInfo[]>([])
const loading = ref(false)
const toggling = ref<string | null>(null)

const columns = computed(() => [
  { title: t('plugin.name'), dataIndex: 'name', key: 'name', width: 200 },
  { title: t('plugin.id'), dataIndex: 'id', key: 'id', width: 220 },
  { title: t('plugin.version'), dataIndex: 'version', key: 'version', width: 110 },
  { title: t('plugin.description'), dataIndex: 'description', key: 'description', ellipsis: true },
  { title: t('plugin.enabled'), key: 'enabled', width: 100 },
])

async function loadData() {
  loading.value = true
  try {
    const response = await queryPlugins()
    rows.value = response.data || []
  } catch (error) {
    message.error(t('plugin.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function toggle(record: PluginInfo, enabled: boolean) {
  toggling.value = record.id
  try {
    await togglePlugin(record.id, enabled)
    record.enabled = enabled
    message.success(enabled ? t('plugin.enabledOn') : t('plugin.enabledOff'))
  } catch (error) {
    message.error(t('plugin.toggleFailed'))
  } finally {
    toggling.value = null
  }
}

onMounted(loadData)
</script>

<style scoped>
.plugin-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.plugin-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

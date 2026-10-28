<template>
  <div class="theme-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <a-space>
          <a-input-number v-model:value="roleId" :min="1" :placeholder="t('theme.roleIdTip')" style="width: 180px" />
          <a-button type="primary" :loading="loading" @click="load">{{ t('theme.generate') }}</a-button>
          <a-button :disabled="!resource" @click="copy">{{ t('theme.copy') }}</a-button>
          <a-button :disabled="!resource" @click="download">{{ t('theme.download') }}</a-button>
        </a-space>
        <span class="muted">{{ t('theme.tip') }}</span>
      </div>

      <a-spin :spinning="loading">
        <pre v-if="resource" class="json-preview">{{ pretty }}</pre>
        <a-empty v-else :description="t('theme.empty')" />
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { fetchThemeResource, type ThemeResource } from '@/services/theme'

const { t } = useI18n()
const roleId = ref<number | undefined>(undefined)
const resource = ref<ThemeResource | null>(null)
const loading = ref(false)

const pretty = computed(() => (resource.value ? JSON.stringify(resource.value, null, 2) : ''))

async function load() {
  if (!roleId.value) {
    message.warning(t('theme.requiredTip'))
    return
  }
  loading.value = true
  try {
    const response = await fetchThemeResource(roleId.value)
    resource.value = response.data || null
  } catch (error) {
    resource.value = null
    message.error(t('theme.failed'))
  } finally {
    loading.value = false
  }
}

async function copy() {
  try {
    await navigator.clipboard.writeText(pretty.value)
    message.success(t('common.copySuccess'))
  } catch (error) {
    message.error(t('theme.copyFailed'))
  }
}

function download() {
  const blob = new Blob([pretty.value], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `theme-${roleId.value}.json`
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.theme-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.theme-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}

.json-preview {
  margin: 0;
  padding: 16px;
  max-height: 520px;
  overflow: auto;
  background: #0b1220;
  color: #d7e2f0;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
}
</style>

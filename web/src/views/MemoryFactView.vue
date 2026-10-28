<template>
  <div class="memory-fact-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <a-space>
          <a-input v-model:value="query.content" :placeholder="t('memoryFact.contentPlaceholder')" allow-clear style="width: 220px" @press-enter="loadData" />
          <a-input v-model:value="query.memberName" :placeholder="t('memoryFact.memberPlaceholder')" allow-clear style="width: 160px" @press-enter="loadData" />
          <a-button type="primary" @click="loadData">{{ t('common.search') }}</a-button>
        </a-space>
        <a-space>
          <a-button @click="extractVisible = true">{{ t('memoryFact.extract') }}</a-button>
          <a-button type="primary" @click="openModal">{{ t('memoryFact.create') }}</a-button>
        </a-space>
      </div>

      <a-table :columns="columns" :data-source="rows" :loading="loading" :pagination="pagination" row-key="factId" size="middle" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'memberName'">
            <a-tag v-if="record.memberName">{{ record.memberName }}</a-tag>
            <span v-else class="muted">{{ t('memoryFact.family') }}</span>
          </template>
          <template v-else-if="column.key === 'source'">
            <a-tag :color="record.source === 'auto' ? 'blue' : 'default'">
              {{ record.source === 'auto' ? t('memoryFact.source.auto') : t('memoryFact.source.manual') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm :title="t('memoryFact.deleteConfirm')" @confirm="remove(record.factId)">
              <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="extractVisible"
      :title="t('memoryFact.extract')"
      :confirm-loading="extracting"
      @ok="runExtract"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('memoryFact.deviceId')" required>
          <a-input v-model:value="extractForm.deviceId" :placeholder="t('memoryFact.deviceIdTip')" />
        </a-form-item>
        <a-form-item :label="t('memoryFact.roleId')" required>
          <a-input-number v-model:value="extractForm.roleId" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item :label="t('memoryFact.memberName')">
          <a-input v-model:value="extractForm.memberName" :maxlength="50" :placeholder="t('memoryFact.memberTip')" />
        </a-form-item>
        <a-alert type="info" :message="t('memoryFact.extractTip')" show-icon />
      </a-form>
    </a-modal>

    <a-modal v-model:open="modalVisible" :title="t('memoryFact.create')" :confirm-loading="submitting" @ok="submit">
      <a-form layout="vertical">
        <a-form-item :label="t('memoryFact.content')" required>
          <a-textarea v-model:value="form.content" :rows="3" :maxlength="500" show-count :placeholder="t('memoryFact.contentTip')" />
        </a-form-item>
        <a-form-item :label="t('memoryFact.memberName')">
          <a-input v-model:value="form.memberName" :maxlength="50" :placeholder="t('memoryFact.memberTip')" />
        </a-form-item>
        <a-form-item :label="t('memoryFact.category')">
          <a-input v-model:value="form.category" :maxlength="50" :placeholder="t('memoryFact.categoryTip')" />
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
import { addMemoryFact, deleteMemoryFact, extractMemoryFacts, queryMemoryFacts } from '@/services/memoryFact'
import type { MemoryFact } from '@/types/memoryFact'

const { t } = useI18n()

const rows = ref<MemoryFact[]>([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const query = reactive({ pageNo: 1, pageSize: 10, content: '', memberName: '' })
const form = reactive<Partial<MemoryFact>>({ content: '', memberName: '', category: '' })
const extractVisible = ref(false)
const extracting = ref(false)
const extractForm = reactive<{ deviceId: string; roleId?: number; memberName: string }>({
  deviceId: '',
  roleId: undefined,
  memberName: '',
})

const columns = computed(() => [
  { title: t('memoryFact.content'), dataIndex: 'content', key: 'content', ellipsis: true },
  { title: t('memoryFact.memberName'), key: 'memberName', width: 140 },
  { title: t('memoryFact.category'), dataIndex: 'category', key: 'category', width: 120 },
  { title: t('memoryFact.sourceLabel'), key: 'source', width: 110 },
  { title: t('memoryFact.createTime'), dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: t('memoryFact.action'), key: 'action', width: 90 },
])

const pagination = computed<TablePaginationConfig>(() => ({
  current: query.pageNo,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: true,
}))

async function loadData() {
  loading.value = true
  try {
    const response = await queryMemoryFacts({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      content: query.content || undefined,
      memberName: query.memberName || undefined,
    })
    rows.value = response.data?.list || []
    total.value = response.data?.total || 0
  } catch (error) {
    message.error(t('memoryFact.loadFailed'))
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
  form.content = ''
  form.memberName = ''
  form.category = ''
  modalVisible.value = true
}

async function submit() {
  if (!form.content) {
    message.warning(t('memoryFact.requiredTip'))
    return
  }
  submitting.value = true
  try {
    await addMemoryFact({ ...form })
    message.success(t('memoryFact.saveSuccess'))
    modalVisible.value = false
    await loadData()
  } catch (error) {
    message.error(t('memoryFact.saveFailed'))
  } finally {
    submitting.value = false
  }
}

async function runExtract() {
  if (!extractForm.deviceId || !extractForm.roleId) {
    message.warning(t('memoryFact.extractRequired'))
    return
  }
  extracting.value = true
  try {
    const response = await extractMemoryFacts({
      deviceId: extractForm.deviceId,
      roleId: extractForm.roleId,
      memberName: extractForm.memberName || undefined,
    })
    const created = response.data?.length || 0
    message.success(t('memoryFact.extractDone', { count: created }))
    extractVisible.value = false
    await loadData()
  } catch (error) {
    message.error(t('memoryFact.extractFailed'))
  } finally {
    extracting.value = false
  }
}

async function remove(factId?: number) {
  if (!factId) return
  try {
    await deleteMemoryFact(factId)
    message.success(t('memoryFact.deleteSuccess'))
    await loadData()
  } catch (error) {
    message.error(t('memoryFact.deleteFailed'))
  }
}

onMounted(loadData)
</script>

<style scoped>
.memory-fact-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.memory-fact-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

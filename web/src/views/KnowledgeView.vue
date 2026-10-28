<template>
  <div class="knowledge-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <a-input
          v-model:value="query.name"
          :placeholder="t('knowledge.namePlaceholder')"
          allow-clear
          style="width: 220px"
          @press-enter="loadData"
        />
        <a-button type="primary" @click="loadData">{{ t('common.search') }}</a-button>
        <a-button type="primary" @click="openBaseModal()">{{ t('knowledge.create') }}</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="knowledgeId"
        size="middle"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'state'">
            <a-tag :color="record.state === '1' ? 'green' : 'default'">
              {{ record.state === '1' ? t('knowledge.state.enabled') : t('knowledge.state.disabled') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openDocs(record)">{{ t('knowledge.docs') }}</a-button>
              <a-button type="link" size="small" @click="openBaseModal(record)">{{ t('common.edit') }}</a-button>
              <a-popconfirm :title="t('knowledge.deleteConfirm')" @confirm="removeBase(record.knowledgeId)">
                <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新建/编辑 -->
    <a-modal
      v-model:open="baseModalVisible"
      :title="baseForm.knowledgeId ? t('knowledge.edit') : t('knowledge.create')"
      :confirm-loading="submitting"
      @ok="submitBase"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('knowledge.name')" required>
          <a-input v-model:value="baseForm.name" :maxlength="100" show-count />
        </a-form-item>
        <a-form-item :label="t('knowledge.description')">
          <a-textarea v-model:value="baseForm.description" :rows="2" :maxlength="500" show-count />
        </a-form-item>
        <a-form-item :label="t('knowledge.roleId')">
          <a-input-number v-model:value="baseForm.roleId" :min="1" style="width: 100%" :placeholder="t('knowledge.roleIdTip')" />
        </a-form-item>
        <a-form-item :label="t('knowledge.embeddingConfigId')">
          <a-input-number v-model:value="baseForm.embeddingConfigId" :min="1" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 文档 + 检索测试 -->
    <a-drawer v-model:open="docsVisible" :title="currentBase?.name" width="720">
      <a-space class="drawer-tools">
        <a-upload :show-upload-list="false" :before-upload="handleUpload">
          <a-button type="primary" :loading="uploading">{{ t('knowledge.upload') }}</a-button>
        </a-upload>
        <span class="muted">{{ t('knowledge.uploadTip') }}</span>
      </a-space>

      <a-table
        :columns="docColumns"
        :data-source="docs"
        :loading="docsLoading"
        row-key="docId"
        size="small"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'state'">
            <a-tag :color="record.state === '1' ? 'green' : record.state === '2' ? 'red' : 'blue'">
              {{ docState(record.state) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm :title="t('knowledge.docDeleteConfirm')" @confirm="removeDoc(record.docId)">
              <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>

      <a-divider>{{ t('knowledge.searchTest') }}</a-divider>
      <a-space class="drawer-tools">
        <a-input
          v-model:value="searchQuery"
          :placeholder="t('knowledge.searchPlaceholder')"
          style="width: 360px"
          @press-enter="runSearch"
        />
        <a-button :loading="searching" @click="runSearch">{{ t('knowledge.search') }}</a-button>
      </a-space>
      <a-list :data-source="hits" size="small" :locale="{ emptyText: t('knowledge.noHits') }">
        <template #renderItem="{ item }">
          <a-list-item>
            <div>
              <div class="hit-content">{{ item.content }}</div>
              <div class="muted">chunk #{{ item.chunkId }}</div>
            </div>
          </a-list-item>
        </template>
      </a-list>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import {
  addKnowledgeBase,
  deleteKnowledgeBase,
  deleteKnowledgeDoc,
  queryKnowledgeBases,
  queryKnowledgeDocs,
  searchKnowledge,
  updateKnowledgeBase,
  uploadKnowledgeDoc,
} from '@/services/knowledge'
import type { KnowledgeBase, KnowledgeDoc, KnowledgeHit } from '@/types/knowledge'

const { t } = useI18n()

const rows = ref<KnowledgeBase[]>([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const baseModalVisible = ref(false)
const docsVisible = ref(false)
const docsLoading = ref(false)
const uploading = ref(false)
const searching = ref(false)
const docs = ref<KnowledgeDoc[]>([])
const hits = ref<KnowledgeHit[]>([])
const currentBase = ref<KnowledgeBase | null>(null)
const searchQuery = ref('')

const query = reactive({ pageNo: 1, pageSize: 10, name: '' })
const baseForm = reactive<Partial<KnowledgeBase>>({ name: '', description: '', roleId: undefined, embeddingConfigId: undefined })

const columns = computed(() => [
  { title: t('knowledge.name'), dataIndex: 'name', key: 'name', width: 200 },
  { title: t('knowledge.description'), dataIndex: 'description', key: 'description', ellipsis: true },
  { title: t('knowledge.roleId'), dataIndex: 'roleId', key: 'roleId', width: 120 },
  { title: t('knowledge.stateLabel'), key: 'state', width: 100 },
  { title: t('knowledge.actionLabel'), key: 'action', width: 220 },
])

const docColumns = computed(() => [
  { title: t('knowledge.fileName'), dataIndex: 'fileName', key: 'fileName', ellipsis: true },
  { title: t('knowledge.chunkCount'), dataIndex: 'chunkCount', key: 'chunkCount', width: 90 },
  { title: t('knowledge.docState'), key: 'state', width: 90 },
  { title: t('knowledge.actionLabel'), key: 'action', width: 90 },
])

const pagination = computed<TablePaginationConfig>(() => ({
  current: query.pageNo,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: true,
}))

function docState(state: string) {
  if (state === '1') return t('knowledge.docStateReady')
  if (state === '2') return t('knowledge.docStateFailed')
  return t('knowledge.docStateProcessing')
}

async function loadData() {
  loading.value = true
  try {
    const response = await queryKnowledgeBases({ pageNo: query.pageNo, pageSize: query.pageSize, name: query.name || undefined })
    rows.value = response.data?.list || []
    total.value = response.data?.total || 0
  } catch (error) {
    message.error(t('knowledge.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onTableChange(p: TablePaginationConfig) {
  query.pageNo = p.current || 1
  query.pageSize = p.pageSize || 10
  loadData()
}

function openBaseModal(record?: KnowledgeBase) {
  baseForm.knowledgeId = record?.knowledgeId
  baseForm.name = record?.name || ''
  baseForm.description = record?.description || ''
  baseForm.roleId = record?.roleId
  baseForm.embeddingConfigId = record?.embeddingConfigId
  baseModalVisible.value = true
}

async function submitBase() {
  if (!baseForm.name) {
    message.warning(t('knowledge.requiredTip'))
    return
  }
  submitting.value = true
  try {
    if (baseForm.knowledgeId) {
      await updateKnowledgeBase({ ...baseForm })
    } else {
      await addKnowledgeBase({ ...baseForm })
    }
    message.success(t('knowledge.saveSuccess'))
    baseModalVisible.value = false
    await loadData()
  } catch (error) {
    message.error(t('knowledge.saveFailed'))
  } finally {
    submitting.value = false
  }
}

async function removeBase(knowledgeId?: number) {
  if (!knowledgeId) return
  try {
    await deleteKnowledgeBase(knowledgeId)
    message.success(t('knowledge.deleteSuccess'))
    await loadData()
  } catch (error) {
    message.error(t('knowledge.deleteFailed'))
  }
}

async function openDocs(record: KnowledgeBase) {
  currentBase.value = record
  hits.value = []
  searchQuery.value = ''
  docsVisible.value = true
  await loadDocs()
}

async function loadDocs() {
  if (!currentBase.value?.knowledgeId) return
  docsLoading.value = true
  try {
    const response = await queryKnowledgeDocs(currentBase.value.knowledgeId)
    docs.value = response.data || []
  } catch (error) {
    docs.value = []
  } finally {
    docsLoading.value = false
  }
}

async function handleUpload(file: File) {
  if (!currentBase.value?.knowledgeId) return false
  uploading.value = true
  try {
    await uploadKnowledgeDoc(currentBase.value.knowledgeId, file)
    message.success(t('knowledge.uploadSuccess'))
    await loadDocs()
  } catch (error) {
    message.error(t('knowledge.uploadFailed'))
  } finally {
    uploading.value = false
  }
  return false
}

async function removeDoc(docId: number) {
  try {
    await deleteKnowledgeDoc(docId)
    message.success(t('knowledge.deleteSuccess'))
    await loadDocs()
  } catch (error) {
    message.error(t('knowledge.deleteFailed'))
  }
}

async function runSearch() {
  if (!searchQuery.value) return
  searching.value = true
  try {
    const response = await searchKnowledge({
      knowledgeId: currentBase.value?.knowledgeId,
      query: searchQuery.value,
      topK: 5,
    })
    hits.value = response.data || []
  } catch (error) {
    message.error(t('knowledge.searchFailed'))
  } finally {
    searching.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.knowledge-view .toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.knowledge-view .drawer-tools {
  margin-bottom: 12px;
}

.knowledge-view .hit-content {
  white-space: pre-wrap;
}

.knowledge-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

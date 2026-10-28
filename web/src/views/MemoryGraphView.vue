<template>
  <div class="memory-graph-view">
    <a-card :bordered="false">
      <div class="toolbar">
        <span class="muted">{{ t('memoryGraph.tip') }}</span>
        <a-space>
          <a-button @click="loadData">{{ t('memoryGraph.refresh') }}</a-button>
          <a-button type="primary" @click="modalVisible = true">{{ t('memoryGraph.add') }}</a-button>
        </a-space>
      </div>

      <a-list :data-source="statements" :loading="loading" :locale="{ emptyText: t('memoryGraph.empty') }">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-tag color="blue">{{ item }}</a-tag>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <a-modal v-model:open="modalVisible" :title="t('memoryGraph.add')" :confirm-loading="submitting" @ok="submit">
      <a-form layout="vertical">
        <a-form-item :label="t('memoryGraph.from')" required>
          <a-input v-model:value="form.fromName" :maxlength="100" :placeholder="t('memoryGraph.fromTip')" />
        </a-form-item>
        <a-form-item :label="t('memoryGraph.relation')" required>
          <a-input v-model:value="form.relation" :maxlength="50" :placeholder="t('memoryGraph.relationTip')" />
        </a-form-item>
        <a-form-item :label="t('memoryGraph.to')" required>
          <a-input v-model:value="form.toName" :maxlength="100" :placeholder="t('memoryGraph.toTip')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { addGraphRelation, queryGraphRelations } from '@/services/memoryGraph'

const { t } = useI18n()
const statements = ref<string[]>([])
const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const form = reactive({ fromName: '', relation: '', toName: '' })

async function loadData() {
  loading.value = true
  try {
    const response = await queryGraphRelations()
    statements.value = response.data || []
  } catch (error) {
    message.error(t('memoryGraph.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.fromName || !form.relation || !form.toName) {
    message.warning(t('memoryGraph.requiredTip'))
    return
  }
  submitting.value = true
  try {
    await addGraphRelation({ ...form })
    message.success(t('memoryGraph.saveSuccess'))
    modalVisible.value = false
    await loadData()
  } catch (error) {
    message.error(t('memoryGraph.saveFailed'))
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.memory-graph-view .toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.memory-graph-view .muted {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

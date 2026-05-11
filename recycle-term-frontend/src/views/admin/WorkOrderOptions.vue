<template>
  <div class="option-page">
    <el-row :gutter="14">
      <el-col :xs="24" :md="12">
        <option-card title="工单类型" category="TYPE" :items="typeItems" @refresh="fetchItems" />
      </el-col>
      <el-col :xs="24" :md="12">
        <option-card title="失败原因" category="FAILURE_REASON" :items="reasonItems" @refresh="fetchItems" />
      </el-col>
    </el-row>
    <el-card class="status-card" shadow="never">
      <template #header>状态说明</template>
      <el-table :data="statusItems" size="small" stripe>
        <el-table-column label="状态值" prop="value" width="150" />
        <el-table-column label="显示名称" prop="label" />
        <el-table-column label="说明">
          <template #default>状态流转规则由后端固定控制，避免误配置破坏流程。</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { ElButton, ElCard, ElInput, ElMessage, ElPopconfirm, ElSwitch, ElTable, ElTableColumn } from 'element-plus'
import { createAdminOption, deleteAdminOption, listAdminOptionItems, updateAdminOption } from '../../api/workOrder'
import type { WorkOrderOption } from '../../types'

const items = ref<WorkOrderOption[]>([])
const typeItems = computed(() => items.value.filter(item => item.category === 'TYPE'))
const reasonItems = computed(() => items.value.filter(item => item.category === 'FAILURE_REASON'))
const statusItems = computed(() => items.value.filter(item => item.category === 'STATUS'))

async function fetchItems() {
  try {
    const { data: res } = await listAdminOptionItems()
    items.value = res.data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '获取选项失败')
  }
}

const OptionCard = defineComponent({
  name: 'OptionCard',
  props: { title: { type: String, required: true }, category: { type: String, required: true }, items: { type: Array as () => WorkOrderOption[], required: true } },
  emits: ['refresh'],
  setup(props, { emit }) {
    const newName = ref('')
    const savingId = ref<number | null>(null)
    async function add() {
      if (!newName.value.trim()) { ElMessage.warning('请输入名称'); return }
      await createAdminOption({ category: props.category, value: newName.value.trim(), label: newName.value.trim(), enabled: true, sortOrder: props.items.length * 10 + 10 })
      newName.value = ''
      ElMessage.success('添加成功')
      emit('refresh')
    }
    async function save(row: WorkOrderOption) {
      savingId.value = row.id
      try {
        await updateAdminOption(row.id, { category: row.category, value: row.value, label: row.label, enabled: row.enabled, sortOrder: row.sortOrder })
        ElMessage.success('保存成功')
        emit('refresh')
      } finally { savingId.value = null }
    }
    async function remove(row: WorkOrderOption) {
      await deleteAdminOption(row.id)
      ElMessage.success('删除成功')
      emit('refresh')
    }
    return () => h(ElCard, { shadow: 'never', class: 'option-card' }, {
      header: () => h('div', { class: 'card-header' }, [h('span', props.title), h('div', { class: 'add-box' }, [h(ElInput, { modelValue: newName.value, 'onUpdate:modelValue': (v: string) => newName.value = v, placeholder: '新增名称', size: 'small', onKeydown: (e: Event | KeyboardEvent) => { if (e instanceof KeyboardEvent && e.key === 'Enter') add() } }), h(ElButton, { type: 'primary', size: 'small', onClick: add }, () => '添加')])]),
      default: () => h(ElTable, { data: props.items, size: 'small', stripe: true }, () => [
        h(ElTableColumn, { label: '名称' }, { default: ({ row }: { row: WorkOrderOption }) => h(ElInput, { modelValue: row.label, 'onUpdate:modelValue': (v: string) => { row.label = v; row.value = v }, size: 'small' }) }),
        h(ElTableColumn, { label: '启用', width: 80, align: 'center' }, { default: ({ row }: { row: WorkOrderOption }) => h(ElSwitch, { modelValue: row.enabled, 'onUpdate:modelValue': (v: string | number | boolean) => row.enabled = Boolean(v) }) }),
        h(ElTableColumn, { label: '操作', width: 130 }, { default: ({ row }: { row: WorkOrderOption }) => h('div', { class: 'row-actions' }, [h(ElButton, { size: 'small', type: 'primary', loading: savingId.value === row.id, onClick: () => save(row) }, () => '保存'), h(ElPopconfirm, { title: '确定删除？', onConfirm: () => remove(row) }, { reference: () => h(ElButton, { size: 'small', type: 'danger' }, () => '删除') })]) })
      ])
    })
  }
})

onMounted(fetchItems)
</script>

<style scoped>
.option-page { display: grid; gap: 14px; }
.status-card { margin-top: 14px; }
:deep(.card-header) { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
:deep(.add-box) { display: flex; gap: 8px; }
:deep(.row-actions) { display: flex; gap: 6px; }
@media (max-width: 768px) { :deep(.card-header) { align-items: stretch; flex-direction: column; } }
</style>

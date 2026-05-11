<template>
  <div class="quick-page">
    <el-page-header class="page-header" @back="router.push('/work-orders')">
      <template #content><span class="page-title">快速录入工单</span></template>
    </el-page-header>

    <el-card class="quick-card" shadow="never">
      <el-form label-position="top">
        <el-form-item label="粘贴工单短信/文本">
          <el-input v-model="rawText" type="textarea" :rows="8" placeholder="粘贴包含业务号码、客户、联系方式、装机地址、分光器、ONU SN号等内容的文本" />
        </el-form-item>
        <el-button type="primary" :loading="parsing" style="width:100%" @click="parseText">解析文本</el-button>
      </el-form>
    </el-card>

    <el-card v-if="form" class="quick-card" shadow="never">
      <template #header>核对后创建</template>
      <el-form label-position="top">
        <el-form-item label="工单号">
          <el-input v-model="form.workOrderNo" placeholder="不填则自动生成" />
        </el-form-item>
        <el-form-item label="产品号">
          <el-input v-model="form.productId" />
        </el-form-item>
        <el-form-item label="工单类型">
          <el-select v-model="form.workOrderType" style="width:100%" filterable>
            <el-option v-for="item in options.types" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.userName" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" type="textarea" :rows="2" />
        </el-form-item>
        <div class="two-col">
          <el-form-item label="CVLAN"><el-input v-model="form.cvlan" /></el-form-item>
          <el-form-item label="SVLAN"><el-input v-model="form.svlan" /></el-form-item>
        </div>
        <el-form-item label="分光器">
          <el-input v-model="form.splitter" />
        </el-form-item>
        <el-form-item label="分光器端口">
          <el-input v-model="form.splitterPort" />
        </el-form-item>
        <el-form-item label="SN">
          <el-input v-model="form.onuSn" placeholder="没有设备可以留空" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div class="actions">
        <el-button @click="form = null">重新粘贴</el-button>
        <el-button type="success" :loading="submitting" @click="submit">创建工单</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createWorkOrder, getWorkOrderOptions, quickParseWorkOrder } from '../../api/workOrder'
import type { WorkOrder, WorkOrderOptions } from '../../types'

const router = useRouter()
const rawText = ref('')
const parsing = ref(false)
const submitting = ref(false)
const form = ref<Partial<WorkOrder> | null>(null)
const options = ref<WorkOrderOptions>({ types: [], failureReasons: [], statuses: [] })

async function fetchOptions() {
  try {
    const { data: res } = await getWorkOrderOptions()
    options.value = res.data
  } catch {
    options.value = { types: [], failureReasons: [], statuses: [] }
  }
}

async function parseText() {
  if (!rawText.value.trim()) {
    ElMessage.warning('请先粘贴文本')
    return
  }
  parsing.value = true
  try {
    const { data: res } = await quickParseWorkOrder({ text: rawText.value })
    form.value = { ...res.data, status: 'ACCEPTED', rawSource: rawText.value }
    ElMessage.success('解析完成，请核对信息')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '解析失败')
  } finally {
    parsing.value = false
  }
}

async function submit() {
  if (!form.value) return
  submitting.value = true
  try {
    const { data: res } = await createWorkOrder(form.value)
    ElMessage.success('工单已创建')
    router.replace(`/work-orders/${res.data.id}`)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchOptions)
</script>

<style scoped>
.quick-page { max-width: 760px; margin: 0 auto; padding: 14px; min-height: 100vh; background: #f5f7fa; }
.page-header { margin-bottom: 12px; }
.page-title { font-weight: 700; }
.quick-card { margin-bottom: 12px; border-radius: 14px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.actions { display: flex; justify-content: flex-end; gap: 10px; }
@media (max-width: 768px) { .two-col { grid-template-columns: 1fr; gap: 0; } .actions .el-button { flex: 1; } }
</style>

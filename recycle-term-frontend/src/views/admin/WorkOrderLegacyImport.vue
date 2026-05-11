<template>
  <el-card class="legacy-import" shadow="never">
    <template #header>
      <div class="card-header">
        <span>仓库历史数据导入工单</span>
        <div>
          <el-button :loading="previewing" @click="fetchPreview">刷新预览</el-button>
          <el-button type="success" :loading="importing" :disabled="!preview || preview.importable === 0" @click="executeImport">执行导入</el-button>
        </div>
      </div>
    </template>

    <el-alert title="这里只复制仓库历史记录中的产品号、用户、联系方式、地址、分光器、SN 等信息到工单系统，不会绑定设备，也不会创建或修改仓库设备。" type="info" :closable="false" show-icon />

    <el-row v-if="preview" :gutter="14" class="stats">
      <el-col :xs="24" :sm="8"><el-statistic title="仓库记录总数" :value="preview.total" /></el-col>
      <el-col :xs="24" :sm="8"><el-statistic title="可导入" :value="preview.importable" /></el-col>
      <el-col :xs="24" :sm="8"><el-statistic title="已存在/跳过" :value="preview.existing" /></el-col>
    </el-row>

    <el-table v-if="preview" :data="preview.samples" stripe style="margin-top:16px">
      <el-table-column label="产品号" prop="productId" width="130" />
      <el-table-column label="用户" prop="userName" width="100" />
      <el-table-column label="电话" prop="contactPhone" width="120" />
      <el-table-column label="分光器" prop="splitter" width="140" show-overflow-tooltip />
      <el-table-column label="SN" prop="onuSn" width="150" show-overflow-tooltip />
      <el-table-column label="地址" prop="address" min-width="220" show-overflow-tooltip />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { executeLegacyImport, previewLegacyImport } from '../../api/workOrder'
import type { WorkOrderLegacyImportPreview } from '../../types'

const preview = ref<WorkOrderLegacyImportPreview | null>(null)
const previewing = ref(false)
const importing = ref(false)

async function fetchPreview() {
  previewing.value = true
  try {
    const { data: res } = await previewLegacyImport(20)
    preview.value = res.data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '预览失败')
  } finally {
    previewing.value = false
  }
}

async function executeImport() {
  if (!preview.value || preview.value.importable === 0) return
  try {
    await ElMessageBox.confirm(`确定导入 ${preview.value.importable} 条仓库历史记录到工单系统？`, '确认导入', { type: 'warning' })
    importing.value = true
    const { data: res } = await executeLegacyImport()
    ElMessage.success(`导入完成：新增 ${res.data.created} 条，跳过 ${res.data.skipped} 条`)
    fetchPreview()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

onMounted(fetchPreview)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.stats { margin-top: 18px; }
:deep(.el-statistic) { background: #f8fafc; border-radius: 10px; padding: 16px; }
</style>

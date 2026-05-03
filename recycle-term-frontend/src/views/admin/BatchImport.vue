<template>
  <div class="batch-import">
    <div class="import-wrapper">
      <!-- Step 1: Upload -->
      <el-card shadow="never">
        <template #header><span>Excel 导入</span></template>
        <el-upload
          drag
          :show-file-list="false"
          :before-upload="handleUpload"
          accept=".xlsx,.xls"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 .xlsx / .xls 格式，先预览再确认导入</div>
          </template>
        </el-upload>
      </el-card>

      <!-- Step 2: Preview -->
      <el-card v-if="previewData.length > 0" shadow="never">
        <template #header>
          <div class="card-header">
            <span>数据预览（{{ previewData.length }} 条）</span>
            <el-button type="danger" size="small" @click="previewData = []; file = null">放弃</el-button>
          </div>
        </template>
        <el-table :data="previewData.slice(0, 100)" stripe size="small" max-height="400" class="preview-table">
          <el-table-column label="#" type="index" width="40" />
          <el-table-column label="产品号" prop="productId" width="120" show-overflow-tooltip />
          <el-table-column label="用户名称" prop="userName" width="80" />
          <el-table-column label="用户号码" prop="phoneNumber" width="120" show-overflow-tooltip />
          <el-table-column label="区域" prop="area" width="70" />
          <el-table-column label="地址" prop="userAddress" show-overflow-tooltip min-width="160" />
          <el-table-column label="应回收" prop="expectedCount" width="70" align="center" />
          <el-table-column label="工程师" width="90">
            <template #default="{ row }">{{ row.engineerName }}</template>
          </el-table-column>
          <el-table-column label="上门" width="60" align="center">
            <template #default="{ row }">{{ row.needVisit ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
        <div v-if="previewData.length > 100" class="preview-tip">仅显示前 100 条，共 {{ previewData.length }} 条</div>
        <div style="margin-top:16px; text-align:right">
          <el-button type="primary" :loading="importing" @click="handleImport" size="large">
            确认导入 {{ previewData.length }} 条
          </el-button>
        </div>
      </el-card>

      <!-- JSON import -->
      <el-card shadow="never">
        <template #header><span>JSON 批量导入</span></template>
        <el-alert title="每个任务需包含 userName、phoneNumber(用户号码) 和 productId(产品号) 字段" type="info" :closable="false" style="margin-bottom:12px" />
        <el-input v-model="jsonInput" type="textarea" :rows="8" placeholder='[
  { "userName": "张三", "phoneNumber": "13800138000", "productId": "02001559573", "area": "南沙", "userAddress": "地址", "expectedCount": 5 }
]' />
        <div style="margin-top:12px; display:flex; gap:12px">
          <el-button type="primary" :loading="jsonLoading" @click="handleJsonImport">提交导入</el-button>
          <el-button @click="fillSample">填入示例</el-button>
          <el-button @click="jsonInput = ''">清空</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { batchCreateTasks } from '../../api/admin'
import { importExcel, previewExcel } from '../../api'
import type { RecycleTask } from '../../types'

const jsonInput = ref('')
const jsonLoading = ref(false)
const previewData = ref<RecycleTask[]>([])
const importing = ref(false)
const file = ref<File | null>(null)

async function handleUpload(f: File) {
  file.value = f
  try {
    const { data: res } = await previewExcel(f)
    previewData.value = res.data
    ElMessage.success(`解析成功，共 ${res.data.length} 条数据，请确认后导入`)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '解析失败')
  }
  return false
}

async function handleImport() {
  if (!file.value) return
  importing.value = true
  try {
    const { data: res } = await importExcel(file.value)
    ElMessage.success(res.message)
    previewData.value = []
    file.value = null
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

async function handleJsonImport() {
  if (!jsonInput.value.trim()) {
    ElMessage.warning('请输入 JSON 数据')
    return
  }
  let data: any[]
  try {
    data = JSON.parse(jsonInput.value)
    if (!Array.isArray(data)) throw new Error()
  } catch {
    ElMessage.error('JSON 格式不正确，需要是数组')
    return
  }
  jsonLoading.value = true
  try {
    const { data: res } = await batchCreateTasks(data)
    ElMessage.success(res.message)
    jsonInput.value = ''
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '导入失败')
  } finally {
    jsonLoading.value = false
  }
}

function fillSample() {
  jsonInput.value = JSON.stringify([
    { userName: '张三', phoneNumber: '13800138000', productId: '02001559573', userAddress: '广州市南沙区黄阁镇xxx', area: '南沙', expectedCount: 5, engineerName: '王工', engineerPhone: '13700137000' },
    { userName: '李四', phoneNumber: '13900139000', productId: '02001830568', userAddress: '广州市番禺区市桥xxx', area: '番禺', expectedCount: 3, engineerName: '赵工', engineerPhone: '13600136000' },
  ], null, 2)
}
</script>

<style scoped>
.import-wrapper {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.import-wrapper .el-card {
  width: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.preview-table :deep(th) { background: #f5f7fa; }
.preview-tip {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 8px 0;
}
</style>

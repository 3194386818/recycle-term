<template>
  <div class="batch-import">
    <div class="import-wrapper">
      <el-card shadow="never">
        <template #header><span>Excel 导入</span></template>
        <el-upload
          drag
          :show-file-list="false"
          :before-upload="handleExcelImport"
          accept=".xlsx,.xls"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 .xlsx / .xls 格式，按模板格式填写</div>
          </template>
        </el-upload>
      </el-card>

      <el-card shadow="never">
        <template #header><span>JSON 批量导入</span></template>
        <el-alert title="每个任务需包含 userName 和 phoneNumber 字段" type="info" :closable="false" style="margin-bottom:12px" />
        <el-input v-model="jsonInput" type="textarea" :rows="10" placeholder='[
  { "userName": "张三", "phoneNumber": "13800138000", "userAddress": "地址", "area": "南沙", "expectedCount": 5 },
  { "userName": "李四", "phoneNumber": "13900139000", "userAddress": "地址", "area": "番禺", "expectedCount": 3 }
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
import { importExcel } from '../../api'

const jsonInput = ref('')
const jsonLoading = ref(false)

async function handleExcelImport(file: File) {
  try {
    const { data: res } = await importExcel(file)
    ElMessage.success(res.message)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || 'Excel 导入失败')
  }
  return false
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
    { userName: '张三', phoneNumber: '13800138000', userAddress: '广州市南沙区黄阁镇xxx', area: '南沙', expectedCount: 5, engineerName: '王工', engineerPhone: '13700137000' },
    { userName: '李四', phoneNumber: '13900139000', userAddress: '广州市番禺区市桥xxx', area: '番禺', expectedCount: 3, engineerName: '赵工', engineerPhone: '13600136000' },
  ], null, 2)
}
</script>

<style scoped>
.import-wrapper {
  max-width: 700px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.import-wrapper .el-card {
  width: 100%;
}
</style>

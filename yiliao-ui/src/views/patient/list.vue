<template>
  <div class="patient-list">
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="姓名 / 患者编号"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-select v-model="query.stageLabel" placeholder="阶段标签" clearable style="width: 160px">
          <el-option v-for="s in stageLabels" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe empty-text="暂无患者档案">
        <el-table-column prop="patientNo" label="患者编号" width="140" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="阶段" width="120">
          <template #default="{ row }">
            <el-tag :type="stageTagType(row.stageLabel)">{{ row.stageLabel || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="100">
          <template #default="{ row }">{{ sourceText(row.sourceType) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="建档时间" min-width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { pageArchives } from '../../api/patient'

const router = useRouter()

const stageLabels = ['结节随访中', '术后随访中', '治疗中', 'MDT中', '失访', '结案']

const query = reactive({ page: 1, size: 10, keyword: '', stageLabel: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

const genderText = (g) => (g === 1 ? '男' : g === 2 ? '女' : '-')
const sourceText = (s) => ({ 1: '体检', 2: '门诊', 3: '住院' }[s] || '-')
const stageTagType = (label) => {
  if (label === '失访') return 'danger'
  if (label === 'MDT中') return 'warning'
  if (label === '结案') return 'info'
  return 'primary'
}

async function load() {
  loading.value = true
  try {
    const data = await pageArchives({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      stageLabel: query.stageLabel || undefined
    })
    rows.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    // 业务错误已由拦截器弹提示，这里兜底清空避免展示脏数据
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onReset() {
  query.keyword = ''
  query.stageLabel = ''
  query.page = 1
  load()
}

function onSizeChange() {
  query.page = 1
  load()
}

function goDetail(row) {
  router.push('/patient/detail/' + row.id)
}

onMounted(load)
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

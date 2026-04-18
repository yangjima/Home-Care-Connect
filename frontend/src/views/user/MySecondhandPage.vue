<template>
  <div class="page">
    <UserSubpageHeader title="跳蚤市场发布" subtitle="我的发布记录" />
    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="secondhandStatusType(row.status)" size="small">
              {{ secondhandStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/secondhand/${row.id}`)">查看</el-button>
            <el-button
              v-if="String(row.status) === '0'"
              type="warning"
              link
              @click="resubmit(row.id)"
            >
              重新提交审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          layout="prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import UserSubpageHeader from '@/components/user/UserSubpageHeader.vue'
import { getMySecondhandItems, submitSecondhandListing } from '@/api/asset'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const rows = ref<Record<string, unknown>[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

function secondhandStatusLabel(status: unknown) {
  const s = String(status ?? '')
  if (s === '1') return '在售'
  if (s === '2') return '待审核'
  if (s === '0') return '下架/已驳回'
  return s || '—'
}

function secondhandStatusType(status: unknown) {
  const s = String(status ?? '')
  if (s === '1') return 'success'
  if (s === '2') return 'warning'
  if (s === '0') return 'info'
  return 'info'
}

async function resubmit(id: number) {
  try {
    await submitSecondhandListing(id)
    ElMessage.success('已重新提交上架审核')
    await load()
  } catch {
    ElMessage.error('提交失败')
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getMySecondhandItems({ page: page.value, size: size.value })
    const data = res as { records?: unknown[]; list?: unknown[]; total?: number }
    rows.value = (data.records || data.list || []) as Record<string, unknown>[]
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
/* header is unified by UserSubpageHeader */
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>

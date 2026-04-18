<template>
  <div class="page">
    <UserSubpageHeader title="我的看房" subtitle="预约记录" />
    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="propertyTitle" label="房源" min-width="160" />
        <el-table-column prop="viewingTime" label="预约时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(String(row.status))" size="small">
              {{ statusText(String(row.status)) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userRemark" label="备注" min-width="120" show-overflow-tooltip />
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
import { getMyViewings } from '@/api/property'

const loading = ref(false)
const rows = ref<Record<string, unknown>[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

function statusText(s: string) {
  const m: Record<string, string> = {
    pending: '待确认',
    confirmed: '已确认',
    cancelled: '已取消',
    completed: '已完成',
  }
  return m[s] || s
}

function statusType(s: string) {
  const m: Record<string, 'warning' | 'success' | 'info' | 'danger'> = {
    pending: 'warning',
    confirmed: 'success',
    completed: 'success',
    cancelled: 'info',
  }
  return m[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getMyViewings({ page: page.value, size: size.value })
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

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
import { onMounted } from 'vue'
import UserSubpageHeader from '@/components/user/UserSubpageHeader.vue'
import { getMyViewings } from '@/api/property'
import { viewingStatusLabel, viewingStatusType } from '@/utils/status'
import { useResourceList } from '@/composables/useResourceList'

const { loading, rows, total, page, size, load } = useResourceList<Record<string, unknown>>((params) =>
  getMyViewings(params),
)

function statusText(s: string) {
  return viewingStatusLabel(s)
}

function statusType(s: string) {
  return viewingStatusType(s)
}

onMounted(() => {
  void load()
})
</script>

<style scoped lang="scss">
/* header is unified by UserSubpageHeader */
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>

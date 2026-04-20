<template>
  <div class="content">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">➕ 新建门店</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="门店名称" min-width="160" />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="managerId" label="店长用户ID" width="120">
          <template #default="{ row }">{{ row.managerId ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑门店' : '新建门店'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="门店名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="详细地址" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormRules } from 'element-plus'
import { useAdminList } from '@/composables/useAdminList'
import { useAdminForm } from '@/composables/useAdminForm'
import { createStore, listStores, updateStore, type Store } from '@/api/stores'

const { loading, rows, load } = useAdminList<Store>(async () => {
  try {
    return (await listStores()) || []
  } catch {
    return []
  }
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
}

const {
  visible: dialogVisible,
  loading: saving,
  formRef,
  form,
  open: openCreate,
  openWith,
  submit,
} = useAdminForm({
  initial: () => ({ id: 0, name: '', address: '', phone: '' }),
  onSuccess: () => load(),
  onSubmit: async (f) => {
    const payload = {
      name: f.name.trim(),
      address: f.address.trim(),
      phone: f.phone.trim() || undefined,
    }
    if (f.id) {
      await updateStore(f.id, payload)
    } else {
      await createStore(payload)
    }
  },
})

function openEdit(row: Store) {
  openWith({
    id: row.id,
    name: row.name || '',
    address: row.address || '',
    phone: row.phone || '',
  })
}

onMounted(() => {
  void load()
})
</script>

<style scoped lang="scss">
.content {
  padding: 25px 30px;
}
.toolbar {
  margin-bottom: 16px;
}
.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
</style>

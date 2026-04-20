import { reactive, ref } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'

type AnyObject = Record<string, unknown>

type UseAdminFormOptions<TForm extends AnyObject> = {
  /** 表单初始值（每次 open 时会被 reset 为这个） */
  initial: () => TForm
  /** 保存回调，返回 Promise。收到的是当前 form 拷贝 */
  onSubmit: (form: TForm) => Promise<void>
  /** 保存成功提示，默认"操作成功" */
  successMessage?: string
  /** 保存成功后的回调（通常用于刷新列表） */
  onSuccess?: () => void | Promise<void>
  /** 是否在保存成功后自动关闭对话框，默认 true */
  autoClose?: boolean
}

/**
 * 通用 admin 表单 composable：封装 open/close/validate/submit/loading
 * - open() 重置表单并打开对话框
 * - openWith(row) 用 row 回填表单并打开（editing）
 * - submit() 校验并保存；成功后默认关闭并 onSuccess
 */
export function useAdminForm<TForm extends AnyObject>(options: UseAdminFormOptions<TForm>) {
  const visible = ref(false)
  const loading = ref(false)
  const formRef = ref<FormInstance>()
  const form = reactive(options.initial()) as TForm

  function reset(next?: Partial<TForm>) {
    const base = options.initial()
    const keys = new Set([...Object.keys(form), ...Object.keys(base)])
    keys.forEach((key) => {
      ;(form as AnyObject)[key] = (next && key in next ? (next as AnyObject)[key] : (base as AnyObject)[key]) as unknown
    })
    // 清理校验
    formRef.value?.clearValidate?.()
  }

  function open() {
    reset()
    visible.value = true
  }

  function openWith(row: Partial<TForm>) {
    reset(row)
    visible.value = true
  }

  function close() {
    visible.value = false
  }

  async function submit() {
    if (formRef.value) {
      const valid = await formRef.value.validate().catch(() => false)
      if (!valid) return false
    }
    loading.value = true
    try {
      await options.onSubmit({ ...(form as AnyObject) } as TForm)
      ElMessage.success(options.successMessage ?? '操作成功')
      if (options.autoClose !== false) visible.value = false
      await options.onSuccess?.()
      return true
    } finally {
      loading.value = false
    }
  }

  return {
    visible,
    loading,
    formRef,
    form,
    open,
    openWith,
    close,
    submit,
    reset,
  }
}

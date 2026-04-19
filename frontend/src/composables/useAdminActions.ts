import { ElMessage, ElMessageBox } from 'element-plus'

type AsyncAction = () => Promise<void>
type ConfirmButtonType = 'warning' | 'success' | 'info' | 'primary' | 'danger'

export function useAdminActions() {
  async function runAction(action: AsyncAction, successMessage: string) {
    await action()
    ElMessage.success(successMessage)
  }

  async function runConfirmAction(options: {
    confirmMessage: string
    confirmTitle?: string
    confirmButtonText?: string
    cancelButtonText?: string
    type?: ConfirmButtonType
    action: AsyncAction
    successMessage: string
  }) {
    await ElMessageBox.confirm(options.confirmMessage, options.confirmTitle || '提示', {
      type: options.type || 'warning',
      confirmButtonText: options.confirmButtonText,
      cancelButtonText: options.cancelButtonText,
    })
    await runAction(options.action, options.successMessage)
  }

  async function runConfirmActionSafe(options: Parameters<typeof runConfirmAction>[0]) {
    try {
      await runConfirmAction(options)
    } catch {
      // user cancelled or request failed (toast handled by interceptor)
    }
  }

  async function runPromptAction(options: {
    promptMessage: string
    promptTitle?: string
    inputType?: 'text' | 'textarea' | 'password'
    inputValue?: string
    confirmButtonText?: string
    cancelButtonText?: string
    action: (value: string) => Promise<void>
    successMessage: string
  }) {
    const { value } = await ElMessageBox.prompt(options.promptMessage, options.promptTitle || '提示', {
      confirmButtonText: options.confirmButtonText,
      cancelButtonText: options.cancelButtonText,
      inputType: options.inputType,
      inputValue: options.inputValue,
    })
    await options.action(String(value || ''))
    ElMessage.success(options.successMessage)
  }

  return {
    runAction,
    runConfirmAction,
    runConfirmActionSafe,
    runPromptAction,
  }
}

import { ElMessage, ElMessageBox } from 'element-plus'

type AsyncAction = () => Promise<void>

export function useAdminActions() {
  async function runAction(action: AsyncAction, successMessage: string) {
    await action()
    ElMessage.success(successMessage)
  }

  async function runConfirmAction(options: {
    confirmMessage: string
    confirmTitle?: string
    action: AsyncAction
    successMessage: string
  }) {
    await ElMessageBox.confirm(options.confirmMessage, options.confirmTitle || '提示', { type: 'warning' })
    await runAction(options.action, options.successMessage)
  }

  return {
    runAction,
    runConfirmAction,
  }
}

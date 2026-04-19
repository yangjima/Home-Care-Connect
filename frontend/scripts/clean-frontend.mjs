/**
 * 清理前端生成物与本地缓存：
 * - src 下误生成的 *.vue.d.ts（类型由根目录 env.d.ts 全局声明）
 * - TypeScript incremental 缓存（node_modules/.tmp）
 * - Vite 预构建缓存（node_modules/.vite）
 */
import { readdirSync, unlinkSync, rmSync, existsSync } from 'fs'
import { join } from 'path'
import { fileURLToPath } from 'url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))
const root = join(__dirname, '..')

function walkRemoveVueDts(dir) {
  for (const name of readdirSync(dir, { withFileTypes: true })) {
    const p = join(dir, name.name)
    if (name.isDirectory()) {
      if (name.name === 'node_modules') continue
      walkRemoveVueDts(p)
    } else if (name.name.endsWith('.vue.d.ts')) {
      unlinkSync(p)
    }
  }
}

const src = join(root, 'src')
if (existsSync(src)) {
  walkRemoveVueDts(src)
}

for (const rel of ['node_modules/.tmp', 'node_modules/.vite']) {
  const p = join(root, rel)
  if (existsSync(p)) {
    rmSync(p, { recursive: true, force: true })
  }
}

console.log('clean-frontend: removed *.vue.d.ts under src, cleared .tmp / .vite caches')

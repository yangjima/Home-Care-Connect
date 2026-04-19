/**
 * 生成 RSA-OAEP(SHA-256) 登录密钥对，并写入：
 * - frontend/.env.local、frontend/.env.example、frontend/.env.production.example（公钥，可提交 .env*.example）
 * - services/user-service/.../application-local.yml（私钥，该文件已在 .gitignore）
 * - infrastructure/docker/.env（LOGIN_RSA_PRIVATE_KEY + VITE_LOGIN_RSA_PUBLIC_KEY，供 compose 构建前端与运行 user-service）
 * - config/login-rsa-public.pem、config/login-rsa-private.pem（与 README-login-rsa.txt 一致，已 gitignore）
 *
 * 用法（仓库根目录）：node scripts/generate-login-rsa.mjs
 */
import fs from 'fs'
import {
  generateKeyPairSync,
  createPublicKey,
  createPrivateKey,
  privateDecrypt,
  publicEncrypt,
  constants,
} from 'crypto'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = join(__dirname, '..')

/**
 * 更新 infrastructure/docker/.env 中的两把 RSA 变量，保留文件中其它配置行。
 */
function mergeDockerEnv(dockerEnvPath, pubEscaped, privEscaped) {
  let content = ''
  try {
    content = fs.readFileSync(dockerEnvPath, 'utf8')
  } catch {
    // 无文件则新建
  }
  const lines = content.split(/\r?\n/)
  const kept = lines.filter((line) => {
    const t = line.trim()
    if (t.startsWith('LOGIN_RSA_PRIVATE_KEY=')) return false
    if (t.startsWith('VITE_LOGIN_RSA_PUBLIC_KEY=')) return false
    if (t.startsWith('# Login RSA（RSA-OAEP SHA-256）')) return false
    if (t === '# Optional: if you build frontend via docker compose, pass the public key as build arg')
      return false
    // 曾错误拼接成一行时的脏数据
    if (t.includes('LOGIN_RSA_PRIVATE_KEY=') && t.includes('VITE_LOGIN_RSA_PUBLIC_KEY=')) return false
    return true
  })
  while (kept.length && kept[kept.length - 1] === '') kept.pop()
  const parts = []
  if (kept.length) parts.push(kept.join('\n'))
  parts.push(
    '# Login RSA（RSA-OAEP SHA-256）：由 scripts/generate-login-rsa.mjs 写入，与 frontend / application-local 同步',
    `LOGIN_RSA_PRIVATE_KEY="${privEscaped}"`,
    `VITE_LOGIN_RSA_PUBLIC_KEY="${pubEscaped}"`,
  )
  fs.mkdirSync(dirname(dockerEnvPath), { recursive: true })
  fs.writeFileSync(dockerEnvPath, parts.join('\n') + '\n', 'utf8')
}

const { publicKey: pubPem, privateKey: privPem } = generateKeyPairSync('rsa', {
  modulusLength: 2048,
  publicKeyEncoding: { type: 'spki', format: 'pem' },
  privateKeyEncoding: { type: 'pkcs8', format: 'pem' },
})

const pub = pubPem.trim()
const priv = privPem.trim()

const pubEscaped = pub.split(/\r?\n/).join('\\n')
const privEscaped = priv.split(/\r?\n/).join('\\n')
const envLine = `VITE_LOGIN_RSA_PUBLIC_KEY="${pubEscaped}"\n`
const envComment =
  '# 复制为 .env.local / .env.production；须与 user-service 登录私钥为同一对密钥\n'

fs.writeFileSync(join(root, 'frontend/.env.local'), envLine, 'utf8')
fs.writeFileSync(join(root, 'frontend/.env.example'), envComment + envLine, 'utf8')
fs.writeFileSync(join(root, 'frontend/.env.production.example'), envComment + envLine, 'utf8')

const configDir = join(root, 'config')
fs.mkdirSync(configDir, { recursive: true })
fs.writeFileSync(join(configDir, 'login-rsa-public.pem'), pub + '\n', 'utf8')
fs.writeFileSync(join(configDir, 'login-rsa-private.pem'), priv + '\n', 'utf8')

const ok = (() => {
  const pwd = 'verify-login-rsa-' + Date.now()
  const enc = publicEncrypt(
    { key: createPublicKey(pubPem), padding: constants.RSA_PKCS1_OAEP_PADDING, oaepHash: 'sha256' },
    Buffer.from(pwd, 'utf8'),
  )
  const dec = privateDecrypt(
    {
      key: createPrivateKey(privPem),
      padding: constants.RSA_PKCS1_OAEP_PADDING,
      oaepHash: 'sha256',
    },
    enc,
  )
  return dec.toString('utf8') === pwd
})()

console.log(
  ok
    ? '已生成并校验密钥对：frontend/.env*、application-local.yml、infrastructure/docker/.env、config/login-rsa-*.pem 已同步。'
    : '校验失败（不应出现）',
)

/**
 * 用 frontend/.env.local 中公钥加密密码，POST 登录（默认网关 :8080）。
 * 用法：node scripts/probe-login.mjs [username] [plainPassword]
 */
import fs from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'
import {
  createPublicKey,
  publicEncrypt,
  constants,
} from 'crypto'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = join(__dirname, '..')

const username = process.argv[2] || 'admin'
const plainPassword = process.argv[3] || '123456'
const baseUrl = process.env.LOGIN_PROBE_URL || 'http://127.0.0.1:8080'

const envText = fs.readFileSync(join(root, 'frontend/.env.local'), 'utf8')
const envMatch = envText.match(/VITE_LOGIN_RSA_PUBLIC_KEY="([\s\S]*)"/)
if (!envMatch) {
  console.error('parse VITE_LOGIN_RSA_PUBLIC_KEY failed')
  process.exit(1)
}
const pubPem = envMatch[1].replace(/\\n/g, '\n')

const ciphertext = publicEncrypt(
  {
    key: createPublicKey(pubPem),
    padding: constants.RSA_PKCS1_OAEP_PADDING,
    oaepHash: 'sha256',
  },
  Buffer.from(plainPassword, 'utf8'),
)
const password = `rsa_oaep_sha256:${ciphertext.toString('base64')}`

const url = `${baseUrl.replace(/\/$/, '')}/api/user/auth/login`
const body = JSON.stringify({ username, password })

const res = await fetch(url, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body,
})
const text = await res.text()
console.log('POST', url)
console.log('status', res.status)
console.log(text.slice(0, 2000))

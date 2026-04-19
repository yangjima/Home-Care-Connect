/**
 * 校验 frontend/.env.local 中公钥与 application-local.yml 中私钥是否为同一对（RSA-OAEP SHA-256）。
 * 用法：node scripts/verify-login-rsa-pair.mjs
 */
import fs from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'
import {
  createPublicKey,
  createPrivateKey,
  publicEncrypt,
  privateDecrypt,
  constants,
} from 'crypto'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = join(__dirname, '..')

const envText = fs.readFileSync(join(root, 'frontend/.env.local'), 'utf8')
const envMatch = envText.match(/VITE_LOGIN_RSA_PUBLIC_KEY="([\s\S]*)"/)
if (!envMatch) {
  console.error('无法在 frontend/.env.local 中解析 VITE_LOGIN_RSA_PUBLIC_KEY')
  process.exit(1)
}
const pubPem = envMatch[1].replace(/\\n/g, '\n')

const yml = fs.readFileSync(
  join(root, 'services/user-service/src/main/resources/application-local.yml'),
  'utf8',
)
const block = yml.match(
  /login-rsa:\s*\n\s*private-key:\s*\|\s*\n([\s\S]*?)(?=\n\s{2}[a-z#]|\napp:|\n$)/,
)
if (!block) {
  console.error('无法在 application-local.yml 中解析 app.security.login-rsa.private-key')
  process.exit(1)
}
const privPem = block[1]
  .split('\n')
  .map((l) => l.replace(/^\s+/, ''))
  .join('\n')
  .trim()

const pwd = `probe-${Date.now()}`
const ciphertext = publicEncrypt(
  {
    key: createPublicKey(pubPem),
    padding: constants.RSA_PKCS1_OAEP_PADDING,
    oaepHash: 'sha256',
  },
  Buffer.from(pwd, 'utf8'),
)
const plain = privateDecrypt(
  {
    key: createPrivateKey(privPem),
    padding: constants.RSA_PKCS1_OAEP_PADDING,
    oaepHash: 'sha256',
  },
  ciphertext,
)

let ok = plain.toString('utf8') === pwd

let dockerOk = true
try {
  const dockerEnv = fs.readFileSync(join(root, 'infrastructure/docker/.env'), 'utf8')
  const dp = dockerEnv.match(/LOGIN_RSA_PRIVATE_KEY="([\s\S]*?)"/)
  const dpub = dockerEnv.match(/VITE_LOGIN_RSA_PUBLIC_KEY="([\s\S]*?)"/)
  if (dp && dpub) {
    const dPrivPem = dp[1].replace(/\\n/g, '\n')
    const dPubPem = dpub[1].replace(/\\n/g, '\n')
    const t = `d-${Date.now()}`
    const c = publicEncrypt(
      {
        key: createPublicKey(dPubPem),
        padding: constants.RSA_PKCS1_OAEP_PADDING,
        oaepHash: 'sha256',
      },
      Buffer.from(t, 'utf8'),
    )
    const p = privateDecrypt(
      {
        key: createPrivateKey(dPrivPem),
        padding: constants.RSA_PKCS1_OAEP_PADDING,
        oaepHash: 'sha256',
      },
      c,
    )
    dockerOk = p.toString('utf8') === t
  }
} catch {
  dockerOk = false
}

console.log(
  ok && dockerOk
    ? 'OK: .env.local ↔ application-local.yml ↔ infrastructure/docker/.env 三处密钥一致'
    : `FAIL: local+yml=${ok} docker.env=${dockerOk}`,
)
process.exit(ok && dockerOk ? 0 : 1)

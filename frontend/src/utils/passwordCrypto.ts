function pemToArrayBuffer(pem: string) {
  const b64 = pem
    .replace(/-----BEGIN PUBLIC KEY-----/g, '')
    .replace(/-----END PUBLIC KEY-----/g, '')
    .replace(/\s+/g, '')
  const binary = atob(b64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes.buffer
}

function toBase64(bytes: ArrayBuffer) {
  const u8 = new Uint8Array(bytes)
  let binary = ''
  for (let i = 0; i < u8.length; i++) binary += String.fromCharCode(u8[i])
  return btoa(binary)
}

export const LOGIN_PASSWORD_PREFIX = 'rsa_oaep_sha256:'

async function encryptWithWebCrypto(password: string, pubPem: string): Promise<string> {
  const keyData = pemToArrayBuffer(pubPem)
  const publicKey = await crypto.subtle.importKey(
    'spki',
    keyData,
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    false,
    ['encrypt'],
  )
  const plaintext = new TextEncoder().encode(password)
  const ciphertext = await crypto.subtle.encrypt({ name: 'RSA-OAEP' }, publicKey, plaintext)
  return `${LOGIN_PASSWORD_PREFIX}${toBase64(ciphertext)}`
}

/** forge 返回二进制字符串，转为与 Web Crypto 相同的 Base64，避免与 Java Base64 解码不一致 */
function forgeBinaryToBase64(binaryStr: string): string {
  const u8 = new Uint8Array(binaryStr.length)
  for (let i = 0; i < binaryStr.length; i++) u8[i] = binaryStr.charCodeAt(i) & 0xff
  return toBase64(u8.buffer)
}

/** 与 Java RSA/ECB/OAEPWithSHA-256AndMGF1Padding、Web Crypto RSA-OAEP(SHA-256) 对齐；用于 HTTP 等非安全上下文无 crypto.subtle 时 */
async function encryptWithForge(password: string, pubPem: string): Promise<string> {
  const forgeModule = await import('node-forge')
  const forge = forgeModule.default ?? forgeModule
  const publicKey = forge.pki.publicKeyFromPem(pubPem)
  const encrypted = publicKey.encrypt(forge.util.encodeUtf8(password), 'RSA-OAEP', {
    md: forge.md.sha256.create(),
    mgf1: { md: forge.md.sha256.create() },
  })
  return `${LOGIN_PASSWORD_PREFIX}${forgeBinaryToBase64(encrypted)}`
}

export async function encryptLoginPassword(password: string): Promise<string> {
  const pubRaw = (import.meta.env.VITE_LOGIN_RSA_PUBLIC_KEY as string | undefined) || ''
  const pub = pubRaw.replace(/\\n/g, '\n')
  if (!pub.trim()) {
    throw new Error('未配置登录加密公钥（VITE_LOGIN_RSA_PUBLIC_KEY）')
  }

  const canUseSubtle = typeof globalThis.crypto !== 'undefined' && !!globalThis.crypto.subtle
  if (canUseSubtle) {
    return encryptWithWebCrypto(password, pub)
  }
  return encryptWithForge(password, pub)
}

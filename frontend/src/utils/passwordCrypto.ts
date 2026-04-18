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

export async function encryptLoginPassword(password: string): Promise<string> {
  const pubRaw = (import.meta.env.VITE_LOGIN_RSA_PUBLIC_KEY as string | undefined) || ''
  const pub = pubRaw.replace(/\\n/g, '\n')
  if (!pub.trim()) {
    throw new Error('未配置登录加密公钥（VITE_LOGIN_RSA_PUBLIC_KEY）')
  }

  if (!('crypto' in globalThis) || !crypto.subtle) {
    throw new Error('WebCrypto 不可用，无法加密密码')
  }

  const keyData = pemToArrayBuffer(pub)
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


本目录下由脚本生成的密钥文件已加入 .gitignore，请勿提交。

生成新的一对（覆盖当前文件）可在仓库根目录执行：
  node scripts/generate-login-rsa.mjs
  会同步：frontend/.env*、application-local.yml、infrastructure/docker/.env（Compose 构建前端与 user-service 容器）、config/login-rsa-*.pem

或手动生成后自行保存为：
  login-rsa-public.pem   → 公钥，给前端 VITE_LOGIN_RSA_PUBLIC_KEY（见 frontend-VITE_LOGIN_RSA_PUBLIC_KEY.txt 一行格式）
  login-rsa-private.pem  → 私钥，给 user-service：app.security.login-rsa.private-key 或环境变量 LOGIN_RSA_PRIVATE_KEY

公钥与私钥必须为同一对，否则登录/注册会报「密码解密失败」。

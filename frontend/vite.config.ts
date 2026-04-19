import { defineConfig, loadEnv } from 'vite'
import basicSsl from '@vitejs/plugin-basic-ssl'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  /** 默认 HTTP；需要本机自签 HTTPS 时设 VITE_DEV_HTTPS=true（密码加密已支持无 Web Crypto 的 HTTP） */
  const devHttps = env.VITE_DEV_HTTPS === 'true'

  return {
    plugins: [
      ...(devHttps ? [basicSsl()] : []),
      vue(),
      vueJsx(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
        imports: ['vue', 'vue-router', 'pinia'],
        dts: 'src/auto-imports.d.ts',
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts',
      }),
    ],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      // 内网穿透 / 隧道会带非 localhost 的 Host，默认会被 Vite 拒绝
      allowedHosts: true,
      proxy: {
        '/api': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,
          ws: true,
        },
        // 与生产 nginx 一致：媒体 URL 为 /minio/<bucket>/<object>，开发时转发到本机 MinIO API 端口
        '/minio': {
          target: 'http://127.0.0.1:9001',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/minio/, ''),
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      rollupOptions: {
        output: {
          manualChunks: {
            'element-plus': ['element-plus'],
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
          },
        },
      },
    },
  }
})

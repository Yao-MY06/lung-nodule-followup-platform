import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端 5173，/api 经代理转发网关 8080（specs/modules/gateway.md 路由表）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})

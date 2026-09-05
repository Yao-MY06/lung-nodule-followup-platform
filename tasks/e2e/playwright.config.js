// R2 前端 E2E 冒烟（tasks/run-briefs/R2-前端E2E冒烟.md）
// 独立工程，不进 yiliao-ui 构建；截图/报告输出到 tasks/evidence/R2-*
// 端口说明：宿主机 5173 被另一项目（lims-handover）的 vite 占用，yiliao-ui dev 起 5174
import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  timeout: 90_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['html', { outputFolder: '../evidence/R2-report', open: 'never' }], ['list']],
  use: {
    baseURL: 'http://localhost:5174',
    locale: 'zh-CN',
    viewport: { width: 1440, height: 900 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 30_000
  }
})

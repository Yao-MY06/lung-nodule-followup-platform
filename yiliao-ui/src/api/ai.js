import request from './request'

// ai（specs/modules/ai.md §3）。SSE 流式接口不在此封装（需 fetch 流式读取），见各页面自行实现。
export const extractReport = (rawText) => request.post('/ai/report/extract', { rawText })
export const importKnowledge = (data) => request.post('/ai/knowledge/import', data)

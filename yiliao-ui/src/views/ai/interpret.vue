<template>
  <div class="interpret-page">
    <el-alert type="info" :closable="false" title="AI 解读为辅助参考，不替代医生诊断" class="interpret-tip" />

    <el-card shadow="never">
      <el-input
        v-model="rawText"
        type="textarea"
        :rows="8"
        placeholder="粘贴检查报告原文，AI 将进行结构化解读"
        :disabled="loading"
      />
      <div class="action-row">
        <el-button type="primary" :loading="loading" :disabled="!rawText.trim()" @click="startInterpret">
          {{ loading ? '解读中…' : '开始解读' }}
        </el-button>
        <el-button :disabled="loading || (!resultText && !hasCitations)" @click="clearResult">清空结果</el-button>
      </div>
    </el-card>

    <el-card v-if="resultText || loading || hasCitations" shadow="never" class="result-card">
      <template #header>解读结果</template>
      <div class="result-body">
        <div v-for="(seg, index) in segments" :key="index" class="result-segment">
          <template v-if="seg.title">
            <div class="segment-title">{{ seg.title }}</div>
            <div class="segment-content">{{ seg.content }}</div>
          </template>
          <template v-else>
            <div class="segment-content">{{ seg.content }}</div>
          </template>
        </div>
        <div v-if="citations.length" class="result-citations">
          <div v-for="(citation, i) in citations" :key="i" class="citation-item">{{ citation }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const rawText = ref('')
const resultText = ref('')
const citations = ref([])
const loading = ref(false)

const hasCitations = computed(() => citations.value.length > 0)

// 按【xxx】标题拆段，每段标题加粗
const segments = computed(() => {
  if (!resultText.value) {
    return []
  }
  return resultText.value
    .split(/(?=【[^】]+】)/)
    .filter((part) => part.trim())
    .map((part) => {
      const match = part.match(/^【([^】]+)】/)
      if (match) {
        return { title: match[1], content: part.slice(match[0].length).trim() }
      }
      return { title: '', content: part.trim() }
    })
})

// SSE 解析：buffer 累积拼接，按 \n\n 切事件块，解析 event:/data: 行（delta/citation/done）
async function readSse(response, onEvent) {
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop()
    for (const block of blocks) {
      if (!block.trim()) {
        continue
      }
      let event = ''
      const dataLines = []
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) {
          event = line.slice('event:'.length).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice('data:'.length).replace(/^ /, ''))
        }
      }
      onEvent(event, dataLines.join('\n'))
    }
  }
  if (buffer.trim()) {
    let event = ''
    const dataLines = []
    for (const line of buffer.split('\n')) {
      if (line.startsWith('event:')) {
        event = line.slice('event:'.length).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice('data:'.length).replace(/^ /, ''))
      }
    }
    onEvent(event, dataLines.join('\n'))
  }
}

function clearResult() {
  resultText.value = ''
  citations.value = []
}

async function startInterpret() {
  const text = rawText.value.trim()
  if (!text || loading.value) {
    return
  }
  clearResult()
  loading.value = true

  try {
    const response = await fetch('/api/ai/report/interpret', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + (localStorage.getItem('accessToken') || '')
      },
      body: JSON.stringify({ rawText: text })
    })

    if (!response.ok) {
      let errMsg = 'AI 服务暂时不可用，请稍后再试'
      try {
        const errBody = await response.json()
        if (errBody && errBody.msg) {
          errMsg = errBody.msg
        }
      } catch {
        // 非 JSON 错误体，用兜底文案
      }
      resultText.value = errMsg
      return
    }

    await readSse(response, (event, data) => {
      if (event === 'delta') {
        resultText.value += data
      } else if (event === 'citation') {
        citations.value.push(data)
      }
    })
    if (!resultText.value) {
      resultText.value = 'AI 服务暂时不可用，请稍后再试'
    }
  } catch {
    if (!resultText.value) {
      resultText.value = 'AI 服务暂时不可用，请稍后再试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.interpret-page {
  padding: 4px;
}
.interpret-tip {
  margin-bottom: 12px;
}
.action-row {
  margin-top: 12px;
  display: flex;
  gap: 12px;
}
.action-row .el-button + .el-button {
  margin-left: 0;
}
.result-card {
  margin-top: 16px;
}
.result-segment {
  margin-bottom: 12px;
}
.segment-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}
.segment-content {
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
}
.result-citations {
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px dashed #e4e7ed;
}
.citation-item {
  font-size: 12px;
  color: #909399;
}
</style>

<template>
  <div class="chat-page">
    <el-alert type="info" :closable="false" title="AI 回答为辅助参考，不替代医生诊断" class="chat-tip" />

    <div ref="messageAreaRef" class="message-area">
      <el-empty v-if="messages.length === 0" description="你好，我是 AI 健康助手，可以向我提问" />
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-row"
        :class="msg.role === 'user' ? 'from-user' : 'from-ai'"
      >
        <div class="bubble">
          <span v-if="msg.thinking" class="thinking">思考中…</span>
          <span v-else class="bubble-text">{{ msg.text }}</span>
          <div v-if="msg.citations && msg.citations.length" class="citations">
            <div v-for="(citation, i) in msg.citations" :key="i" class="citation-item">{{ citation }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="input-bar">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入你的问题，Enter 发送，Shift+Enter 换行"
        :disabled="sending"
        @keydown.enter.exact.prevent="send"
      />
      <el-button type="primary" size="large" :loading="sending" :disabled="!inputText.trim()" @click="send">
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'

const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const messageAreaRef = ref(null)

function scrollToBottom() {
  nextTick(() => {
    const el = messageAreaRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

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

async function send() {
  const text = inputText.value.trim()
  if (!text || sending.value) {
    return
  }
  messages.value.push({ role: 'user', text, citations: [] })
  const assistantMsg = { role: 'assistant', text: '', citations: [], thinking: true }
  messages.value.push(assistantMsg)
  inputText.value = ''
  sending.value = true
  scrollToBottom()

  try {
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + (localStorage.getItem('accessToken') || '')
      },
      body: JSON.stringify({ message: text })
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
      assistantMsg.thinking = false
      assistantMsg.text = errMsg
      return
    }

    await readSse(response, (event, data) => {
      if (event === 'delta') {
        assistantMsg.thinking = false
        assistantMsg.text += data
        scrollToBottom()
      } else if (event === 'citation') {
        assistantMsg.citations.push(data)
      }
    })
    assistantMsg.thinking = false
    if (!assistantMsg.text) {
      assistantMsg.text = 'AI 服务暂时不可用，请稍后再试'
    }
  } catch {
    assistantMsg.thinking = false
    assistantMsg.text = 'AI 服务暂时不可用，请稍后再试'
  } finally {
    sending.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 4px;
}
.chat-tip {
  margin-bottom: 12px;
}
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 8px;
  min-height: 320px;
  max-height: calc(100vh - 260px);
}
.message-row {
  display: flex;
  margin-bottom: 12px;
}
.message-row.from-user {
  justify-content: flex-end;
}
.message-row.from-ai {
  justify-content: flex-start;
}
.bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.from-user .bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.from-ai .bubble {
  background: #fff;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}
.thinking {
  color: #909399;
}
.citations {
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px dashed #e4e7ed;
}
.citation-item {
  font-size: 12px;
  color: #909399;
}
.input-bar {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  margin-top: 12px;
}
.input-bar .el-button {
  flex-shrink: 0;
}
</style>

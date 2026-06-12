<script setup>
import { computed, onMounted, ref } from 'vue'
import { messages } from './i18n.js'

const uiLang = ref('ru')
const t = computed(() => messages[uiLang.value])

const user = ref(null)
const authMode = ref('login')
const email = ref('')
const password = ref('')
const authError = ref('')

const topic = ref('')
const equipmentText = ref('')
const language = ref('ru')
const provider = ref('ANTHROPIC')
const loading = ref(false)
const error = ref('')
const plan = ref(null)

const history = ref([])

const chatQuestion = ref('')
const chatMessages = ref([])
const chatLoading = ref(false)

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  })
  if (response.status === 401) {
    user.value = null
    throw new Error('Unauthorized')
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.error || `Error ${response.status}`)
  }
  return response.json()
}

onMounted(async () => {
  try {
    const me = await api('/api/auth/me')
    user.value = me.email
    await loadHistory()
  } catch {
  }
})

async function submitAuth() {
  authError.value = ''
  try {
    if (authMode.value === 'register') {
      await api('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ email: email.value, password: password.value })
      })
    }
    const me = await api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: email.value, password: password.value })
    })
    user.value = me.email
    password.value = ''
    await loadHistory()
  } catch (e) {
    authError.value = e.message
  }
}

async function logout() {
  await fetch('/api/auth/logout', { method: 'POST' })
  user.value = null
  plan.value = null
  history.value = []
  chatMessages.value = []
}

async function loadHistory() {
  history.value = await api('/api/history')
}

async function openHistoryItem(id) {
  plan.value = await api(`/api/history/${id}`)
  chatMessages.value = []
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function generatePlan() {
  loading.value = true
  error.value = ''
  plan.value = null
  chatMessages.value = []
  try {
    plan.value = await api('/api/lab-plans', {
      method: 'POST',
      body: JSON.stringify({
        topic: topic.value,
        equipment: equipmentText.value.split('\n').map(s => s.trim()).filter(Boolean),
        language: language.value,
        provider: provider.value
      })
    })
    await loadHistory()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function askQuestion() {
  const question = chatQuestion.value.trim()
  if (!question || !plan.value?.historyId) return
  chatMessages.value.push({ role: 'user', text: question })
  chatQuestion.value = ''
  chatLoading.value = true
  try {
    const response = await api('/api/lab-plans/chat', {
      method: 'POST',
      body: JSON.stringify({
        historyId: plan.value.historyId,
        question,
        provider: provider.value
      })
    })
    chatMessages.value.push({ role: 'assistant', text: response.answer })
  } catch (e) {
    chatMessages.value.push({ role: 'assistant', text: e.message })
  } finally {
    chatLoading.value = false
  }
}
</script>

<template>
  <main class="container">
    <header class="topbar">
      <h1>OpticAI Lab</h1>
      <div class="topbar-right">
        <select v-model="uiLang" class="lang-switch">
          <option value="kk">ҚАЗ</option>
          <option value="ru">РУС</option>
          <option value="en">ENG</option>
        </select>
        <button v-if="user" class="link-btn" @click="logout">{{ t.logout }} ({{ user }})</button>
      </div>
    </header>
    <p class="subtitle">{{ t.subtitle }}</p>

    <form v-if="!user" class="card auth-form" @submit.prevent="submitAuth">
      <label>
        {{ t.email }}
        <input v-model="email" type="email" required autocomplete="username" />
      </label>
      <label>
        {{ t.password }}
        <input v-model="password" type="password" required minlength="6" autocomplete="current-password" />
      </label>
      <button type="submit">{{ authMode === 'login' ? t.login : t.register }}</button>
      <button type="button" class="link-btn"
              @click="authMode = authMode === 'login' ? 'register' : 'login'">
        {{ authMode === 'login' ? t.noAccount : t.haveAccount }}
      </button>
      <p v-if="authError" class="error">{{ authError }}</p>
    </form>

    <template v-else>
      <form class="card" @submit.prevent="generatePlan">
        <label>
          {{ t.topic }}
          <input v-model="topic" required :placeholder="t.topicPlaceholder" />
        </label>
        <label>
          {{ t.equipment }}
          <textarea v-model="equipmentText" rows="5" :placeholder="t.equipmentPlaceholder" />
        </label>
        <div class="form-row">
          <label>
            {{ t.language }}
            <select v-model="language">
              <option value="kk">Қазақша</option>
              <option value="ru">Русский</option>
              <option value="en">English</option>
            </select>
          </label>
          <label>
            {{ t.model }}
            <select v-model="provider">
              <option value="ANTHROPIC">Claude (Anthropic)</option>
              <option value="OPENAI">ChatGPT (OpenAI)</option>
            </select>
          </label>
        </div>
        <button type="submit" :disabled="loading || !topic">
          {{ loading ? t.generating : t.generate }}
        </button>
      </form>

      <p v-if="error" class="error">{{ error }}</p>

      <section v-if="plan" class="card plan">
        <h2>{{ plan.title }}</h2>

        <h3>{{ t.theory }}</h3>
        <p>{{ plan.theory }}</p>

        <h3>{{ t.steps }}</h3>
        <ol>
          <li v-for="step in plan.steps" :key="step.number">
            <strong>{{ step.title }}</strong>
            <p>{{ step.description }}</p>
            <ul v-if="step.videos?.length" class="videos">
              <li v-for="video in step.videos" :key="video.videoId">
                <a :href="video.url" target="_blank">▶ {{ video.title }}</a>
              </li>
            </ul>
            <a v-else-if="step.videoSearchQuery"
               :href="`https://www.youtube.com/results?search_query=${encodeURIComponent(step.videoSearchQuery)}`"
               target="_blank">{{ t.findVideo }}</a>
          </li>
        </ol>

        <template v-if="plan.missingEquipment?.length">
          <h3>{{ t.missingEquipment }}</h3>
          <ul><li v-for="item in plan.missingEquipment" :key="item">{{ item }}</li></ul>
        </template>

        <h3>{{ t.mistakes }}</h3>
        <ul><li v-for="item in plan.commonMistakes" :key="item">{{ item }}</li></ul>

        <h3>{{ t.expectedResults }}</h3>
        <ul><li v-for="item in plan.expectedResults" :key="item">{{ item }}</li></ul>

        <div class="chat">
          <h3>{{ t.chatTitle }}</h3>
          <div v-for="(message, index) in chatMessages" :key="index"
               :class="['chat-message', message.role]">
            {{ message.text }}
          </div>
          <form class="chat-form" @submit.prevent="askQuestion">
            <input v-model="chatQuestion" :placeholder="t.chatPlaceholder" :disabled="chatLoading" />
            <button type="submit" :disabled="chatLoading || !chatQuestion.trim()">
              {{ chatLoading ? t.asking : t.ask }}
            </button>
          </form>
        </div>
      </section>

      <section v-if="history.length" class="card">
        <h3>{{ t.history }}</h3>
        <ul class="history">
          <li v-for="item in history" :key="item.id">
            <button class="link-btn" @click="openHistoryItem(item.id)">
              {{ item.topic }}
            </button>
            <span class="muted">{{ new Date(item.createdAt).toLocaleString() }}</span>
          </li>
        </ul>
      </section>
    </template>
  </main>
</template>

<style>
body {
  font-family: system-ui, sans-serif;
  margin: 0;
  background: #f7f7f5;
  color: #1a1a1a;
}
.container {
  max-width: 760px;
  margin: 0 auto;
  padding: 2rem 1rem 4rem;
}
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.topbar h1 {
  margin: 0;
}
.topbar-right {
  display: flex;
  gap: 0.8rem;
  align-items: center;
}
.lang-switch {
  padding: 0.3rem;
}
.subtitle {
  color: #555;
}
.card {
  background: #fff;
  padding: 1.5rem;
  border-radius: 12px;
  border: 1px solid #e2e2de;
  margin-bottom: 1.5rem;
}
form.card, .auth-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.auth-form {
  max-width: 400px;
}
.form-row {
  display: flex;
  gap: 1rem;
}
.form-row label {
  flex: 1;
}
label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-weight: 600;
}
input, textarea, select {
  font: inherit;
  font-weight: 400;
  padding: 0.6rem;
  border: 1px solid #ccc;
  border-radius: 8px;
}
button {
  font: inherit;
  font-weight: 600;
  padding: 0.7rem;
  border: none;
  border-radius: 8px;
  background: #1d4ed8;
  color: #fff;
  cursor: pointer;
}
button:disabled {
  background: #93a8e8;
  cursor: default;
}
.link-btn {
  background: none;
  color: #1d4ed8;
  padding: 0.2rem;
  text-align: left;
}
.error {
  color: #b91c1c;
}
.plan li {
  margin-bottom: 0.8rem;
}
.videos {
  list-style: none;
  padding-left: 0;
  margin: 0.3rem 0;
}
.videos li {
  margin-bottom: 0.2rem;
}
.history {
  list-style: none;
  padding-left: 0;
}
.history li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.4rem;
}
.muted {
  color: #888;
  font-size: 0.85rem;
  white-space: nowrap;
}
.chat {
  margin-top: 1.5rem;
  border-top: 1px solid #e2e2de;
  padding-top: 1rem;
}
.chat-message {
  padding: 0.6rem 0.9rem;
  border-radius: 10px;
  margin-bottom: 0.5rem;
  white-space: pre-wrap;
}
.chat-message.user {
  background: #e8eefc;
}
.chat-message.assistant {
  background: #f1f1ee;
}
.chat-form {
  display: flex;
  gap: 0.5rem;
}
.chat-form input {
  flex: 1;
}
</style>

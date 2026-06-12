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
const showForm = ref(true)

const history = ref([])
const folders = ref([])
const newFolderName = ref('')
const creatingFolder = ref(false)
const expandedFolders = ref(new Set())

const chatQuestion = ref('')
const chatMessages = ref([])
const chatLoading = ref(false)

const playingVideo = ref(null)

const folderGroups = computed(() => {
  const groups = folders.value.map(folder => ({
    folder,
    items: history.value.filter(item => item.folderId === folder.id)
  }))
  const loose = history.value.filter(item => !item.folderId)
  return { groups, loose }
})

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
  const text = await response.text()
  return text ? JSON.parse(text) : null
}

onMounted(async () => {
  try {
    const me = await api('/api/auth/me')
    user.value = me.email
    await reload()
  } catch {
  }
})

async function reload() {
  const [historyData, foldersData] = await Promise.all([api('/api/history'), api('/api/folders')])
  history.value = historyData
  folders.value = foldersData
}

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
    await reload()
  } catch (e) {
    authError.value = e.message
  }
}

async function logout() {
  await fetch('/api/auth/logout', { method: 'POST' })
  user.value = null
  plan.value = null
  history.value = []
  folders.value = []
  chatMessages.value = []
}

function startNewPlan() {
  plan.value = null
  chatMessages.value = []
  showForm.value = true
  playingVideo.value = null
}

async function openHistoryItem(id) {
  plan.value = await api(`/api/history/${id}`)
  chatMessages.value = await api(`/api/history/${id}/chat`)
  showForm.value = false
  playingVideo.value = null
  document.querySelector('.content')?.scrollTo({ top: 0 })
}

async function generatePlan() {
  loading.value = true
  error.value = ''
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
    chatMessages.value = []
    showForm.value = false
    playingVideo.value = null
    await reload()
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

async function createFolder() {
  const name = newFolderName.value.trim()
  if (!name) return
  await api('/api/folders', { method: 'POST', body: JSON.stringify({ name }) })
  newFolderName.value = ''
  creatingFolder.value = false
  await reload()
}

async function deleteFolder(id) {
  if (!confirm(t.value.confirmDeleteFolder)) return
  await api(`/api/folders/${id}`, { method: 'DELETE' })
  await reload()
}

async function movePlan(historyId, folderId) {
  await api(`/api/history/${historyId}/folder`, {
    method: 'PUT',
    body: JSON.stringify({ folderId: folderId || null })
  })
  await reload()
}

async function deletePlan(id) {
  if (!confirm(t.value.confirmDeletePlan)) return
  await api(`/api/history/${id}`, { method: 'DELETE' })
  if (plan.value?.historyId === id) startNewPlan()
  await reload()
}

function toggleFolder(id) {
  const set = new Set(expandedFolders.value)
  set.has(id) ? set.delete(id) : set.add(id)
  expandedFolders.value = set
}

function thumbnail(videoId) {
  return `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`
}
</script>

<template>
  <div v-if="!user" class="auth-screen">
    <div class="auth-box">
      <div class="brand">
        <span class="brand-mark">◎</span>
        <h1>OpticAI Lab</h1>
      </div>
      <p class="auth-subtitle">{{ t.subtitle }}</p>
      <form @submit.prevent="submitAuth">
        <label>
          {{ t.email }}
          <input v-model="email" type="email" required autocomplete="username" />
        </label>
        <label>
          {{ t.password }}
          <input v-model="password" type="password" required minlength="6" autocomplete="current-password" />
        </label>
        <button type="submit" class="primary">
          {{ authMode === 'login' ? t.login : t.register }}
        </button>
        <button type="button" class="ghost"
                @click="authMode = authMode === 'login' ? 'register' : 'login'">
          {{ authMode === 'login' ? t.noAccount : t.haveAccount }}
        </button>
        <p v-if="authError" class="error">{{ authError }}</p>
      </form>
      <div class="auth-lang">
        <button v-for="code in ['kk', 'ru', 'en']" :key="code"
                :class="['lang-btn', { active: uiLang === code }]" @click="uiLang = code">
          {{ code.toUpperCase() }}
        </button>
      </div>
    </div>
  </div>

  <div v-else class="layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">◎</span>
        <span class="brand-name">OpticAI Lab</span>
      </div>

      <button class="primary new-plan" @click="startNewPlan">+ {{ t.newPlan }}</button>

      <div class="sidebar-section">
        <div class="sidebar-heading">
          <span>{{ t.folders }}</span>
          <button class="icon-btn" :title="t.newFolder" @click="creatingFolder = !creatingFolder">+</button>
        </div>
        <form v-if="creatingFolder" class="folder-form" @submit.prevent="createFolder">
          <input v-model="newFolderName" :placeholder="t.folderNamePlaceholder" autofocus />
        </form>

        <div v-for="group in folderGroups.groups" :key="group.folder.id" class="folder">
          <div class="folder-row" @click="toggleFolder(group.folder.id)">
            <span class="folder-name">
              {{ expandedFolders.has(group.folder.id) ? '▾' : '▸' }} 📁 {{ group.folder.name }}
              <span class="count">{{ group.items.length }}</span>
            </span>
            <button class="icon-btn danger" @click.stop="deleteFolder(group.folder.id)">×</button>
          </div>
          <ul v-if="expandedFolders.has(group.folder.id)" class="plan-list nested">
            <li v-for="item in group.items" :key="item.id"
                :class="{ active: plan?.historyId === item.id }">
              <button class="plan-link" @click="openHistoryItem(item.id)">{{ item.topic }}</button>
            </li>
          </ul>
        </div>

        <ul class="plan-list">
          <li v-for="item in folderGroups.loose" :key="item.id"
              :class="{ active: plan?.historyId === item.id }">
            <button class="plan-link" @click="openHistoryItem(item.id)">{{ item.topic }}</button>
          </li>
        </ul>
      </div>

      <div class="sidebar-footer">
        <div class="lang-row">
          <button v-for="code in ['kk', 'ru', 'en']" :key="code"
                  :class="['lang-btn', { active: uiLang === code }]" @click="uiLang = code">
            {{ code.toUpperCase() }}
          </button>
        </div>
        <button class="ghost logout" @click="logout">{{ t.logout }}</button>
        <div class="user-email">{{ user }}</div>
      </div>
    </aside>

    <main class="content">
      <form v-if="showForm" class="panel form-panel" @submit.prevent="generatePlan">
        <h2>{{ t.emptyTitle }}</h2>
        <p class="muted">{{ t.emptyText }}</p>
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
        <button type="submit" class="primary" :disabled="loading || !topic">
          {{ loading ? t.generating : t.generate }}
        </button>
        <p v-if="error" class="error">{{ error }}</p>
      </form>

      <article v-if="plan && !showForm" class="plan-view">
        <header class="plan-header">
          <h2>{{ plan.title }}</h2>
          <div class="plan-actions">
            <select :value="history.find(h => h.id === plan.historyId)?.folderId ?? ''"
                    @change="movePlan(plan.historyId, $event.target.value ? Number($event.target.value) : null)">
              <option value="">{{ t.noFolder }}</option>
              <option v-for="folder in folders" :key="folder.id" :value="folder.id">
                {{ folder.name }}
              </option>
            </select>
            <button class="icon-btn danger big" :title="t.deletePlan"
                    @click="deletePlan(plan.historyId)">🗑</button>
          </div>
        </header>

        <section class="panel">
          <h3>{{ t.theory }}</h3>
          <p class="theory">{{ plan.theory }}</p>
        </section>

        <section class="panel">
          <h3>{{ t.steps }}</h3>
          <div v-for="step in plan.steps" :key="step.number" class="step">
            <div class="step-head">
              <span class="step-num">{{ step.number }}</span>
              <strong>{{ step.title }}</strong>
            </div>
            <p>{{ step.description }}</p>
            <div v-if="step.videos?.length" class="video-grid">
              <div v-for="video in step.videos" :key="video.videoId" class="video-card">
                <iframe v-if="playingVideo === video.videoId"
                        :src="`https://www.youtube.com/embed/${video.videoId}?autoplay=1`"
                        allow="autoplay; encrypted-media; picture-in-picture"
                        allowfullscreen />
                <button v-else class="video-thumb" @click="playingVideo = video.videoId">
                  <img :src="thumbnail(video.videoId)" :alt="video.title" loading="lazy" />
                  <span class="play-badge">▶</span>
                </button>
                <div class="video-title">{{ video.title }}</div>
              </div>
            </div>
            <a v-else-if="step.videoSearchQuery" class="search-link"
               :href="`https://www.youtube.com/results?search_query=${encodeURIComponent(step.videoSearchQuery)}`"
               target="_blank">{{ t.findVideo }} ↗</a>
          </div>
        </section>

        <section v-if="plan.missingEquipment?.length" class="panel warn">
          <h3>{{ t.missingEquipment }}</h3>
          <ul><li v-for="item in plan.missingEquipment" :key="item">{{ item }}</li></ul>
        </section>

        <section class="panel">
          <h3>{{ t.mistakes }}</h3>
          <ul><li v-for="item in plan.commonMistakes" :key="item">{{ item }}</li></ul>
        </section>

        <section class="panel">
          <h3>{{ t.expectedResults }}</h3>
          <ul><li v-for="item in plan.expectedResults" :key="item">{{ item }}</li></ul>
        </section>

        <section class="panel chat">
          <h3>{{ t.chatTitle }}</h3>
          <div v-for="(message, index) in chatMessages" :key="index"
               :class="['chat-message', message.role]">
            {{ message.text }}
          </div>
          <form class="chat-form" @submit.prevent="askQuestion">
            <input v-model="chatQuestion" :placeholder="t.chatPlaceholder" :disabled="chatLoading" />
            <button type="submit" class="primary" :disabled="chatLoading || !chatQuestion.trim()">
              {{ chatLoading ? t.asking : t.ask }}
            </button>
          </form>
        </section>
      </article>
    </main>
  </div>
</template>

<style>
:root {
  --bg: #f6f7f9;
  --panel: #ffffff;
  --border: #e4e7ec;
  --text: #16181d;
  --text-soft: #5f6672;
  --accent: #2f5af0;
  --accent-soft: #e8edfd;
  --sidebar-bg: #14161b;
  --sidebar-text: #c6cbd4;
  --danger: #c2402f;
  --radius: 10px;
}
* {
  box-sizing: border-box;
}
body {
  font-family: -apple-system, 'Segoe UI', 'Inter', system-ui, sans-serif;
  margin: 0;
  background: var(--bg);
  color: var(--text);
}
h1, h2, h3 {
  font-weight: 650;
  letter-spacing: -0.015em;
}
button {
  font: inherit;
  cursor: pointer;
  border: none;
  background: none;
  color: inherit;
}
input, textarea, select {
  font: inherit;
  padding: 0.6rem 0.7rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #fff;
  color: var(--text);
}
input:focus, textarea:focus, select:focus {
  outline: 2px solid var(--accent-soft);
  border-color: var(--accent);
}
label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-weight: 560;
  font-size: 0.92rem;
}
.primary {
  background: var(--accent);
  color: #fff;
  font-weight: 600;
  padding: 0.7rem 1.2rem;
  border-radius: var(--radius);
  transition: background 0.15s;
}
.primary:hover {
  background: #2448c8;
}
.primary:disabled {
  background: #9eb1f0;
  cursor: default;
}
.ghost {
  color: var(--accent);
  padding: 0.4rem;
}
.error {
  color: var(--danger);
}
.muted {
  color: var(--text-soft);
}

.auth-screen {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #14161b 0%, #232734 60%, #2f3a5c 100%);
}
.auth-box {
  width: 400px;
  background: var(--panel);
  border-radius: 16px;
  padding: 2.2rem;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
}
.auth-box form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1.4rem;
}
.auth-subtitle {
  color: var(--text-soft);
  font-size: 0.92rem;
  margin: 0.4rem 0 0;
}
.auth-lang {
  display: flex;
  gap: 0.4rem;
  justify-content: center;
  margin-top: 1.2rem;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}
.brand h1 {
  margin: 0;
  font-size: 1.5rem;
}
.brand-mark {
  font-size: 1.5rem;
  color: var(--accent);
}
.brand-name {
  font-weight: 650;
  font-size: 1.1rem;
  color: #fff;
}

.layout {
  display: flex;
  height: 100vh;
}
.sidebar {
  width: 290px;
  flex-shrink: 0;
  background: var(--sidebar-bg);
  color: var(--sidebar-text);
  display: flex;
  flex-direction: column;
  padding: 1.2rem;
  gap: 1.2rem;
}
.new-plan {
  width: 100%;
}
.sidebar-section {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
.sidebar-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  text-transform: uppercase;
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  color: #7d8494;
  margin-bottom: 0.5rem;
}
.icon-btn {
  color: #7d8494;
  font-size: 1rem;
  padding: 0 0.3rem;
  border-radius: 6px;
}
.icon-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}
.icon-btn.danger:hover {
  color: #ff8b78;
}
.icon-btn.big {
  font-size: 1.1rem;
  padding: 0.4rem 0.6rem;
}
.folder-form input {
  width: 100%;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.14);
  color: #fff;
  margin-bottom: 0.5rem;
}
.folder-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.35rem 0.4rem;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.92rem;
}
.folder-row:hover {
  background: rgba(255, 255, 255, 0.06);
}
.count {
  color: #7d8494;
  font-size: 0.8rem;
  margin-left: 0.3rem;
}
.plan-list {
  list-style: none;
  margin: 0.2rem 0 0.6rem;
  padding: 0;
}
.plan-list.nested {
  margin-left: 1rem;
}
.plan-list li {
  border-radius: 8px;
}
.plan-list li.active {
  background: rgba(47, 90, 240, 0.25);
}
.plan-link {
  display: block;
  width: 100%;
  text-align: left;
  padding: 0.4rem 0.6rem;
  font-size: 0.9rem;
  color: var(--sidebar-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border-radius: 8px;
}
.plan-link:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}
.sidebar-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.lang-row {
  display: flex;
  gap: 0.4rem;
}
.lang-btn {
  padding: 0.25rem 0.55rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
  color: #7d8494;
  border: 1px solid transparent;
}
.lang-btn.active {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.25);
}
.logout {
  text-align: left;
  color: #7d8494;
  padding: 0.2rem 0;
}
.logout:hover {
  color: #fff;
}
.user-email {
  font-size: 0.78rem;
  color: #7d8494;
  overflow: hidden;
  text-overflow: ellipsis;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 2.2rem clamp(1rem, 5vw, 3.5rem);
}
.panel {
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 1.6rem;
  margin-bottom: 1.2rem;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}
.panel.warn {
  border-left: 4px solid #e8a23d;
}
.form-panel {
  max-width: 640px;
  margin: 8vh auto 0;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}
.form-panel h2 {
  margin: 0;
}
.form-panel .muted {
  margin: -0.6rem 0 0;
  font-size: 0.92rem;
}
.form-row {
  display: flex;
  gap: 1rem;
}
.form-row label {
  flex: 1;
}

.plan-view {
  max-width: 860px;
  margin: 0 auto;
}
.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.2rem;
}
.plan-header h2 {
  margin: 0;
  font-size: 1.6rem;
}
.plan-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}
.plan-actions select {
  font-size: 0.85rem;
  padding: 0.4rem 0.5rem;
}
.theory {
  line-height: 1.65;
  color: #2a2e36;
}
.step {
  padding: 1.1rem 0;
  border-bottom: 1px solid var(--border);
}
.step:last-child {
  border-bottom: none;
}
.step-head {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  margin-bottom: 0.3rem;
}
.step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 650;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.step p {
  margin: 0.3rem 0 0.7rem;
  line-height: 1.6;
  color: #2a2e36;
}
.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 0.9rem;
}
.video-card iframe,
.video-thumb {
  width: 100%;
  aspect-ratio: 16 / 9;
  border: none;
  border-radius: 10px;
  display: block;
}
.video-thumb {
  position: relative;
  padding: 0;
  overflow: hidden;
  background: #000;
}
.video-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}
.video-thumb:hover img {
  transform: scale(1.04);
}
.play-badge {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.video-title {
  font-size: 0.84rem;
  color: var(--text-soft);
  margin-top: 0.4rem;
  line-height: 1.35;
}
.search-link {
  color: var(--accent);
  font-size: 0.9rem;
  text-decoration: none;
}
.panel ul {
  margin: 0;
  padding-left: 1.2rem;
}
.panel li {
  margin-bottom: 0.5rem;
  line-height: 1.55;
}

.chat-message {
  padding: 0.7rem 1rem;
  border-radius: 12px;
  margin-bottom: 0.6rem;
  white-space: pre-wrap;
  line-height: 1.55;
  font-size: 0.95rem;
}
.chat-message.user {
  background: var(--accent-soft);
  margin-left: 15%;
}
.chat-message.assistant {
  background: #f2f3f5;
  margin-right: 15%;
}
.chat-form {
  display: flex;
  gap: 0.6rem;
  margin-top: 0.8rem;
}
.chat-form input {
  flex: 1;
}
</style>

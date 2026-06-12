<script setup>
import { ref } from 'vue'

const topic = ref('')
const equipmentText = ref('')
const language = ref('ru')
const provider = ref('ANTHROPIC')
const loading = ref(false)
const error = ref('')
const plan = ref(null)

async function generatePlan() {
  loading.value = true
  error.value = ''
  plan.value = null
  try {
    const response = await fetch('/api/lab-plans', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        topic: topic.value,
        equipment: equipmentText.value.split('\n').map(s => s.trim()).filter(Boolean),
        language: language.value,
        provider: provider.value
      })
    })
    if (!response.ok) throw new Error(`Server error: ${response.status}`)
    plan.value = await response.json()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="container">
    <h1>OpticAI Lab</h1>
    <p class="subtitle">AI-ассистент для лабораторных работ по оптике и лазерной физике</p>

    <form @submit.prevent="generatePlan">
      <label>
        Тема лабораторной работы
        <input v-model="topic" required placeholder="Например: Кольца Ньютона" />
      </label>
      <label>
        Доступное оборудование (каждый пункт с новой строки)
        <textarea v-model="equipmentText" rows="5" placeholder="He-Ne лазер&#10;Собирающая линза&#10;Экран" />
      </label>
      <label>
        Язык
        <select v-model="language">
          <option value="kk">Қазақша</option>
          <option value="ru">Русский</option>
          <option value="en">English</option>
        </select>
      </label>
      <label>
        ИИ-модель
        <select v-model="provider">
          <option value="ANTHROPIC">Claude (Anthropic)</option>
          <option value="OPENAI">ChatGPT (OpenAI)</option>
        </select>
      </label>
      <button type="submit" :disabled="loading || !topic">
        {{ loading ? 'Генерация…' : 'Сгенерировать план' }}
      </button>
    </form>

    <p v-if="error" class="error">{{ error }}</p>

    <section v-if="plan" class="plan">
      <h2>{{ plan.title }}</h2>

      <h3>Теория</h3>
      <p>{{ plan.theory }}</p>

      <h3>Шаги выполнения</h3>
      <ol>
        <li v-for="step in plan.steps" :key="step.number">
          <strong>{{ step.title }}</strong>
          <p>{{ step.description }}</p>
          <ul v-if="step.videos?.length" class="videos">
            <li v-for="video in step.videos" :key="video.videoId">
              <a :href="video.url" target="_blank">▶ {{ video.title }}</a>
            </li>
          </ul>
          <a v-else
             :href="`https://www.youtube.com/results?search_query=${encodeURIComponent(step.videoSearchQuery)}`"
             target="_blank">Найти видео</a>
        </li>
      </ol>

      <template v-if="plan.missingEquipment?.length">
        <h3>Недостающее оборудование</h3>
        <ul><li v-for="item in plan.missingEquipment" :key="item">{{ item }}</li></ul>
      </template>

      <h3>Типичные ошибки</h3>
      <ul><li v-for="item in plan.commonMistakes" :key="item">{{ item }}</li></ul>

      <h3>Ожидаемые результаты</h3>
      <ul><li v-for="item in plan.expectedResults" :key="item">{{ item }}</li></ul>
    </section>
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
.subtitle {
  color: #555;
}
form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: #fff;
  padding: 1.5rem;
  border-radius: 12px;
  border: 1px solid #e2e2de;
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
.error {
  color: #b91c1c;
}
.plan {
  margin-top: 2rem;
  background: #fff;
  padding: 1.5rem;
  border-radius: 12px;
  border: 1px solid #e2e2de;
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
</style>

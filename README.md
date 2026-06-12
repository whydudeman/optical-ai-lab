# optical-ai-lab

Universal AI lab helper for optics and laser physics. A student enters a lab work topic and the available equipment, and the assistant generates a step-by-step execution plan: theory, concrete steps with video links, missing equipment, typical mistakes, and expected results.

## Stack

- Backend: Java 21, Spring Boot 3, Spring AI (Anthropic; OpenAI and Gemini planned)
- Frontend: Vue 3 + Vite
- Languages: KZ / RU / EN

## Run

Backend (requires `ANTHROPIC_API_KEY`):

```sh
export ANTHROPIC_API_KEY=sk-ant-...
mvn spring-boot:run
```

Frontend (dev server on http://localhost:5173, proxies `/api` to the backend):

```sh
cd frontend
npm install
npm run dev
```

## API

`POST /api/lab-plans`

```json
{
  "topic": "Кольца Ньютона",
  "equipment": ["He-Ne лазер", "собирающая линза", "экран"],
  "language": "ru"
}
```

Returns a structured plan: `title`, `theory`, `steps[]` (with `videoSearchQuery` per step), `missingEquipment[]`, `commonMistakes[]`, `expectedResults[]`.

## Roadmap

1. ~~Skeleton: topic + equipment → structured plan (Anthropic)~~
2. Multi-provider switching: OpenAI, Gemini
3. Video attachment via YouTube Data API
4. Auth + session history (PostgreSQL)
5. Chat follow-up within a generated plan
6. Trilingual UI (KZ/RU/EN)

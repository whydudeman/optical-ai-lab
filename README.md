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

## Environment

| Variable | Purpose |
|---|---|
| `ANTHROPIC_API_KEY` | Claude provider |
| `OPENAI_API_KEY` | ChatGPT provider (optional) |
| `YOUTUBE_API_KEY` | Video search per step (optional, plan works without it) |

Data is stored in file-backed H2 (`./data/`); switch `spring.datasource` to PostgreSQL for production.

## Roadmap

1. ~~Skeleton: topic + equipment → structured plan (Anthropic)~~
2. ~~Multi-provider switching: OpenAI~~ (Gemini pending)
3. ~~Video attachment via YouTube Data API~~
4. ~~Auth + per-user plan history~~
5. ~~Chat follow-up within a generated plan~~
6. ~~Trilingual UI (KZ/RU/EN)~~
7. Pilot with students, usage metrics
8. PostgreSQL + deployment

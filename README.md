# Audit Committee Reporting
# AI Service — Tool-27 Audit Committee Reporting

Flask microservice providing AI-powered features via Groq LLM + ChromaDB RAG.

## Tech Stack

| Technology | Purpose |
|---|---|
| Python 3.11 + Flask 3.x | Web framework |
| Groq API (LLaMA-3.3-70b) | AI language model (free tier) |
| ChromaDB | Vector database for RAG |
| sentence-transformers | Text → vector embeddings |
| Redis 7 | AI response cache (15-min TTL) |
| flask-limiter | Rate limiting (30 req/min) |

## Prerequisites

- Python 3.11
- Docker + Docker Compose
- Free Groq API key from https://console.groq.com

## Setup (Local Development)

```bash
# 1. Clone the repo and enter the ai-service folder
cd ai-service

# 2. Create virtual environment
python -m venv venv
source venv/bin/activate        # Mac/Linux
venv\Scripts\activate           # Windows

# 3. Install dependencies
pip install -r requirements.txt

# 4. Create your .env file
cp .env.example .env
# Edit .env and add your GROQ_API_KEY

# 5. Test Groq connection
python test_groq.py

# 6. Run the service
python app.py
# Service starts at http://localhost:5000
```

## Setup (Docker — recommended)

```bash
# From the project root folder:
docker-compose up --build

# The AI service will be available at http://localhost:5000
```

## Endpoints

### GET /health
Returns service health, uptime, cache stats, and ChromaDB document count.

```bash
curl http://localhost:5000/health
```

### POST /categorise
Classifies an audit finding into one of 8 categories.

```bash
curl -X POST http://localhost:5000/categorise \
  -H "Content-Type: application/json" \
  -d '{"text": "Bank reconciliation not performed for Q3 2026."}'
```

Response:
```json
{
  "category": "Financial Risk",
  "confidence": 0.94,
  "reasoning": "Bank reconciliation failures directly impact financial reporting accuracy.",
  "meta": { "model_used": "llama-3.3-70b-versatile", "tokens_used": 87, "cached": false, "is_fallback": false }
}
```

### POST /generate-report
Generate a formal audit committee report.

**Async mode (default):**
```bash
curl -X POST http://localhost:5000/generate-report \
  -H "Content-Type: application/json" \
  -d '{"title": "Q2 2026 Report", "period": "Q2 2026", "items": ["Missing approval workflow", "Unpatched servers"]}'
```
Returns `{"job_id": "...", "status": "processing"}` — then poll:
```bash
curl http://localhost:5000/generate-report/status/<job_id>
```

**Streaming mode:**
```bash
curl -N -X POST "http://localhost:5000/generate-report?stream=true" \
  -H "Content-Type: application/json" \
  -d '{"title": "Q2 2026 Report", "items": ["Missing controls"]}'
```

### POST /query
Ask a question answered using RAG (documents in ChromaDB).

```bash
curl -X POST http://localhost:5000/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What were the top audit findings this quarter?"}'
```

## Running Tests

```bash
# All tests pass without a live Groq API key (Groq is mocked)
pytest tests/test_ai_developer2.py -v
```

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GROQ_API_KEY` | ✅ Yes | — | Get free at console.groq.com |
| `REDIS_HOST` | No | localhost | Redis hostname |
| `REDIS_PORT` | No | 6379 | Redis port |
| `CHROMA_PATH` | No | ./chroma_data | ChromaDB storage path |

## Developer

AI Developer 2 — Team 7, Tool-27 Capstone Sprint (14 Apr – 9 May 2026)
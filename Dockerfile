# ── Dockerfile for AI microservice (ai-service/) ──────────────────────────────
# Build: docker build -t ai-service .
# Run:   docker run -p 5000:5000 --env-file ../.env ai-service

# Use the exact Python version specified in the project spec
FROM python:3.11-slim

# Set working directory inside the container
WORKDIR /app

# Install system dependencies needed by sentence-transformers and chromadb
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy and install Python packages FIRST (Docker layer caching)
# This means if only your code changes (not requirements.txt),
# Docker won't re-install all packages — much faster rebuilds
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create directory for ChromaDB persistent storage
RUN mkdir -p /app/chroma_data

# Expose the port Flask runs on
EXPOSE 5000

# Health check — Docker will mark the container "unhealthy" if this fails
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:5000/health || exit 1

# Start with Gunicorn (production WSGI server, more stable than Flask dev server)
# --timeout 120 : allow up to 120s for long AI calls
# --workers 2   : 2 parallel worker processes
CMD ["gunicorn", \
     "--bind", "0.0.0.0:5000", \
     "--timeout", "120", \
     "--workers", "2", \
     "--log-level", "info", \
     "app:create_app()"]
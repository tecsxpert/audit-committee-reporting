import time
import logging
from flask import Blueprint, request, jsonify
from sentence_transformers import SentenceTransformer
from services.groq_client import call_groq, get_model_name
from services.chroma_client import get_collection
from services.redis_cache import make_cache_key, get_cached, set_cached

logger = logging.getLogger(__name__)
query_bp = Blueprint("query", __name__)

# Module-level embedding model (loaded ONCE at startup — it's slow to load)
# Pre-loading prevents a 10-second delay on the first request
_embedding_model: SentenceTransformer | None = None


def get_embedding_model() -> SentenceTransformer:
    """
    Return the sentence-transformers embedding model.
    Loads it on first call, reuses on all subsequent calls.
    'all-MiniLM-L6-v2' is fast and accurate for English text.
    """
    global _embedding_model
    if _embedding_model is None:
        logger.info("Loading sentence-transformers model (first load may take ~30s)...")
        _embedding_model = SentenceTransformer("all-MiniLM-L6-v2")
        logger.info("Embedding model loaded successfully.")
    return _embedding_model


# Safe fallback — returned if Groq or ChromaDB fails
_FALLBACK_RESPONSE = {
    "answer": "AI service is temporarily unavailable. Please try again in a moment.",
    "sources": [],
    "meta": {
        "model_used": "fallback",
        "tokens_used": 0,
        "response_time_ms": 0,
        "cached": False,
        "confidence": 0.0,
        "is_fallback": True,
    },
}


@query_bp.route("/query", methods=["POST"])
def query_rag():
    """
    Answer a question using RAG (Retrieval-Augmented Generation).

    Steps:
    1.  Validate input
    2.  Check Redis cache
    3.  Embed the question into a vector using sentence-transformers
    4.  Query ChromaDB for the 3 most similar documents
    5.  Build a prompt that includes those documents as context
    6.  Call Groq to generate the answer
    7.  Return answer + source documents + meta
    """
    # ── Step 1: Validate ───────────────────────────────────────────────────────
    data = request.get_json(silent=True)
    if not data or not data.get("question"):
        return jsonify({"error": "Request body must contain a 'question' field."}), 400

    question = str(data["question"]).strip()
    if len(question) < 5:
        return jsonify({"error": "Question is too short. Minimum 5 characters."}), 400
    if len(question) > 2000:
        return jsonify({"error": "Question is too long. Maximum 2000 characters."}), 400

    fresh = bool(data.get("fresh", False))

    # ── Step 2: Cache check ────────────────────────────────────────────────────
    cache_key = make_cache_key("query", {"question": question})
    if not fresh:
        cached = get_cached(cache_key)
        if cached:
            cached["meta"]["cached"] = True
            return jsonify(cached), 200

    start_time = time.time()

    try:
        # ── Step 3: Embed the question ─────────────────────────────────────────
        model = get_embedding_model()
        question_embedding = model.encode(question).tolist()

        # ── Step 4: Search ChromaDB for top-3 relevant chunks ─────────────────
        collection = get_collection()
        total_docs = collection.count()

        sources = []
        context_text = "No relevant documents found in the knowledge base."

        if total_docs > 0:
            n_results = min(3, total_docs)  # can't ask for more than what exists
            results = collection.query(
                query_embeddings=[question_embedding],
                n_results=n_results,
            )

            # Build context from the retrieved chunks
            context_parts = []
            for i, (doc, meta, doc_id) in enumerate(
                zip(
                    results["documents"][0],
                    results["metadatas"][0],
                    results["ids"][0],
                ),
                start=1,
            ):
                context_parts.append(f"[Source {i}] {doc}")
                sources.append(
                    {
                        "id": doc_id,
                        "source": meta.get("source", "Unknown"),
                        "excerpt": doc[:200] + ("..." if len(doc) > 200 else ""),
                    }
                )

            context_text = "\n\n".join(context_parts)

        # ── Step 5: Build prompt with context ─────────────────────────────────
        prompt = f"""You are an expert audit committee analyst. Use the documents below to answer the question.
If the documents do not contain relevant information, say so clearly rather than guessing.

=== RELEVANT DOCUMENTS ===
{context_text}
=== END OF DOCUMENTS ===

Question: {question}

Provide a clear, professional, and concise answer based strictly on the documents above."""

        messages = [{"role": "user", "content": prompt}]

        # ── Step 6: Call Groq ──────────────────────────────────────────────────
        answer_text, tokens, response_ms = call_groq(
            messages,
            temperature=0.3,
            max_tokens=800,
        )

        # Confidence is higher when we found relevant documents
        confidence = 0.85 if sources else 0.30

        # ── Step 7: Build, cache, and return response ──────────────────────────
        response = {
            "answer": answer_text.strip(),
            "sources": sources,
            "meta": {
                "model_used": get_model_name(),
                "tokens_used": tokens,
                "response_time_ms": response_ms,
                "cached": False,
                "confidence": confidence,
                "is_fallback": False,
            },
        }

        set_cached(cache_key, response)
        return jsonify(response), 200

    except Exception as exc:
        logger.error(f"/query failed: {exc}")
        return jsonify({**_FALLBACK_RESPONSE, "error": str(exc)}), 200
import os
import time
import logging
import redis
from flask import Blueprint, jsonify
from services.groq_client import get_avg_response_time, get_model_name
from services.chroma_client import get_doc_count
from services.redis_cache import get_cache_stats

logger = logging.getLogger(__name__)
health_bp = Blueprint("health", __name__)

# Record when the service started — used to calculate uptime
_service_start_time = time.time()


def _check_redis() -> str:
    """
    Ping Redis to see if it's reachable.
    Returns "ok" on success or an error message string on failure.
    """
    try:
        r = redis.Redis(
            host=os.getenv("REDIS_HOST", "localhost"),
            port=int(os.getenv("REDIS_PORT", 6379)),
            socket_connect_timeout=2,
        )
        r.ping()
        return "ok"
    except Exception as exc:
        return f"error: {exc}"


def _check_chroma() -> tuple[str, int]:
    """
    Check ChromaDB connectivity and return (status, doc_count).
    Returns ("ok", count) on success, ("error: ...", 0) on failure.
    """
    try:
        count = get_doc_count()
        return "ok", count
    except Exception as exc:
        return f"error: {exc}", 0


def _format_uptime(seconds: int) -> str:
    """Convert seconds to human-readable string: '2h 14m 33s'"""
    hours = seconds // 3600
    minutes = (seconds % 3600) // 60
    secs = seconds % 60
    return f"{hours}h {minutes}m {secs}s"


@health_bp.route("/health", methods=["GET"])
def health():
    """
    Return full health status of the AI service.

    Checks:
    • Groq model info and average response time (last 10 calls)
    • ChromaDB connectivity and document count
    • Redis connectivity
    • Cache hit/miss statistics
    • Service uptime
    """
    uptime_seconds = int(time.time() - _service_start_time)

    # Run all health checks
    redis_status = _check_redis()
    chroma_status, doc_count = _check_chroma()
    cache_stats = get_cache_stats()

    # Determine overall status
    # If both Redis and ChromaDB are OK → healthy
    # If one is down → degraded (still serving requests with fallback)
    # If both are down → unhealthy
    issues = sum(
        1 for s in [redis_status, chroma_status] if not s.startswith("ok")
    )
    if issues == 0:
        overall_status = "healthy"
    elif issues == 1:
        overall_status = "degraded"
    else:
        overall_status = "unhealthy"

    return jsonify(
        {
            "status": overall_status,
            "model_name": get_model_name(),
            "avg_response_time_ms": get_avg_response_time(),
            "chroma_doc_count": doc_count,
            "chroma_status": chroma_status,
            "redis_status": redis_status,
            "cache_stats": {
                "hits": cache_stats["hits"],
                "misses": cache_stats["misses"],
                "hit_rate": cache_stats["hit_rate"],
                "ttl_seconds": 900,
            },
            "uptime": _format_uptime(uptime_seconds),
            "uptime_seconds": uptime_seconds,
        }
    ), 200
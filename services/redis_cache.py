import os
import json
import hashlib
import logging
import redis
from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger(__name__)

# Cache TTL = 15 minutes (as specified in the project spec)
CACHE_TTL_SECONDS = 900

# Counters (in-memory, reset on restart)
_hit_count = 0
_miss_count = 0


def _get_redis() -> redis.Redis:
    """
    Return a Redis connection using environment variables.
    In Docker: REDIS_HOST=redis, REDIS_PORT=6379
    """
    return redis.Redis(
        host=os.getenv("REDIS_HOST", "localhost"),
        port=int(os.getenv("REDIS_PORT", 6379)),
        db=0,
        decode_responses=True,  # return strings instead of bytes
        socket_connect_timeout=2,  # fail fast if Redis is down
    )


def make_cache_key(endpoint: str, payload: dict) -> str:
    """
    Create a unique SHA256 hash that identifies this specific request.

    Parameters
    ----------
    endpoint : name of the API endpoint, e.g. "categorise"
    payload  : the request data dict, e.g. {"text": "Missing controls..."}

    Returns
    -------
    A 64-character hex string (SHA256 hash)

    Why sort_keys=True?
      {"text": "hello", "fresh": false} and {"fresh": false, "text": "hello"}
      are the same request — sorting ensures they get the same cache key.
    """
    raw_string = f"{endpoint}:{json.dumps(payload, sort_keys=True)}"
    return hashlib.sha256(raw_string.encode()).hexdigest()


def get_cached(key: str) -> dict | None:
    """
    Look up a cached response in Redis.

    Returns the cached dict if found, or None if not cached.
    Increments hit_count or miss_count accordingly.
    If Redis is down, returns None (safe fallback — just calls Groq instead).
    """
    global _hit_count, _miss_count
    try:
        r = _get_redis()
        value = r.get(f"ai_cache:{key}")
        if value:
            _hit_count += 1
            logger.debug(f"Cache HIT for key ...{key[-8:]}")
            return json.loads(value)
        _miss_count += 1
        logger.debug(f"Cache MISS for key ...{key[-8:]}")
        return None
    except Exception as exc:
        logger.warning(f"Redis get failed (will call Groq instead): {exc}")
        _miss_count += 1
        return None


def set_cached(key: str, value: dict) -> None:
    """
    Store an AI response in Redis with a 15-minute TTL.

    If Redis is down, this silently fails — the app keeps working,
    it just won't cache that response.
    """
    try:
        r = _get_redis()
        r.setex(
            name=f"ai_cache:{key}",
            time=CACHE_TTL_SECONDS,
            value=json.dumps(value),
        )
        logger.debug(f"Cached response for key ...{key[-8:]} (TTL={CACHE_TTL_SECONDS}s)")
    except Exception as exc:
        logger.warning(f"Redis set failed (response not cached): {exc}")


def get_cache_stats() -> dict:
    """
    Return hit and miss counts. Used by the /health endpoint.
    """
    return {
        "hits": _hit_count,
        "misses": _miss_count,
        "hit_rate": (
            round(_hit_count / (_hit_count + _miss_count), 2)
            if (_hit_count + _miss_count) > 0
            else 0.0
        ),
    }
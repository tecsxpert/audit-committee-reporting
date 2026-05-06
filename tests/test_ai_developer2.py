import json
import pytest
import sys
import os

# Add project root to Python path so imports work
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))


# ── Fixtures ──────────────────────────────────────────────────────────────────

@pytest.fixture
def flask_client():
    """Create a Flask test client for testing HTTP endpoints."""
    from app import create_app
    app = create_app()
    app.config["TESTING"] = True
    with app.test_client() as client:
        yield client


@pytest.fixture
def mock_groq_response():
    """
    A fake Groq API response object.
    Prevents any real network calls during tests.
    """
    from unittest.mock import MagicMock
    mock = MagicMock()
    mock.choices[0].message.content = "Test AI response"
    mock.usage.total_tokens = 42
    return mock


# ── Test 1: GroqClient returns correct tuple ──────────────────────────────────

def test_call_groq_returns_tuple(mock_groq_response):
    """
    call_groq() should return a 3-tuple: (str, int, int)
    (text, tokens_used, response_time_ms)
    """
    from unittest.mock import patch, MagicMock

    with patch("services.groq_client.Groq") as mock_groq_class:
        mock_client = MagicMock()
        mock_groq_class.return_value = mock_client
        mock_client.chat.completions.create.return_value = mock_groq_response

        from services.groq_client import call_groq
        text, tokens, ms = call_groq([{"role": "user", "content": "test"}])

        assert isinstance(text, str), "text must be a string"
        assert isinstance(tokens, int), "tokens must be an int"
        assert isinstance(ms, int), "response_time_ms must be an int"
        assert text == "Test AI response"
        assert tokens == 42


# ── Test 2: GroqClient retries on failure ─────────────────────────────────────

def test_call_groq_retries_on_failure():
    """
    call_groq() should retry 3 times before raising an exception.
    """
    from unittest.mock import patch, MagicMock
    import services.groq_client  # re-import to reset state

    with patch("services.groq_client.Groq") as mock_groq_class:
        with patch("services.groq_client.time") as mock_time:
            mock_time.time.return_value = 0.0
            mock_time.sleep = MagicMock()  # don't actually sleep in tests

            mock_client = MagicMock()
            mock_groq_class.return_value = mock_client
            mock_client.chat.completions.create.side_effect = Exception("API error")

            from services.groq_client import call_groq
            with pytest.raises(Exception, match="API error"):
                call_groq([{"role": "user", "content": "test"}], retries=3)

            # Should have been called exactly 3 times (3 retries)
            assert mock_client.chat.completions.create.call_count == 3


# ── Test 3: Cache key is deterministic ────────────────────────────────────────

def test_make_cache_key_is_deterministic():
    """
    The same endpoint + payload should always produce the same cache key.
    """
    from services.redis_cache import make_cache_key

    key1 = make_cache_key("categorise", {"text": "Missing financial controls"})
    key2 = make_cache_key("categorise", {"text": "Missing financial controls"})

    assert key1 == key2, "Same input must produce same SHA256 key"
    assert len(key1) == 64, "SHA256 hash must be 64 hex characters"


# ── Test 4: Cache key differs by input ────────────────────────────────────────

def test_make_cache_key_differs_by_input():
    """
    Different inputs must produce different cache keys.
    """
    from services.redis_cache import make_cache_key

    key1 = make_cache_key("categorise", {"text": "Missing financial controls"})
    key2 = make_cache_key("categorise", {"text": "IT system not patched"})

    assert key1 != key2, "Different inputs must produce different keys"


# ── Test 5: Cache miss returns None ───────────────────────────────────────────

def test_get_cached_returns_none_on_miss():
    """
    get_cached() must return None when the key is not in Redis.
    Redis is mocked to return None (simulating a cache miss).
    """
    from unittest.mock import patch, MagicMock

    with patch("services.redis_cache._get_redis") as mock_redis_fn:
        mock_r = MagicMock()
        mock_redis_fn.return_value = mock_r
        mock_r.get.return_value = None  # simulate cache miss

        from services.redis_cache import get_cached
        result = get_cached("nonexistent_key_12345")

        assert result is None, "Cache miss should return None"


# ── Test 6: Cache hit returns stored data ─────────────────────────────────────

def test_get_cached_returns_data_on_hit():
    """
    get_cached() must return the stored dict when the key exists in Redis.
    """
    from unittest.mock import patch, MagicMock

    stored_data = {"category": "Financial Risk", "confidence": 0.9}

    with patch("services.redis_cache._get_redis") as mock_redis_fn:
        mock_r = MagicMock()
        mock_redis_fn.return_value = mock_r
        mock_r.get.return_value = json.dumps(stored_data)  # simulate cache hit

        from services.redis_cache import get_cached
        result = get_cached("test_key_abc")

        assert result == stored_data, "Cache hit should return the stored dict"


# ── Test 7: /categorise returns 200 on valid input ───────────────────────────

def test_categorise_endpoint_success(flask_client):
    """
    POST /categorise with valid text should return 200 and the correct fields.
    Groq is mocked to return a valid JSON response.
    """
    from unittest.mock import patch

    mock_ai_json = json.dumps({
        "category": "Financial Risk",
        "confidence": 0.92,
        "reasoning": "This relates to financial reporting controls.",
    })

    with patch("routes.categorise.call_groq", return_value=(mock_ai_json, 80, 700)):
        with patch("routes.categorise.get_cached", return_value=None):
            with patch("routes.categorise.set_cached"):
                response = flask_client.post(
                    "/categorise",
                    json={"text": "Bank reconciliation was not performed for Q3 2026."},
                    content_type="application/json",
                )

    assert response.status_code == 200
    data = response.get_json()
    assert "category" in data
    assert "confidence" in data
    assert "reasoning" in data
    assert "meta" in data
    assert data["meta"]["is_fallback"] is False


# ── Test 8: /categorise returns 400 on missing input ─────────────────────────

def test_categorise_endpoint_missing_text(flask_client):
    """
    POST /categorise without a 'text' field must return HTTP 400.
    """
    response = flask_client.post(
        "/categorise",
        json={},
        content_type="application/json",
    )
    assert response.status_code == 400
    data = response.get_json()
    assert "error" in data


# ── Test 9: /generate-report async returns job_id ────────────────────────────

def test_generate_report_async_returns_job_id(flask_client):
    """
    POST /generate-report (without ?stream=true) must return 202 Accepted
    with a job_id string immediately.
    """
    from unittest.mock import patch

    mock_report = json.dumps({
        "title": "Test Report",
        "executive_summary": "Summary here.",
        "overview": "Overview here.",
        "top_items": [],
        "recommendations": [],
    })

    with patch("routes.generate_report.call_groq", return_value=(mock_report, 300, 1500)):
        with patch("routes.generate_report.get_cached", return_value=None):
            response = flask_client.post(
                "/generate-report",
                json={
                    "title": "Q2 Report",
                    "period": "Q2 2026",
                    "items": ["Missing invoice approval"],
                },
                content_type="application/json",
            )

    assert response.status_code == 202
    data = response.get_json()
    assert "job_id" in data
    assert data["status"] == "processing"
    assert len(data["job_id"]) == 36  # UUID is 36 chars


# ── Test 10: /health returns status ──────────────────────────────────────────

def test_health_endpoint_returns_status(flask_client):
    """
    GET /health must return 200 with a 'status' field.
    Redis and ChromaDB are mocked so we don't need real connections.
    """
    from unittest.mock import patch

    with patch("routes.health._check_redis", return_value="ok"):
        with patch("routes.health._check_chroma", return_value=("ok", 47)):
            response = flask_client.get("/health")

    assert response.status_code == 200
    data = response.get_json()
    assert "status" in data
    assert "model_name" in data
    assert "uptime" in data
    assert "cache_stats" in data
    assert data["status"] in ("healthy", "degraded", "unhealthy")
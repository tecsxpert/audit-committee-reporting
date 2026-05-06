import os
import json
import time
import logging
from flask import Blueprint, request, jsonify
from services.groq_client import call_groq, get_model_name
from services.redis_cache import make_cache_key, get_cached, set_cached

logger = logging.getLogger(__name__)

# Flask Blueprint — groups this endpoint under the /categorise path
categorise_bp = Blueprint("categorise", __name__)

# The 8 valid categories for this project
VALID_CATEGORIES = [
    "Financial Risk",
    "Compliance & Regulatory",
    "Operational Risk",
    "IT & Cybersecurity",
    "Governance & Ethics",
    "Fraud & Misconduct",
    "Strategic Risk",
    "Environmental & Social",
]

# This is returned if Groq API fails completely (so the app doesn't crash)
_FALLBACK_RESPONSE = {
    "category": "Operational Risk",
    "confidence": 0.0,
    "reasoning": "AI service is temporarily unavailable. This is a default classification.",
    "meta": {
        "model_used": "fallback",
        "tokens_used": 0,
        "response_time_ms": 0,
        "cached": False,
        "confidence": 0.0,
        "is_fallback": True,
    },
}


def _load_prompt(text: str) -> str:
    """
    Load the categorise prompt template from disk and fill in the audit text.
    Using a file instead of hardcoding makes it easy to tune the prompt
    without touching Python code.
    """
    prompt_path = os.path.join(
        os.path.dirname(__file__), "..", "prompts", "categorise_prompt.txt"
    )
    with open(prompt_path, "r") as f:
        template = f.read()
    return template.replace("{text}", text)


@categorise_bp.route("/categorise", methods=["POST"])
def categorise():
    """
    Classify an audit finding into one of 8 predefined categories.

    Steps:
    1. Validate input (must have 'text' field, minimum 10 chars)
    2. Check Redis cache (skip if fresh=true)
    3. Build prompt from template file
    4. Call Groq API
    5. Parse JSON response
    6. Cache result and return
    """
    # ── Step 1: Validate input ─────────────────────────────────────────────────
    data = request.get_json(silent=True)
    if not data or not data.get("text"):
        return jsonify({"error": "Request body must contain a 'text' field."}), 400

    text = str(data["text"]).strip()
    if len(text) < 10:
        return jsonify({"error": "Text is too short. Minimum 10 characters required."}), 400
    if len(text) > 5000:
        return jsonify({"error": "Text is too long. Maximum 5000 characters allowed."}), 400

    fresh = bool(data.get("fresh", False))  # skip cache if True

    # ── Step 2: Check Redis cache ──────────────────────────────────────────────
    cache_key = make_cache_key("categorise", {"text": text})
    if not fresh:
        cached = get_cached(cache_key)
        if cached:
            cached["meta"]["cached"] = True
            logger.info("Returning cached /categorise response")
            return jsonify(cached), 200

    # ── Step 3: Build prompt ───────────────────────────────────────────────────
    prompt = _load_prompt(text)
    messages = [{"role": "user", "content": prompt}]

    # ── Step 4: Call Groq API ─────────────────────────────────────────────────
    try:
        raw_text, tokens, response_ms = call_groq(
            messages,
            temperature=0.1,   # low temperature = consistent, precise categorisation
            max_tokens=250,
        )

        # ── Step 5: Parse JSON response ────────────────────────────────────────
        # Clean up the response in case Groq wraps it in ```json ... ```
        clean = raw_text.strip().replace("```json", "").replace("```", "").strip()
        parsed = json.loads(clean)

        # Ensure the category is one of our valid options
        category = parsed.get("category", "Operational Risk")
        if category not in VALID_CATEGORIES:
            logger.warning(f"AI returned unknown category '{category}'. Defaulting.")
            category = "Operational Risk"

        confidence = round(float(parsed.get("confidence", 0.5)), 2)

        response = {
            "category": category,
            "confidence": confidence,
            "reasoning": parsed.get("reasoning", ""),
            "meta": {
                "model_used": get_model_name(),
                "tokens_used": tokens,
                "response_time_ms": response_ms,
                "cached": False,
                "confidence": confidence,
                "is_fallback": False,
            },
        }

        # ── Step 6: Cache and return ───────────────────────────────────────────
        set_cached(cache_key, response)
        return jsonify(response), 200

    except json.JSONDecodeError as exc:
        logger.error(f"/categorise JSON parse error: {exc}. Raw text: {raw_text[:200]}")
        return jsonify({**_FALLBACK_RESPONSE, "error": "AI response could not be parsed"}), 200

    except Exception as exc:
        logger.error(f"/categorise failed: {exc}")
        return jsonify({**_FALLBACK_RESPONSE, "error": str(exc)}), 200
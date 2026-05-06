import os
import json
import uuid
import time
import threading
import logging
from flask import Blueprint, request, jsonify, Response, stream_with_context
from groq import Groq
from services.groq_client import call_groq, get_model_name
from services.redis_cache import make_cache_key, get_cached, set_cached

logger = logging.getLogger(__name__)
generate_report_bp = Blueprint("generate_report", __name__)

# In-memory job store: { job_id: { status, result, created_at, error } }
# In production you'd use Redis for this, but in-memory is fine for the sprint
_jobs: dict[str, dict] = {}

# Fallback report — returned when Groq API is completely unavailable
_FALLBACK_REPORT = {
    "title": "Audit Committee Report",
    "executive_summary": (
        "The AI report generation service is temporarily unavailable. "
        "Please retry the request in a few minutes."
    ),
    "overview": "Report generation is currently unavailable due to a service interruption.",
    "top_items": [],
    "recommendations": [],
    "meta": {
        "model_used": "fallback",
        "tokens_used": 0,
        "response_time_ms": 0,
        "cached": False,
        "confidence": 0.0,
        "is_fallback": True,
    },
}


# ── Prompt builder ─────────────────────────────────────────────────────────────

def _build_prompt(data: dict) -> str:
    """
    Load the report prompt template and substitute the request data.
    Limits to 20 items to keep the prompt a reasonable length.
    """
    title = data.get("title", "Audit Committee Report")
    period = data.get("period", "Current Period")
    items = data.get("items", [])[:20]  # cap at 20 items

    items_text = "\n".join(f"{i + 1}. {item}" for i, item in enumerate(items))
    if not items_text:
        items_text = "No specific audit items provided."

    prompt_path = os.path.join(
        os.path.dirname(__file__), "..", "prompts", "generate_report_prompt.txt"
    )
    with open(prompt_path, "r") as f:
        template = f.read()

    return (
        template
        .replace("{title}", title)
        .replace("{period}", period)
        .replace("{item_count}", str(len(items)))
        .replace("{items_text}", items_text)
    )


# ── Main route ─────────────────────────────────────────────────────────────────

@generate_report_bp.route("/generate-report", methods=["POST"])
def generate_report():
    """
    Generate a formal audit committee report.
    Routes to streaming or async mode based on ?stream=true query param.
    """
    data = request.get_json(silent=True)
    if not data:
        return jsonify({"error": "Request body is required."}), 400

    if not data.get("items"):
        return jsonify({"error": "Request body must contain an 'items' list."}), 400

    use_stream = request.args.get("stream", "false").lower() == "true"
    fresh = bool(data.get("fresh", False))

    # Cache only applies to async mode (streaming can't be cached)
    if not use_stream and not fresh:
        cache_key = make_cache_key("generate_report", {
            "title": data.get("title", ""),
            "period": data.get("period", ""),
            "items": data.get("items", []),
        })
        cached = get_cached(cache_key)
        if cached:
            cached["meta"]["cached"] = True
            return jsonify(cached), 200

    if use_stream:
        return _stream_report(data)
    else:
        return _async_report(data)


# ── Mode 1: SSE Streaming ──────────────────────────────────────────────────────

def _stream_report(data: dict) -> Response:
    """
    Stream the report token-by-token using Server-Sent Events (SSE).
    The React frontend reads this with EventSource and appends each token
    to the screen in real time — great for the Demo Day presentation.
    """
    def generate_tokens():
        prompt = _build_prompt(data)
        messages = [{"role": "user", "content": prompt}]

        try:
            api_key = os.getenv("GROQ_API_KEY")
            client = Groq(api_key=api_key)

            start = time.time()

            # stream=True makes Groq send tokens as they are generated
            stream = client.chat.completions.create(
                model=get_model_name(),
                messages=messages,
                temperature=0.4,
                max_tokens=1500,
                stream=True,
            )

            for chunk in stream:
                token = chunk.choices[0].delta.content
                if token:
                    # SSE format: "data: <json>\n\n"
                    yield f"data: {json.dumps({'token': token})}\n\n"

            elapsed_ms = int((time.time() - start) * 1000)
            # Send a final "done" event so the frontend knows streaming is complete
            yield f"data: {json.dumps({'done': True, 'response_time_ms': elapsed_ms})}\n\n"

        except Exception as exc:
            logger.error(f"SSE streaming error: {exc}")
            yield f"data: {json.dumps({'error': str(exc), 'is_fallback': True})}\n\n"

    return Response(
        stream_with_context(generate_tokens()),
        content_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",  # disables Nginx buffering (important!)
            "Connection": "keep-alive",
        },
    )


# ── Mode 2: Async Job Processing ───────────────────────────────────────────────

def _async_report(data: dict):
    """
    Start report generation in a background thread and return a job_id immediately.
    The client polls GET /generate-report/status/<job_id> to get the result.
    """
    job_id = str(uuid.uuid4())
    _jobs[job_id] = {
        "status": "processing",
        "result": None,
        "error": None,
        "created_at": time.time(),
    }

    def background_task():
        """Runs in a separate thread. Calls Groq and stores result in _jobs."""
        prompt = _build_prompt(data)
        messages = [{"role": "user", "content": prompt}]

        try:
            raw_text, tokens, response_ms = call_groq(
                messages, temperature=0.4, max_tokens=1500
            )

            # Clean up any markdown code fences Groq might add
            clean = raw_text.strip().replace("```json", "").replace("```", "").strip()
            result = json.loads(clean)

            result["meta"] = {
                "model_used": get_model_name(),
                "tokens_used": tokens,
                "response_time_ms": response_ms,
                "cached": False,
                "confidence": 0.88,
                "is_fallback": False,
            }

            _jobs[job_id]["status"] = "completed"
            _jobs[job_id]["result"] = result

            # Cache for next request with the same data
            cache_key = make_cache_key("generate_report", {
                "title": data.get("title", ""),
                "period": data.get("period", ""),
                "items": data.get("items", []),
            })
            set_cached(cache_key, result)

            # Webhook callback — notifies the caller when the report is ready
            webhook_url = data.get("webhook_url")
            if webhook_url:
                try:
                    import requests as req_lib
                    req_lib.post(
                        webhook_url,
                        json={"job_id": job_id, "status": "completed", "result": result},
                        timeout=5,
                    )
                    logger.info(f"Webhook sent to {webhook_url} for job {job_id}")
                except Exception as wh_exc:
                    logger.warning(f"Webhook failed for job {job_id}: {wh_exc}")

        except json.JSONDecodeError as exc:
            logger.error(f"Job {job_id}: JSON parse failed: {exc}")
            _jobs[job_id]["status"] = "failed"
            _jobs[job_id]["result"] = _FALLBACK_REPORT
            _jobs[job_id]["error"] = "AI response could not be parsed as JSON"

        except Exception as exc:
            logger.error(f"Job {job_id}: failed: {exc}")
            _jobs[job_id]["status"] = "failed"
            _jobs[job_id]["result"] = _FALLBACK_REPORT
            _jobs[job_id]["error"] = str(exc)

    # Start background thread (daemon=True so it won't block app shutdown)
    thread = threading.Thread(target=background_task, daemon=True)
    thread.start()

    return jsonify(
        {
            "job_id": job_id,
            "status": "processing",
            "message": (
                f"Report generation started. "
                f"Poll GET /generate-report/status/{job_id} for the result."
            ),
        }
    ), 202


@generate_report_bp.route("/generate-report/status/<job_id>", methods=["GET"])
def get_job_status(job_id: str):
    """
    Poll this endpoint to check if an async report job has finished.

    Returns:
      202  — still processing
      200  — completed (result included) or failed (fallback included)
      404  — job_id not found
    """
    job = _jobs.get(job_id)
    if not job:
        return jsonify({"error": f"No job found with id '{job_id}'"}), 404

    if job["status"] == "completed":
        return jsonify(
            {"job_id": job_id, "status": "completed", "result": job["result"]}
        ), 200

    if job["status"] == "failed":
        return jsonify(
            {
                "job_id": job_id,
                "status": "failed",
                "error": job["error"],
                "result": job["result"],   # fallback report
            }
        ), 200

    # Still processing
    elapsed = int(time.time() - job["created_at"])
    return jsonify(
        {
            "job_id": job_id,
            "status": "processing",
            "elapsed_seconds": elapsed,
            "message": "Still generating. Check back in a few seconds.",
        }
    ), 202
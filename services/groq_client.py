import os
import time
import logging
from groq import Groq
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

# The AI model we use in this project (free tier on Groq)
MODEL_NAME = "llama-3.3-70b-versatile"

# Store the last 10 response times in milliseconds.
# This list is used by the /health endpoint to report average speed.
_response_times: list[int] = []


def call_groq(
    messages: list[dict],
    temperature: float = 0.3,
    max_tokens: int = 1000,
    retries: int = 3,
) -> tuple[str, int, int]:
    """
    Send a list of messages to the Groq LLM and return the reply.

    Parameters
    ----------
    messages    : list of dicts like [{"role": "user", "content": "..."}]
    temperature : 0.0 = very precise/factual, 1.0 = very creative.
                  Use 0.1–0.3 for JSON outputs, 0.4–0.7 for reports/summaries.
    max_tokens  : maximum number of tokens the AI can write back (1 token ≈ 0.75 words)
    retries     : how many times to retry on failure before giving up

    Returns
    -------
    (text, tokens_used, response_time_ms)
      text              – the AI's reply as a string
      tokens_used       – total tokens consumed (prompt + completion)
      response_time_ms  – how long the API call took in milliseconds

    Raises
    ------
    Exception if all retries are exhausted
    """
    api_key = os.getenv("GROQ_API_KEY")
    if not api_key:
        raise ValueError("GROQ_API_KEY is not set in environment variables")

    client = Groq(api_key=api_key)
    last_exception = None

    for attempt in range(retries):
        try:
            start_time = time.time()

            response = client.chat.completions.create(
                model=MODEL_NAME,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens,
            )

            elapsed_ms = int((time.time() - start_time) * 1000)

            # Keep only the last 10 response times (for /health stats)
            _response_times.append(elapsed_ms)
            if len(_response_times) > 10:
                _response_times.pop(0)

            text = response.choices[0].message.content
            tokens = response.usage.total_tokens

            logger.info(
                f"Groq call succeeded | attempt={attempt + 1} | "
                f"tokens={tokens} | time={elapsed_ms}ms"
            )
            return text, tokens, elapsed_ms

        except Exception as exc:
            last_exception = exc
            wait_seconds = 2 ** attempt  # 1s, 2s, 4s
            logger.error(
                f"Groq call failed (attempt {attempt + 1}/{retries}): {exc}. "
                f"Retrying in {wait_seconds}s..."
            )
            if attempt < retries - 1:
                time.sleep(wait_seconds)

    # All retries exhausted — raise so callers can return fallback response
    raise last_exception


def get_model_name() -> str:
    """Return the name of the AI model being used."""
    return MODEL_NAME


def get_avg_response_time() -> int:
    """
    Return the average of the last 10 Groq API response times in milliseconds.
    Returns 0 if no calls have been made yet.
    """
    if not _response_times:
        return 0
    return int(sum(_response_times) / len(_response_times))
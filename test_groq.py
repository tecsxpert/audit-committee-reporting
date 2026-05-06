"""
test_groq.py  — DAY 1 TASK (AI Developer 2)
--------------------------------------------
Run this FIRST to confirm your Groq API key is valid and working.

Usage:
    python test_groq.py

What it does:
    1. Reads GROQ_API_KEY from your .env file
    2. Sends a simple message to the LLaMA-3.3-70b model
    3. Prints the response so you can see it is working
"""

import os
import sys
from dotenv import load_dotenv

# Load environment variables from .env
load_dotenv()


def test_groq_connection():
    """
    Send a test message to Groq API and confirm it replies.
    This uses the same model the whole project will use.
    """
    api_key = os.getenv("GROQ_API_KEY")
    if not api_key:
        print("❌  GROQ_API_KEY not found in .env file!")
        print("   → Go to https://console.groq.com, create an API key, and add it to .env")
        sys.exit(1)

    try:
        from groq import Groq
        client = Groq(api_key=api_key)

        print("🔄  Calling Groq API with model llama-3.3-70b-versatile ...")
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {
                    "role": "user",
                    "content": (
                        "You are an audit assistant. "
                        "Reply with exactly one sentence confirming you are ready."
                    ),
                }
            ],
            max_tokens=50,
            temperature=0.3,
        )

        reply = response.choices[0].message.content
        tokens = response.usage.total_tokens

        print(f"✅  Groq API is working!")
        print(f"   Model reply : {reply}")
        print(f"   Tokens used : {tokens}")
        return True

    except Exception as e:
        print(f"❌  Groq API call failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    test_groq_connection()
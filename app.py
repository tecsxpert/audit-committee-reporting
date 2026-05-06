"""
app.py  — Flask entry point for the AI microservice
----------------------------------------------------
This file starts the Flask app and registers all route blueprints.

How to run (development):
    python app.py

How to run (production via Docker):
    gunicorn --bind 0.0.0.0:5000 --timeout 120 --workers 2 "app:create_app()"

Health check:
    curl http://localhost:5000/health
"""

import logging
from flask import Flask
from flask_cors import CORS
from dotenv import load_dotenv

# Load .env file FIRST before anything else
load_dotenv()

# Configure logging so all modules write to the same format
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


def create_app() -> Flask:
    """
    Application factory pattern.
    Creates and configures the Flask app, then returns it.
    Gunicorn calls this function: gunicorn "app:create_app()"
    """
    app = Flask(__name__)

    # Allow requests from the React frontend (port 80) and Java backend (port 8080)
    CORS(app, origins=["http://localhost", "http://localhost:8080", "http://localhost:3000"])

    # ── Register AI Developer 2 blueprints ────────────────────────────────────
    from routes.health import health_bp
    from routes.categorise import categorise_bp
    from routes.generate_report import generate_report_bp
    from routes.query import query_bp

    app.register_blueprint(health_bp)
    app.register_blueprint(categorise_bp)
    app.register_blueprint(generate_report_bp)
    app.register_blueprint(query_bp)

    # ── Register AI Developer 1 blueprints ────────────────────────────────────
    # Uncomment each one as AI Developer 1 completes their endpoints:
    # from routes.describe import describe_bp
    # from routes.recommend import recommend_bp
    # from routes.analyse_document import analyse_document_bp
    # from routes.batch_process import batch_process_bp
    # app.register_blueprint(describe_bp)
    # app.register_blueprint(recommend_bp)
    # app.register_blueprint(analyse_document_bp)
    # app.register_blueprint(batch_process_bp)

    # ── Register AI Developer 3 middleware ────────────────────────────────────
    # Uncomment when AI Developer 3 completes their sanitisation middleware:
    # from middleware.sanitise import register_sanitise_middleware
    # register_sanitise_middleware(app)

    # ── Pre-load the embedding model at startup ────────────────────────────────
    # Loading sentence-transformers takes ~30 seconds on first import.
    # Doing it here means the FIRST real request won't be slow.
    logger.info("Pre-loading sentence-transformers embedding model...")
    try:
        from routes.query import get_embedding_model
        get_embedding_model()
        logger.info("Embedding model ready.")
    except Exception as exc:
        logger.warning(f"Could not pre-load embedding model: {exc}")

    logger.info("Flask AI service started. All blueprints registered.")
    logger.info("Available endpoints:")
    logger.info("  GET  /health")
    logger.info("  POST /categorise")
    logger.info("  POST /generate-report")
    logger.info("  POST /generate-report?stream=true")
    logger.info("  GET  /generate-report/status/<job_id>")
    logger.info("  POST /query")

    return app


if __name__ == "__main__":
    flask_app = create_app()
    flask_app.run(host="0.0.0.0", port=5000, debug=False)
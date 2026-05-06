import os
import logging
import chromadb
from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger(__name__)

# Module-level singletons (created once, reused on every request)
_client = None
_collection = None

COLLECTION_NAME = "audit_documents"


def get_chroma_client() -> chromadb.PersistentClient:
    """
    Return the ChromaDB client (singleton pattern).
    Creates it on the first call, reuses it afterwards.
    Data is saved to the path in CHROMA_PATH env var (default: ./chroma_data).
    """
    global _client
    if _client is None:
        chroma_path = os.getenv("CHROMA_PATH", "./chroma_data")
        _client = chromadb.PersistentClient(path=chroma_path)
        logger.info(f"ChromaDB client created. Storing data at: {chroma_path}")
    return _client


def get_collection() -> chromadb.Collection:
    """
    Return the 'audit_documents' collection (singleton pattern).
    Creates it if it doesn't exist yet.
    Uses cosine similarity — best for finding semantically similar text.
    """
    global _collection
    if _collection is None:
        client = get_chroma_client()
        _collection = client.get_or_create_collection(
            name=COLLECTION_NAME,
            metadata={"hnsw:space": "cosine"},  # cosine = good for text similarity
        )
        logger.info(
            f"ChromaDB collection '{COLLECTION_NAME}' ready. "
            f"Documents stored: {_collection.count()}"
        )
    return _collection


def get_doc_count() -> int:
    """
    Return how many documents are stored in ChromaDB.
    Used by the /health endpoint to report database status.
    Returns 0 if ChromaDB is unreachable.
    """
    try:
        return get_collection().count()
    except Exception as exc:
        logger.error(f"ChromaDB count error: {exc}")
        return 0


# ── Quick self-test ────────────────────────────────────────────────────────────
# Run this file directly to verify ChromaDB is working:
#   python services/chroma_client.py

if __name__ == "__main__":
    from sentence_transformers import SentenceTransformer

    print("🔄  Testing ChromaDB setup ...")
    model = SentenceTransformer("all-MiniLM-L6-v2")
    collection = get_collection()

    # Add a test document
    test_text = "The audit committee reviewed internal controls for financial reporting."
    embedding = model.encode(test_text).tolist()

    collection.upsert(
        ids=["test_doc_1"],
        embeddings=[embedding],
        documents=[test_text],
        metadatas=[{"source": "test", "type": "audit_finding"}],
    )
    print(f"✅  Stored test document. Total docs: {collection.count()}")

    # Query for it
    query_text = "financial audit internal controls"
    query_embedding = model.encode(query_text).tolist()
    results = collection.query(query_embeddings=[query_embedding], n_results=1)

    print(f"✅  Query returned: {results['documents'][0][0][:80]}...")
    print("✅  ChromaDB is working correctly!")
import os
os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"

from fastapi import FastAPI, Request
from sentence_transformers import SentenceTransformer
import uvicorn

app = FastAPI(title="Note-lite Local Embedding")
MODEL_NAME = "BAAI/bge-small-zh-v1.5"

print(f"Loading model: {MODEL_NAME} ...")
model = SentenceTransformer(MODEL_NAME)
print(f"Model loaded. Dimension: {model.get_embedding_dimension()}")

async def do_embed(request: Request):
    body = await request.json()
    texts = body["input"]
    if isinstance(texts, str):
        texts = [texts]
    vectors = model.encode(texts, normalize_embeddings=True).tolist()
    return {
        "object": "list",
        "model": body.get("model", MODEL_NAME),
        "data": [{"object": "embedding", "index": i, "embedding": v} for i, v in enumerate(vectors)],
    }

@app.post("/v1/embeddings")
async def embeddings_v1(request: Request):
    return await do_embed(request)

@app.post("/embeddings")
async def embeddings(request: Request):
    return await do_embed(request)

@app.get("/health")
async def health():
    return {"status": "ok"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8081)

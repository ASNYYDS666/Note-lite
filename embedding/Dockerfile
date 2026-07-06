FROM python:3.11-slim
WORKDIR /app
RUN pip install --no-cache-dir fastapi uvicorn sentence-transformers
COPY embedding_server.py .
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8081/health')"
ENTRYPOINT ["python", "embedding_server.py"]

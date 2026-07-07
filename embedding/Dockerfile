FROM python:3.11-slim
WORKDIR /app

# 先装 CPU 版 PyTorch（~200MB），避免拉无用的 CUDA 库（~3GB）
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple \
      torch --extra-index-url https://download.pytorch.org/whl/cpu && \
    pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple \
      fastapi uvicorn sentence-transformers

COPY embedding_server.py .
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8081/health')"
ENTRYPOINT ["python", "embedding_server.py"]

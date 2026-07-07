FROM python:3.11-slim
WORKDIR /app

# 1. 从清华镜像安装 torch 的全部依赖（不装 torch 本身）
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple \
      filelock typing-extensions sympy networkx jinja2 fsspec mpmath markupsafe numpy

# 2. 仅从 pytorch.org 下载 CPU 版 torch 本体（~192MB，不拉依赖）
RUN pip install --no-cache-dir --no-deps --index-url https://download.pytorch.org/whl/cpu torch

# 3. 从清华镜像安装其余包（sentence-transformers 检测到 torch 已装，不会重复拉）
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple \
      fastapi uvicorn sentence-transformers

COPY embedding_server.py .
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8081/health')"
ENTRYPOINT ["python", "embedding_server.py"]

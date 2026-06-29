FROM python:3.12-slim

# Install physical dependencies required for headless OpenCV matrix rendering
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 1. Create a non-root user named 'appuser'
RUN useradd -m appuser

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# 2. Grant the new user ownership of the application files
RUN chown -R appuser:appuser /app

# 3. Switch to the non-root user before running the application
USER appuser

CMD ["python", "inference_edge.py"]

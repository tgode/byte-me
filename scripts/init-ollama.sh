#!/bin/sh
# init-ollama.sh
# Waits for Ollama to be ready and pulls the required models.
# This script is run inside the ollama-init container, but can also be run manually
# against a local Ollama instance.

OLLAMA_HOST="${OLLAMA_HOST:-http://localhost:11434}"
MODELS="nomic-embed-text qwen3:14b"

wait_for_ollama() {
  echo "Waiting for Ollama at $OLLAMA_HOST ..."
  for i in $(seq 1 30); do
    if curl -sf "$OLLAMA_HOST/api/tags" > /dev/null 2>&1; then
      echo "Ollama is ready."
      return 0
    fi
    echo "  Attempt $i/30 — retrying in 5s..."
    sleep 5
  done
  echo "ERROR: Ollama did not become ready in time."
  exit 1
}

pull_models() {
  for model in $MODELS; do
    echo "Pulling model: $model"
    curl -sf -X POST "$OLLAMA_HOST/api/pull" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"$model\"}" \
      | tail -1
    echo "Done: $model"
  done
}

wait_for_ollama
pull_models
echo "All models are ready."

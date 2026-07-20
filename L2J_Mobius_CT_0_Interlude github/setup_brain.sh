#!/usr/bin/env bash
#
# One-step setup + launch for the FPC "brain" (local LLM via Ollama).
#
# What it does (idempotent - safe to re-run):
#   1. Installs Ollama if it isn't already installed.
#   2. Makes sure the Ollama service is running.
#   3. Pulls the chat model (default: llama3.1).
#   4. Creates a Python virtualenv and installs requirements.
#   5. Writes a local .env (PROVIDER=ollama) if one doesn't exist.
#   6. Starts fpc_brain.py.
#
# Usage:
#   ./setup_brain.sh                 # use default model (llama3.1)
#   OLLAMA_MODEL=gemma3 ./setup_brain.sh
#
set -euo pipefail

cd "$(dirname "$0")"

MODEL="${OLLAMA_MODEL:-llama3.1}"
PYTHON="${PYTHON:-python3}"

say() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m[!] %s\033[0m\n' "$*"; }

# --- 1. Ollama -------------------------------------------------------------
if ! command -v ollama >/dev/null 2>&1; then
    say "Ollama not found. Installing..."
    if [[ "$(uname)" == "Darwin" ]]; then
        if command -v brew >/dev/null 2>&1; then
            brew install ollama
        else
            warn "Homebrew not found. Please install Ollama from https://ollama.com/download and re-run."
            exit 1
        fi
    else
        curl -fsSL https://ollama.com/install.sh | sh
    fi
else
    say "Ollama already installed: $(ollama --version 2>/dev/null | head -1)"
fi

# --- 2. Make sure the Ollama server is up ----------------------------------
if ! curl -fsS http://127.0.0.1:11434/api/tags >/dev/null 2>&1; then
    say "Starting Ollama server in the background..."
    nohup ollama serve >/tmp/ollama-serve.log 2>&1 &
    # Wait up to ~30s for it to answer.
    for i in $(seq 1 30); do
        if curl -fsS http://127.0.0.1:11434/api/tags >/dev/null 2>&1; then break; fi
        sleep 1
    done
fi
if ! curl -fsS http://127.0.0.1:11434/api/tags >/dev/null 2>&1; then
    warn "Ollama server did not come up. Check /tmp/ollama-serve.log"
    exit 1
fi
say "Ollama server is up."

# --- 3. Pull the model -----------------------------------------------------
say "Pulling model '$MODEL' (first run downloads several GB, this can take a while)..."
ollama pull "$MODEL"

# --- 4. Python env + deps --------------------------------------------------
if [[ ! -d .venv ]]; then
    say "Creating Python virtualenv (.venv)..."
    "$PYTHON" -m venv .venv
fi
# shellcheck disable=SC1091
source .venv/bin/activate
say "Installing Python requirements..."
pip install --quiet --upgrade pip
pip install --quiet -r requirements.txt

# --- 5. Local .env ---------------------------------------------------------
if [[ ! -f .env ]]; then
    say "Writing a local .env (PROVIDER=ollama)..."
    cat > .env <<EOF
PROVIDER=ollama
OLLAMA_MODEL=$MODEL
DEEPSEEK_API_KEY=
EOF
else
    say "Keeping your existing .env (not overwriting)."
fi

# --- 6. Launch -------------------------------------------------------------
say "Starting the FPC brain on http://127.0.0.1:5000 ..."
exec "$PYTHON" fpc_brain.py

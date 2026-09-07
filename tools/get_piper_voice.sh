#!/usr/bin/env bash
# Fetch the Piper Armenian voice used by gen_voices.py.
#
# 63 MB, not committed. It is a build tool, not a build input — the OGGs it
# produces are what ship, and those are in the repo.
set -euo pipefail
cd "$(dirname "$0")/voices"
base="https://huggingface.co/rhasspy/piper-voices/resolve/main/hy/hy_AM/gor/medium"
for f in hy_AM-gor-medium.onnx hy_AM-gor-medium.onnx.json; do
  [ -f "$f" ] || curl -fL -o "$f" "$base/$f"
done
echo "piper voice ready in $(pwd)"

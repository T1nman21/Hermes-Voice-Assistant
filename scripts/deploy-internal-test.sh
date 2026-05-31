#!/bin/bash
# Deploy to Google Play internal testing track
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/deploy-playstore.sh --track internal

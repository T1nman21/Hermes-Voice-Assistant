#!/bin/bash
# Deploy to Google Play beta track
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/deploy-playstore.sh --track beta

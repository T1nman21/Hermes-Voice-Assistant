#!/bin/bash
# Deploy to Google Play production track
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/deploy-playstore.sh --track production

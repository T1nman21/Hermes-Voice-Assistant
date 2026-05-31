#!/usr/bin/env bash
#
# deploy-playstore.sh — One-click build & upload to Google Play
#
# Prerequisites:
#   1. brew install fastlane        (or: gem install fastlane)
#   2. A Google Play Service Account JSON key (see setup below)
#   3. A release keystore (this script can generate one)
#
# Usage:
#   ./scripts/deploy-playstore.sh              # build + upload to internal track
#   ./scripts/deploy-playstore.sh --track beta  # upload to beta
#   ./scripts/deploy-playstore.sh --track production
#   ./scripts/deploy-playstore.sh --setup      # first-time setup wizard
#
set -euo pipefail
cd "$(dirname "$0")/.."

# ─── Config ───────────────────────────────────────────────────────────
PACKAGE_NAME="com.xnu.rocky"
KEYSTORE_DIR="keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/openrocky-release.jks"
KEYSTORE_PROPS="$KEYSTORE_DIR/keystore.properties"
PLAY_KEY_FILE="$KEYSTORE_DIR/play-service-account.json"
AAB_OUTPUT="app/build/outputs/bundle/standardRelease/rocky-standard-release.aab"
TRACK="internal"  # default track

# ─── Parse args ───────────────────────────────────────────────────────
RUN_SETUP=false
while [[ $# -gt 0 ]]; do
    case "$1" in
        --track)   TRACK="$2"; shift 2 ;;
        --setup)   RUN_SETUP=true; shift ;;
        -h|--help)
            echo "Usage: $0 [--track internal|alpha|beta|production] [--setup]"
            exit 0 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# ─── Colors ───────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}▸${NC} $1"; }
ok()    { echo -e "${GREEN}✓${NC} $1"; }
warn()  { echo -e "${YELLOW}!${NC} $1"; }
fail()  { echo -e "${RED}✗${NC} $1"; exit 1; }

# ─── Setup wizard ─────────────────────────────────────────────────────
setup() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════${NC}"
    echo -e "${CYAN}   OpenRocky — Google Play Setup Wizard${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════${NC}"
    echo ""

    mkdir -p "$KEYSTORE_DIR"

    # Step 1: Generate keystore
    if [[ ! -f "$KEYSTORE_FILE" ]]; then
        info "Step 1/3: Generate release keystore"
        echo ""
        read -rp "  Key alias [openrocky]: " KEY_ALIAS
        KEY_ALIAS=${KEY_ALIAS:-openrocky}
        read -rsp "  Keystore password: " KS_PASS; echo ""
        read -rsp "  Key password (Enter = same as keystore): " KEY_PASS; echo ""
        KEY_PASS=${KEY_PASS:-$KS_PASS}
        read -rp "  Your name (CN) [OpenRocky]: " CN
        CN=${CN:-OpenRocky}

        keytool -genkeypair \
            -v \
            -keystore "$KEYSTORE_FILE" \
            -alias "$KEY_ALIAS" \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -storepass "$KS_PASS" \
            -keypass "$KEY_PASS" \
            -dname "CN=$CN, OU=Mobile, O=OpenRocky, L=, S=, C=US"

        # Save properties
        cat > "$KEYSTORE_PROPS" <<PROPS
KEYSTORE_PASSWORD=$KS_PASS
KEY_ALIAS=$KEY_ALIAS
KEY_PASSWORD=$KEY_PASS
PROPS
        ok "Keystore created: $KEYSTORE_FILE"
        warn "IMPORTANT: Back up $KEYSTORE_DIR/ securely. Lost keystore = cannot update app."
    else
        ok "Keystore already exists: $KEYSTORE_FILE"
    fi
    echo ""

    # Step 2: Google Play service account
    info "Step 2/3: Google Play Service Account"
    if [[ ! -f "$PLAY_KEY_FILE" ]]; then
        echo ""
        echo "  To upload to Google Play, you need a service account JSON key."
        echo ""
        echo "  Steps:"
        echo "    1. Go to Google Play Console → Setup → API access"
        echo "    2. Link to a Google Cloud project"
        echo "    3. Create a Service Account with 'Release Manager' role"
        echo "    4. Download the JSON key file"
        echo "    5. Grant access in Play Console → Users & permissions"
        echo ""
        read -rp "  Path to JSON key file (or press Enter to skip): " JSON_PATH
        if [[ -n "$JSON_PATH" && -f "$JSON_PATH" ]]; then
            cp "$JSON_PATH" "$PLAY_KEY_FILE"
            ok "Service account key saved"
        else
            warn "Skipped. Place your key at: $PLAY_KEY_FILE"
        fi
    else
        ok "Service account key found: $PLAY_KEY_FILE"
    fi
    echo ""

    # Step 3: Install fastlane
    info "Step 3/3: Check fastlane"
    if ! command -v fastlane &>/dev/null; then
        echo ""
        read -rp "  Install fastlane? (brew install fastlane) [Y/n]: " INSTALL_FL
        if [[ "${INSTALL_FL:-Y}" =~ ^[Yy] ]]; then
            brew install fastlane
            ok "fastlane installed"
        else
            warn "Install fastlane manually: brew install fastlane"
        fi
    else
        ok "fastlane found: $(fastlane --version 2>/dev/null | head -1)"
    fi

    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
    echo -e "${GREEN}   Setup complete! Run: ./scripts/deploy-playstore.sh${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
    echo ""
}

if $RUN_SETUP; then
    setup
    exit 0
fi

# ─── Preflight checks ────────────────────────────────────────────────
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════${NC}"
echo -e "${CYAN}   OpenRocky → Google Play ($TRACK)${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════${NC}"
echo ""

# Check keystore
[[ -f "$KEYSTORE_FILE" ]] || fail "Keystore not found. Run: $0 --setup"
[[ -f "$KEYSTORE_PROPS" ]] || fail "Keystore properties not found. Run: $0 --setup"

# Load keystore properties
source "$KEYSTORE_PROPS"
export KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD

# Check service account key
[[ -f "$PLAY_KEY_FILE" ]] || fail "Service account key not found at $PLAY_KEY_FILE. Run: $0 --setup"

# Check fastlane
command -v fastlane &>/dev/null || fail "fastlane not found. Run: brew install fastlane"

# ─── Auto-increment version ──────────────────────────────────────────
# Read current version from gradle.properties or default
CURRENT_CODE=$(grep -oP 'VERSION_CODE=\K\d+' gradle.properties 2>/dev/null || echo "1")
CURRENT_NAME=$(grep -oP 'VERSION_NAME=\K.+' gradle.properties 2>/dev/null || echo "1.0.0")
NEW_CODE=$((CURRENT_CODE + 1))

info "Version: $CURRENT_NAME (code $CURRENT_CODE → $NEW_CODE)"

# Update version in gradle.properties
if grep -q "VERSION_CODE=" gradle.properties 2>/dev/null; then
    sed -i '' "s/VERSION_CODE=.*/VERSION_CODE=$NEW_CODE/" gradle.properties
else
    echo "VERSION_CODE=$NEW_CODE" >> gradle.properties
fi
if ! grep -q "VERSION_NAME=" gradle.properties 2>/dev/null; then
    echo "VERSION_NAME=$CURRENT_NAME" >> gradle.properties
fi

# ─── Build release AAB ────────────────────────────────────────────────
info "Building release AAB…"
./gradlew bundleStandardRelease \
    -PKEYSTORE_PASSWORD="$KEYSTORE_PASSWORD" \
    -PKEY_ALIAS="$KEY_ALIAS" \
    -PKEY_PASSWORD="$KEY_PASSWORD" \
    -PVERSION_CODE="$NEW_CODE" \
    -PVERSION_NAME="$CURRENT_NAME" \
    --no-daemon \
    --quiet

[[ -f "$AAB_OUTPUT" ]] || fail "AAB not found at $AAB_OUTPUT"
AAB_SIZE=$(du -h "$AAB_OUTPUT" | cut -f1)
ok "AAB built: $AAB_OUTPUT ($AAB_SIZE)"

# ─── Upload to Google Play ────────────────────────────────────────────
info "Uploading to Google Play ($TRACK track)…"

fastlane supply \
    --aab "$AAB_OUTPUT" \
    --track "$TRACK" \
    --package_name "$PACKAGE_NAME" \
    --json_key "$PLAY_KEY_FILE" \
    --skip_upload_metadata \
    --skip_upload_changelogs \
    --skip_upload_images \
    --skip_upload_screenshots

ok "Upload complete!"

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
echo -e "${GREEN}   ✓ OpenRocky v${CURRENT_NAME} (${NEW_CODE}) → $TRACK${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
echo ""
echo "  Next steps:"
if [[ "$TRACK" == "internal" ]]; then
    echo "    • Open Google Play Console to manage internal testers"
    echo "    • Promote to alpha/beta: $0 --track beta"
elif [[ "$TRACK" == "beta" ]]; then
    echo "    • Promote to production: $0 --track production"
elif [[ "$TRACK" == "production" ]]; then
    echo "    • Monitor rollout in Google Play Console"
fi
echo ""

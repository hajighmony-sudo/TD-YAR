#!/usr/bin/env bash
# Generate the Flutter add-to-app module from the canonical Dart source package.
# Run from the repository root before invoking Gradle locally.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODULE_DIR="$ROOT/flutter_module"
SOURCE_DIR="$ROOT/flutter_fx/lib"

if ! command -v flutter >/dev/null 2>&1; then
  echo "Flutter SDK is required. Install Flutter stable and retry." >&2
  exit 1
fi

if [[ ! -f "$MODULE_DIR/.android/include_flutter.groovy" ]]; then
  mkdir -p "$MODULE_DIR"
  flutter create --template module --project-name tradenex_tx_ai "$MODULE_DIR"
fi

mkdir -p "$MODULE_DIR/lib"
cp "$SOURCE_DIR"/*.dart "$MODULE_DIR/lib/"

cat > "$MODULE_DIR/pubspec.yaml" <<'YAML'
name: tradenex_tx_ai
description: TX AI Flutter add-to-app module for TRADIX.
publish_to: none
version: 0.1.0

environment:
  sdk: ">=3.4.0 <4.0.0"
  flutter: ">=3.22.0"

dependencies:
  flutter:
    sdk: flutter
  ffi: ^2.1.4

dev_dependencies:
  flutter_test:
    sdk: flutter

flutter:
  uses-material-design: true

module:
  androidX: true
YAML

(
  cd "$MODULE_DIR"
  flutter pub get
)

echo "Flutter module ready at $MODULE_DIR"

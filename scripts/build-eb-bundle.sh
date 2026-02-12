#!/usr/bin/env bash
# Build the app and create an Elastic Beanstalk deployment zip that includes:
# - JAR (renamed to match Procfile)
# - .platform/ (nginx proxy config)
# - .ebextensions/ (PORT 8080 etc.)
# - Procfile
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_ZIP="${1:-$ROOT_DIR/eb-deployment.zip}"

cd "$ROOT_DIR"

echo "Building JAR..."
mvn -q clean package -DskipTests

JAR_PATH="target/data-embed-ingestion-service-1.0.0-SNAPSHOT.jar"
if [[ ! -f "$JAR_PATH" ]]; then
  echo "Error: JAR not found at $JAR_PATH"
  exit 1
fi

BUNDLE_DIR="$ROOT_DIR/.eb-bundle"
rm -rf "$BUNDLE_DIR"
mkdir -p "$BUNDLE_DIR"

echo "Creating deployment bundle..."
cp "$JAR_PATH" "$BUNDLE_DIR/data-embed-ingestion-service.jar"
cp Procfile "$BUNDLE_DIR/"
cp -r .platform "$BUNDLE_DIR/"
cp -r .ebextensions "$BUNDLE_DIR/"

cd "$BUNDLE_DIR"
# List each item explicitly so dot dirs (.platform, .ebextensions) are always included
# (some zip versions skip names starting with . when using "zip -r file.zip .")
zip -r "$OUTPUT_ZIP" data-embed-ingestion-service.jar Procfile .platform .ebextensions -x "*.DS_Store"
cd "$ROOT_DIR"
rm -rf "$BUNDLE_DIR"

echo "Done. Deployment package: $OUTPUT_ZIP"
echo "Upload this file to Elastic Beanstalk (Application versions) or use: eb deploy"

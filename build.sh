#! /bin/sh

set -e

cd "$(dirname "$0")"

yarn --version || (echo "Yarn is not installed, trying to install it" && npm install -g yarn)

echo "Pull the latest firefox-profiler version"
(
  cd firefox-profiler
  (git fetch origin main && git reset --hard origin/main) > /dev/null || true
)
echo "Install dependencies of firefox-profiler if needed"
(
  cd firefox-profiler
  yarn install
)

echo "Build the profiler"
(
  cd firefox-profiler
  yarn build
)
echo "Copy the profiler to the src/main/resources/fp folder"
rm -rf src/main/resources/fp
mkdir -p src/main/resources/fp
cp -r firefox-profiler/dist/* src/main/resources/fp
echo "gradle assemble for good measure"
./gradlew assemble

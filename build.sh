#! /bin/sh

set -e

  echo "pwdpwdpwd"
  pwd
  ls -alsh .

cd "$(dirname "$0")"
  ls -alsh firefox-profiler

yarn --version || (echo "Yarn is not installed, trying to install it" && npm install -g yarn)
  echo "pwdpwdpwd"
  pwd
echo "Pull the latest firefox-profiler version"
(
  cd firefox-profiler
  (git fetch origin main && git reset --hard origin/main) > /dev/null || true
)
    echo "pwdpwdpwd"
    pwd
echo "Install dependencies of firefox-profiler if needed"
(
  cd firefox-profiler
  yarn install
)
    echo "pwdpwdpwd"
    pwd
echo "Build the profiler"
(
  echo "pwdpwdpwd"
  pwd
  cd firefox-profiler
  echo "lsslsls"
  echo "pwdpwdpwd"
  pwd
  yarn build
)
echo "Copy the profiler to the src/main/resources/fp folder"
rm -rf src/main/resources/fp
cp -r firefox-profiler/dist src/main/resources/fp
echo "gradle assemble for good measure"
./gradlew assemble
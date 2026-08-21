#!/bin/sh
set -eu

# Main was developed against these Mulan PSL v2 SmartJavaAI 1.1.0 artifacts.
# Seed the ephemeral Maven repository with their exact checked copies because
# the same coordinates currently resolve to incompatible public binaries.
mkdir -p "${HOME}/.m2/repository/cn/smartjavaai"
cp -R release-compat/maven-repository/cn/smartjavaai/. \
  "${HOME}/.m2/repository/cn/smartjavaai/"

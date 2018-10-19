#!/usr/bin/env bash

set -e

sbt +clean +test +core/publishSigned +resolve/publishSigned +launch/publishSigned +application/publishSigned +client/publishSigned +server/publishSigned +boot/publishSigned sonatypeRelease
./generate_native.sh
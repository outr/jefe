#!/usr/bin/env bash

sbt +clean +test +core/publishSigned +resolve/publishSigned +launch/publishSigned +application/publishSigned +client/publishSigned +server/publishSigned +boot/publishSigned sonatypeRelease
./generate_native.sh
#!/usr/bin/env bash

set -e

sbt nativeJVM/assembly
mkdir -p bin
#cp native/native/target/scala-2.11/jefe-native-out bin/jefe
cp native/jvm/target/scala-2.12/jefe-native.jar bin/jefe.jar
#upx bin/jefe
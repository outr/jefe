#!/usr/bin/env bash

sbt nativeNative/clean nativeNative/nativeLink nativeJVM/assembly
mkdir bin
cp native/native/target/scala-2.11/jefe-native-out bin/jefe
cp native/jvm/target/scala-2.11/jefe-native.jar bin/jefe.jar
upx bin/jefe
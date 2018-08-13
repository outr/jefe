#!/usr/bin/env bash

sbt native/clean native/nativeLink
mkdir bin
cp native/target/scala-2.11/jefe-native-out bin/jefe
upx bin/jefe
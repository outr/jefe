#!/usr/bin/env bash

(cd ../; sbt "boot/run server start --blocking=true --jefe.token=example")
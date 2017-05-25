#!/usr/bin/env bash

nohup java -jar jefe-server.jar start .
tail -f nohup.out
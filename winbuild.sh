#!/usr/bin/bash

set -x # echo all commands
set -e # exit on errors

bash build.sh "powershell -command clj"

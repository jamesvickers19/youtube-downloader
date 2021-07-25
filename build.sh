#!/usr/bin/env bash

set -x # echo all commands
set -e # exit on errors

# build front-end code and put into server public resources
cd frontend
yarn build
cd ..
cp -r frontend/build/* resources/public/
##########################################################
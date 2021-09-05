#!/usr/bin/bash

set -x # echo all commands
set -e # exit on errors

# build front-end code and put into server public resources
cd frontend
yarn build
cd ..
cp -r frontend/build/* server/resources/public/
##########################################################

# get clojure command to use
clojure=""
if command -v clojure &> /dev/null
then
  echo "Using installed 'clojure'"
  clojure="clojure"
elif (( "$#" == 1 ))
then
  echo "Using provided clojure variable"
  clojure=$1
else
  echo "Need to either have 'clojure' installed or provide argument for it"
  exit 1
fi
##########################################################

# build server
cd server
$clojure -X:uberjar :jar YoutubeDownloader.jar :main-class youtube-downloader.core
##########################################################
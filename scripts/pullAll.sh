#!/bin/bash

# in LPhyBEAST/scripts

CURR_PATH="$(pwd)"
echo "Current path is $CURR_PATH"

if [[ $CURR_PATH == *LPhyBeast ]] || [[ $CURR_PATH == *LPhyBeast/ ]]; then
  echo "git pull $(pwd)"
  git pull
  cd ../beast-outercore
  echo "git pull $(pwd)"
  git pull
  cd ../linguaPhylo
  echo "git pull $(pwd)"
  git pull
fi
#!/bin/bash

# cd ~/WorkSpace/LPhyBEAST
# ./scripts/pullAll.sh

CURR_PATH="$(pwd)"
echo "Current path is $CURR_PATH"

if [[ $CURR_PATH == *LPhyBeast ]] || [[ $CURR_PATH == *LPhyBeast/ ]]; then
  # compulsory
  echo "git pull $(pwd)"
  git pull
  NEXT_PATH="../linguaPhylo/"
  echo "git pull $NEXT_PATH"
  git -C $NEXT_PATH pull
  # optional
  NEXT_PATH="../beast2/"
  if [ -d $NEXT_PATH ] ;  then
    echo "git pull $NEXT_PATH"
    git -C $NEXT_PATH pull
  fi
  NEXT_PATH="../BEASTLabs/"
  if [ -d $NEXT_PATH ] ;  then
    echo "git pull $NEXT_PATH"
    git -C $NEXT_PATH pull
  fi

fi
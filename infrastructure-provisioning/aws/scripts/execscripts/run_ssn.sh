#!/usr/bin/env bash

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

pushd `dirname $0` > /dev/null
SCRIPTPATH=`pwd -P`
popd > /dev/null

##################
# Project common
##################
PROJECT_PREFIX=docker.epmc-bdcc.projects.epam.com/dlab-aws
DOCKER_IMAGE=ssn
DOCKER_IMAGE_SOURCE_DIR=$SCRIPTPATH/../../src

##################
# Docker Common
##################
KEY_DIR=$SCRIPTPATH/keys
OVERWRITE_FILE=$SCRIPTPATH/overwrite.ini
REQUEST_ID=$RANDOM
LOG_DIR=$(pwd)

##################
# Internal vars
##################
REBUILD=false
REQUEST_ID=$RANDOM
RESPONSE_DIR=$SCRIPTPATH

##################
# Routines
##################
function update_images {
    echo "Updating base image"
    docker build --file $DOCKER_IMAGE_SOURCE_DIR/base/Dockerfile -t $PROJECT_PREFIX-base $DOCKER_IMAGE_SOURCE_DIR/base

    echo "Updating working image"
    docker build --file $DOCKER_IMAGE_SOURCE_DIR/$DOCKER_IMAGE/Dockerfile -t $PROJECT_PREFIX-$DOCKER_IMAGE $DOCKER_IMAGE_SOURCE_DIR
}

function run_docker {
    echo docker run -it \
        -v $KEY_DIR:/root/keys \
        -v $OVERWRITE_FILE:/root/conf/overwrite.ini \
        -v $RESPONSE_DIR:/response -e \
        "request_id=$REQUEST_ID" $PROJECT_PREFIX-$DOCKER_IMAGE --action $ACTION
    docker run -it \
        -v $KEY_DIR:/root/keys \
        -v $OVERWRITE_FILE:/root/conf/overwrite.ini \
        -v $RESPONSE_DIR:/response -e \
        "request_id=$REQUEST_ID" $PROJECT_PREFIX-$DOCKER_IMAGE --action $ACTION | tee -a  $LOG_DIR/${REQUEST_ID}_out.log
}

function print_help {
    echo "REQUIRED:"
    echo "-a / --action ACTION: pass command to container. E.g.: create/start/status/stop"
    echo "OPTIONAL:"
    echo "-l / --log-dir DIR: response and log directory. Default: current dir (pwd)"
    echo "-o / --overwrite-file PATH_TO_FILE: path to overwrite conf file"
    echo "-d / --key-dir DIR: path to key dir"
    echo "-s / --source-dir DIR: directory with dlab infrastructure provisioning sources"
    echo "--response_dir DIR: directory where response json will be places"
    echo "-r / --rebuild : if you need to refresh images before run"
}

while [[ $# -gt 1 ]]
do
    key="$1"

    case $key in
        -a|--action)
        ACTION="$2"
        shift # past argument
        ;;
        -s|--source-dir)
        DOCKER_IMAGE_SOURCE_DIR="$2"
        shift # past argument
        ;;
        -l|--log-dir)
        LOG_DIR="$2"
        shift # past argument
        ;;
        --response_dir)
        RESPONSE_DIR="$2"
        shift # past argument
        ;;
        -o|--overwrite-file)
        OVERWRITE_FILE="$2/overwrite.ini"
        shift # past argument
        ;;
        -d|--key-dir)
        KEY_DIR="$2"
        shift # past argument
        ;;
        -r|--rebuild)
        REBUILD=true
        ;;
        -h|--help)
        print_help
        exit
        ;;
        *)
        echo "Unknown option $1."
        print_help
        exit
        ;;
    esac
    shift
done

if [ "$REBUILD" = "true" ]
then
    update_images
fi

run_docker

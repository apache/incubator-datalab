#!/bin/bash
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

set -e

function buildFrontend(){
  # Build front-end
  cd "$WORKSPACE"/services/self-service/src/main/resources/webapp/ || exit 1
  sudo npm install gulp &&\
  sudo npm install &&\
  sudo npm run build.prod &&\
  sudo chown -R "$USER" "$WORKSPACE"/* &&\
  cd "$WORKSPACE"/ || exit 1
}

function buildServices(){
  # Build provisioning-service, security-service, self-service, billing
  mvn -DskipTests package
}

function buildDockers(){
  # Build base and ssn docker
  cd infrastructure-provisioning/src || exit 1
  sudo docker build --build-arg OS=$OS_family --build-arg CLOUD=$Cloud_provider --file base/Dockerfile -t docker.dlab-base base/
  sudo docker build --build-arg OS=$OS_family --build-arg CLOUD=$Cloud_provider --file ssn/Dockerfile -t docker.dlab-ssn .
}

function deployDlab(){
  # Prepare files for deployment
  mkdir -p "$WORKSPACE"/web_app || exit 1
  mkdir -p "$WORKSPACE"/web_app/provisioning-service/ || exit 1
  mkdir -p "$WORKSPACE"/web_app/billing/ || exit 1
  mkdir -p "$WORKSPACE"/web_app/security-service/ || exit 1
  mkdir -p "$WORKSPACE"/web_app/self-service/ || exit 1
  cp "$WORKSPACE"/services/self-service/self-service.yml "$WORKSPACE"/web_app/self-service/
  cp "$WORKSPACE"/services/self-service/target/self-service-1.0.jar "$WORKSPACE"/web_app/self-service/
  cp "$WORKSPACE"/services/provisioning-service/provisioning.yml "$WORKSPACE"/web_app/provisioning-service/
  cp "$WORKSPACE"/services/provisioning-service/target/provisioning-service-1.0.jar "$WORKSPACE"/web_app/provisioning-service/
  cp "$WORKSPACE"/services/billing/billing.yml "$WORKSPACE"/web_app/billing/
  cp "$WORKSPACE"/services/billing/target/billing-1.0.jar "$WORKSPACE"/web_app/billing/
  cp "$WORKSPACE"/services/security-service/security.yml "$WORKSPACE"/web_app/security-service/
  cp "$WORKSPACE"/services/security-service/target/security-service-1.0.jar "$WORKSPACE"/web_app/security-service/

  # Create SSN node and deploy DLab
  sudo docker run -i -v ${Key_path}${Key_name}.pem:/root/keys/${Key_name}.pem \
    -v "$WORKSPACE"/web_app:/root/web_app -e "conf_os_family=$OS_family" -e "conf_os_user=$OS_user" \
        -e "conf_cloud_provider=$Cloud_provider" -e "conf_resource=ssn" -e "aws_ssn_instance_size=t2.medium" \
        -e "aws_region=us-west-2" -e "aws_vpc_id=$VPC_id" -e "aws_subnet_id=$Subnet_id" \
        -e "aws_security_groups_ids=$Sg_ids" -e "conf_key_name=$Key_name" \
        -e "conf_service_base_name=$Infrastructure_Tag" \
        -e "aws_access_key=$Access_Key_ID" -e "aws_secret_access_key=$Secret_Access_Key" \
        docker.dlab-ssn --action "$1"
}

function terminateDlab(){
  # Drop Dlab environment with selected infrastructure tag
  sudo docker run -i -v ${Key_path}${Key_name}.pem:/root/keys/${Key_name}.pem \
    -e "aws_region=$Region" -e "conf_service_base_name=$Infrastructure_Tag" \
    -e "conf_resource=ssn" -e "aws_access_key=$Access_Key_ID" -e "aws_secret_access_key=$Secret_Access_Key" \
    docker.dlab-ssn --action "$1"
}


function print_help {
    echo "[OPTIONS]:"
    echo "--infrastructure_tag : unique name for DLab environment"
    echo "--access_key_id : AWS Access Key ID"
    echo "--secret_access_key : AWS Secret Access Key"
    echo "--region : AWS region"
    echo "--os_family : Operating system type. Available options: debian, redhat"
    echo "--cloud_provider : Where DLab should be deployed. Available options: aws"
    echo "--os_user : Name of OS user. By default for Debian - ubuntu, RedHat - ec2-user"
    echo "--vpc_id : AWS VPC ID"
    echo "--subnet_id : AWS Subnet ID"
    echo "--sg_ids : One of more comma-separated Security groups IDs for SSN"
    echo "--key_path : Path to admin key (WITHOUT KEY NAME)"
    echo "--key_name : Admin key name (WITHOUT '.pem')"
    echo "--action : Available options: build, deploy, create, terminate"
    echo "-h / --help : Show this help message"
}

function main {
    if [ -z "${WORKSPACE+x}" ]; then WORKSPACE="$PWD"; echo "WORKSPACE is not set, using current dir: $WORKSPACE"; else echo "Using workspace path from environment variable: $WORKSPACE"; fi

    case "$Action" in

      build)
        if buildFrontend; then echo "Front-end build was successful, moving to next step!"; else exit 1; fi
        if buildServices; then echo "Services build was successful, moving to next step!"; else exit 1; fi
        if buildDockers; then echo "Docker images build was successful, moving to next step!"; else exit 1; fi
        ;;

      deploy)
        if deployDlab "create"; then echo "Dlab deploy was successful, moving to next step!"; else exit 1; fi
        ;;

      create)
        if buildFrontend; then echo "Front-end build was successful, moving to next step!"; else exit 1; fi
        if buildServices; then echo "Services build was successful, moving to next step!"; else exit 1; fi
        if buildDockers; then echo "Docker images build was successful, moving to next step!"; else exit 1; fi
        if deployDlab "create"; then echo "DLab deploy was successful, moving to next step!"; else exit 1; fi
        ;;

      terminate)
        terminateDlab "terminate"
        ;;

      *)
        echo "Wrong action parameter! Valid parameters are: \"build\",\"deploy\",\"create\",\"terminate\""
        ;;

    esac
}

while [[ $# -gt 0 ]]
do
    key="$1"

    case $key in
        --infrastructure_tag)
        Infrastructure_Tag="$2"
        shift # past argument
        ;;
        --access_key_id)
        Access_Key_ID="$2"
        shift # past argument
        ;;
        --secret_access_key)
        Secret_Access_Key="$2"
        shift # past argument
        ;;
        --region)
        Region="$2"
        shift # past argument
        ;;
        --os_family)
        OS_family="$2"
        shift # past argument
        ;;
        --cloud_provider)
        Cloud_provider="$2"
        shift # past argument
        ;;
        --os_user)
        OS_user="$2"
        shift # past argument
        ;;
        --vpc_id)
        VPC_id="$2"
        shift # past argument
        ;;
        --subnet_id)
        Subnet_id="$2"
        shift # past argument
        ;;
        --sg_ids)
        Sg_ids="$2"
        shift # past argument
        ;;
        --key_path)
        Key_path="$2"
        shift # past argument
        ;;
        --key_name)
        Key_name="$2"
        shift # past argument
        ;;
        --action)
        Action="$2"
        shift # past argument
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

main
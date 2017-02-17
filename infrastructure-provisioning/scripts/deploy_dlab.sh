#!/bin/bash
#
# This script will create a cluster
#
#Print usage statement if incorrect number of command line args

if [ "$#" -lt 5 ]; then
  echo "Usage: ./deploy_dlab.sh <INFRASTRUCTURE_TAG> <ACCESS_KEY_ID> <SECRET_ACCESS_KEY> <AWS_REGION> <ACTION>"
  exit 1
fi

Infrastructure_Tag=$1
Access_Key_ID=$2
Secret_Access_Key=$3
Region=$4
Action=$5

if [ -z "${WORKSPACE+x}" ]; then WORKSPACE="$PWD"; echo "WORKSPACE is not set, using current dir: $WORKSPACE"; else echo "Using workspace path from environment variable: $WORKSPACE"; fi

function buildFrontend(){
  # Build front-end
  cd "$WORKSPACE"/services/self-service/src/main/resources/webapp/ || exit 1
  sudo npm install gulp
  sudo npm install
  sudo npm run build.prod
  sudo chown -R "$USER" "$WORKSPACE"/*
  cd "$WORKSPACE"/ || exit 1
}

function buildServices(){
  # Build provisioning-service, security-service, self-service
  mvn -DskipTests package
}

function buildDockers(){
  # Build base and ssn docker
  cd infrastructure-provisioning/aws/src || exit 1
  sudo docker build --file base/Dockerfile -t docker.epmc-bdcc.projects.epam.com/dlab-aws-base base/
  sudo docker build --file ssn/Dockerfile -t docker.epmc-bdcc.projects.epam.com/dlab-aws-ssn .
}

function deployDlab(){
  # Prepare files for deployment
  mkdir -p "$WORKSPACE"/web_app || exit 1
  mkdir -p "$WORKSPACE"/web_app/provisioning-service/ || exit 1
  mkdir -p "$WORKSPACE"/web_app/security-service/ || exit 1
  mkdir -p "$WORKSPACE"/web_app/self-service/ || exit 1
  cp "$WORKSPACE"/services/self-service/self-service.yml "$WORKSPACE"/web_app/self-service/
  cp "$WORKSPACE"/services/self-service/target/self-service-1.0.jar "$WORKSPACE"/web_app/self-service/
  cp "$WORKSPACE"/services/provisioning-service/provisioning.yml "$WORKSPACE"/web_app/provisioning-service/
  cp "$WORKSPACE"/services/provisioning-service/target/provisioning-service-1.0.jar "$WORKSPACE"/web_app/provisioning-service/
  cp "$WORKSPACE"/services/security-service/security.yml "$WORKSPACE"/web_app/security-service/
  cp "$WORKSPACE"/services/security-service/target/security-service-1.0.jar "$WORKSPACE"/web_app/security-service/

  # Create SSN node and deploy DLab
  sudo docker run -i -v /root/BDCC-DSS-POC.pem:/root/keys/BDCC-DSS-POC.pem \
    -v "$WORKSPACE"/web_app:/root/web_app -e "resource=ssn" -e "aws_ssn_instance_size=t2.medium" \
        -e "aws_region=us-west-2" -e "aws_vpc_id=vpc-588a2c3d" -e "aws_subnet_id=subnet-1e6c9347" \
        -e "aws_security_groups_ids=sg-e338c89a" -e "conf_key_name=BDCC-DSS-POC" -e "conf_service_base_name=$Infrastructure_Tag" \
        -e "aws_access_key=$Access_Key_ID" -e "aws_secret_access_key=$Secret_Access_Key" \
        docker.epmc-bdcc.projects.epam.com/dlab-aws-ssn --action "$1"
}

function terminateDlab(){
  # Drop Dlab environment with selected infrastructure tag
  sudo docker run -i -v /root/BDCC-DSS-POC.pem:/root/keys/BDCC-DSS-POC.pem \
    -e "aws_region=$Region" -e "conf_service_base_name=$Infrastructure_Tag" \
    -e "resource=ssn" -e "aws_access_key=$Access_Key_ID" -e "aws_secret_access_key=$Secret_Access_Key" \
    docker.epmc-bdcc.projects.epam.com/dlab-aws-ssn --action "$1"
}

case "$Action" in

  build)
    if buildFrontend; then echo "Front-end build was successfull, moving to next step!"; else exit 1; fi
    if buildServices; then echo "Services build was successfull, moving to next step!"; else exit 1; fi
    if buildDockers; then echo "Docker images build was successfull, moving to next step!"; else exit 1; fi
    ;;

  deploy)
    if deployDlab "create"; then echo "Dlab deploy was successfull, moving to next step!"; else exit 1; fi
    ;;

  create)
    if buildFrontend; then echo "Front-end build was successfull, moving to next step!"; else exit 1; fi
    if buildServices; then echo "Services build was successfull, moving to next step!"; else exit 1; fi
    if buildDockers; then echo "Docker images build was successfull, moving to next step!"; else exit 1; fi
    if deployDlab "create"; then echo "Dlab deploy was successfull, moving to next step!"; else exit 1; fi
    ;;

  terminate)
    terminateDlab "terminate"
    ;;

  *)
    echo "Wrong action parameter! Valid parameters are: \"build\",\"deploy\",\"create\",\"terminate\""
    ;;

esac
#!/bin/bash

creds_file_path=$1
gke_name=$2
region=$3
project_id=$4

gcloud auth activate-service-account --key-file "$creds_file_path"
export KUBECONFIG=/tmp/config; gcloud beta container clusters get-credentials "$gke_name" --region "$region" --project "$project_id"
ROOT_CA=$(kubectl get -o jsonpath="{.data['root_ca\.crt']}" configmaps/step-certificates-certs -ndlab | base64 | tr -d '\n')
KID=$(kubectl get -o jsonpath="{.data['ca\.json']}" configmaps/step-certificates-config -ndlab | jq -r .authority.provisioners[].key.kid)
KID_NAME=$(kubectl get -o jsonpath="{.data['ca\.json']}" configmaps/step-certificates-config -ndlab | jq -r .authority.provisioners[].name)
jq -n --arg rootCa "$ROOT_CA" --arg kid "$KID" --arg kidName "$KID_NAME" '{rootCa: $rootCa, kid: $kid, kidName: $kidName}'
unset KUBECONFIG
rm /tmp/config

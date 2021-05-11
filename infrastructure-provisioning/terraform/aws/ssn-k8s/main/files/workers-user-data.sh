#!/bin/bash
# *****************************************************************************
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ******************************************************************************

set -e

check_tokens () {
RUN=$(aws s3 ls s3://${k8s-bucket-name}/k8s/masters/ > /dev/null && echo "true" || echo "false")
sleep 5
}

# Creating DataLab user
sudo useradd -m -G sudo -s /bin/bash ${k8s_os_user}
sudo bash -c 'echo "${k8s_os_user} ALL = NOPASSWD:ALL" >> /etc/sudoers'
sudo mkdir /home/${k8s_os_user}/.ssh
sudo bash -c 'cat /home/ubuntu/.ssh/authorized_keys > /home/${k8s_os_user}/.ssh/authorized_keys'
sudo chown -R ${k8s_os_user}:${k8s_os_user} /home/${k8s_os_user}/
sudo chmod 700 /home/${k8s_os_user}/.ssh
sudo chmod 600 /home/${k8s_os_user}/.ssh/authorized_keys

sudo apt-get update
sudo apt-get install -y python3-pip
sudo pip install -U pip
sudo pip install awscli

# installing Docker
sudo bash -c 'curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -'
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install -y docker-ce
sudo systemctl enable docker
# installing kubeadm, kubelet and kubectl
sudo apt-get install -y apt-transport-https curl
sudo bash -c 'curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -'
sudo bash -c 'echo "deb http://apt.kubernetes.io/ kubernetes-xenial main" > /etc/apt/sources.list.d/kubernetes.list'
sudo apt-get update
sudo apt-get install -y kubelet=${kubernetes_version} kubeadm=${kubernetes_version} kubectl=${kubernetes_version}
while check_tokens
do
    if [[ $RUN == "false" ]];
    then
        echo "Waiting for initial cluster initialization..."
    else
        echo "Initial cluster initialized!"
        break
    fi
done

cat <<EOF > /tmp/node.yaml
---
apiVersion: kubeadm.k8s.io/v1beta2
discovery:
  bootstrapToken:
    apiServerEndpoint: ${k8s-nlb-dns-name}:6443
    caCertHashes:
    - HASHES
    token: TOKEN
  tlsBootstrapToken: TOKEN
kind: JoinConfiguration
nodeRegistration:
  kubeletExtraArgs:
    cloud-provider: aws
  name: NODE_NAME
EOF
aws s3 cp s3://${k8s-bucket-name}/k8s/masters/join_command /tmp/join_command
token=$(cat /tmp/join_command | sed 's/--\+/\n/g' | grep "token " | awk '{print $2}')
hashes=$(cat /tmp/join_command | sed 's/--\+/\n/g' | grep "discovery-token-ca-cert-hash" | awk '{print $2}')
full_hostname=$(curl http://169.254.169.254/latest/meta-data/hostname)
sed -i "s/NODE_NAME/$full_hostname/g" /tmp/node.yaml
sed -i "s/TOKEN/$token/g" /tmp/node.yaml
sed -i "s/HASHES/$hashes/g" /tmp/node.yaml
sudo kubeadm join --config /tmp/node.yaml

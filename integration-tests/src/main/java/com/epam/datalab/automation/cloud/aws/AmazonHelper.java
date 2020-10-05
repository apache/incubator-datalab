/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.automation.cloud.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Grant;
import com.epam.datalab.automation.exceptions.CloudException;
import com.epam.datalab.automation.helper.ConfigPropertyValue;
import com.epam.datalab.automation.helper.NamingHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AmazonHelper {

    private static final Logger LOGGER = LogManager.getLogger(AmazonHelper.class);
	private static final Duration CHECK_TIMEOUT = Duration.parse("PT10m");
	private static final String LOCALHOST_IP = ConfigPropertyValue.get("LOCALHOST_IP");

	private AmazonHelper(){}
	
	private static AWSCredentials getCredentials() {
		return new BasicAWSCredentials(ConfigPropertyValue.getAwsAccessKeyId(), ConfigPropertyValue.getAwsSecretAccessKey());
	}
	
	private static Region getRegion() {
		return Region.getRegion(Regions.fromName(ConfigPropertyValue.getAwsRegion()));
	}

	private static List<Instance> getInstances(String instanceName) {
		AWSCredentials credentials = getCredentials();
		AmazonEC2 ec2 = new AmazonEC2Client(credentials);
		ec2.setRegion(getRegion());

		List<String> valuesT1 = new ArrayList<>();
		valuesT1.add(instanceName + "*");
		Filter filter = new Filter("tag:" + NamingHelper.getServiceBaseName() + "-tag", valuesT1);

		DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withFilters(filter);
		DescribeInstancesResult describeInstanceResult = ec2.describeInstances(describeInstanceRequest);

		List<Reservation> reservations = describeInstanceResult.getReservations();

		if (reservations.isEmpty()) {
			throw new CloudException("Instance " + instanceName + " in Amazon not found");
		}

		List<Instance> instances = reservations.get(0).getInstances();
		if (instances.isEmpty()) {
			throw new CloudException("Instance " + instanceName + " in Amazon not found");
		}

		return instances;
	}

	public static Instance getInstance(String instanceName) {
    	return (ConfigPropertyValue.isRunModeLocal() ?
    			new Instance()
            		.withPrivateDnsName("localhost")
            		.withPrivateIpAddress(LOCALHOST_IP)
            		.withPublicDnsName("localhost")
            		.withPublicIpAddress(LOCALHOST_IP)
            		.withTags(new Tag()
            					.withKey("Name")
            					.withValue(instanceName)) :
            	getInstances(instanceName).get(0));
    }

	public static void checkAmazonStatus(String instanceName, AmazonInstanceState expAmazonState) throws
			InterruptedException {
        LOGGER.info("Check status of instance {} on Amazon: {}", instanceName);
        if (ConfigPropertyValue.isRunModeLocal()) {
        	LOGGER.info("Amazon instance {} fake state is {}", instanceName, expAmazonState);
        	return;
        }
        
        String instanceState;
        long requestTimeout = ConfigPropertyValue.getAwsRequestTimeout().toMillis();
    	long timeout = CHECK_TIMEOUT.toMillis();
        long expiredTime = System.currentTimeMillis() + timeout;
        Instance instance;
        while (true) {
        	instance = AmazonHelper.getInstance(instanceName);
        	instanceState = instance.getState().getName();
        	if (!instance.getState().getName().equals("shutting-down")) {
        		break;
        	}
        	if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
                LOGGER.info("Amazon instance {} state is {}", instanceName, instanceState);
        		throw new CloudException("Timeout has been expired for check amazon instance " + instanceState);
            }
            Thread.sleep(requestTimeout);
        }
        
        for (Instance i : AmazonHelper.getInstances(instanceName)) {
            LOGGER.info("Amazon instance {} state is {}. Instance id {}, private IP {}, public IP {}",
            		instanceName, instanceState, i.getInstanceId(), i.getPrivateIpAddress(), i.getPublicIpAddress());
		}
        Assert.assertEquals(instanceState, expAmazonState.toString(), "Amazon instance " + instanceName + " state is not correct. Instance id " +
        		instance.getInstanceId() + ", private IP " + instance.getPrivateIpAddress() + ", public IP " + instance.getPublicIpAddress());
    }

    public static void printBucketGrants(String bucketName){
        LOGGER.info("Print grants for bucket {} on Amazon: " , bucketName);
        if (ConfigPropertyValue.isRunModeLocal()) {
        	LOGGER.info("  action skipped for run in local mode");
        	return;
        }
        AWSCredentials credentials = getCredentials();
        AmazonS3 s3 = new AmazonS3Client(credentials);
        
        s3.setRegion(getRegion());
        AccessControlList acl = s3.getBucketAcl(bucketName);
        for (Grant grant : acl.getGrants()) {
            LOGGER.info(grant);
		}
    }
}

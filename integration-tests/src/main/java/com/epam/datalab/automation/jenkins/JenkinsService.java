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

package com.epam.datalab.automation.jenkins;

import com.epam.datalab.automation.exceptions.JenkinsException;
import com.epam.datalab.automation.helper.ConfigPropertyValue;
import com.epam.datalab.automation.helper.NamingHelper;
import com.epam.datalab.automation.http.HttpStatusCode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.authentication.FormAuthConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jayway.restassured.RestAssured.given;

public class JenkinsService {
	private static final Logger LOGGER = LogManager.getLogger(JenkinsService.class);

    private final String awsAccessKeyId;
    private final String awsSecretAccessKey;
    
    private String ssnURL;
    private String serviceBaseName;

	private FormAuthConfig config = new FormAuthConfig(JenkinsConfigProperties.JENKINS_JOB_NAME_SEARCH, "username",
			"password");
    
    public JenkinsService(){
    	if (!ConfigPropertyValue.isUseJenkins()) {
    		ssnURL = ConfigPropertyValue.getSsnUrl();
    		serviceBaseName = ConfigPropertyValue.getServiceBaseName();
    	}
        awsAccessKeyId = convertToParam(ConfigPropertyValue.getAwsAccessKeyId());
        awsSecretAccessKey = convertToParam(ConfigPropertyValue.getAwsSecretAccessKey());
    }
    
    private String convertToParam(String s) {
    	s= s.replaceAll("/", "%2F");
    	return s;
    }
    
    public String getSsnURL() {
        return ssnURL;
    }

    public String getServiceBaseName() {
        return serviceBaseName;
    }
    
    private String getQueueStatus() {
    	return getWhen(ContentType.XML)
                .get(JenkinsUrls.API).getBody()
                .xmlPath()
                .getString(JenkinsResponseElements.IN_QUEUE_ELEMENT);
    }

	private void waitForJenkinsStartup(Duration duration) throws InterruptedException {
    	String actualStatus;
    	long timeout = duration.toMillis();
        long expiredTime = System.currentTimeMillis() + timeout;
        
    	while ((actualStatus = getQueueStatus()).endsWith(JenkinsConfigProperties.SUCCESS_STATUS)) {
            Thread.sleep(JenkinsConfigProperties.JENKINS_REQUEST_TIMEOUT);
            if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
            	actualStatus = getQueueStatus();
            	break;
            }
        }
        
        if (actualStatus.endsWith(JenkinsConfigProperties.SUCCESS_STATUS)) {
            LOGGER.info("ERROR: Timeout has been expired for Jenkins");
            LOGGER.info("  timeout is {}");
        }
    }

	public String runJenkinsJob(String jenkinsJobURL) throws InterruptedException {
    	if (!ConfigPropertyValue.isUseJenkins()) {
    		return ConfigPropertyValue.getJenkinsBuildNumber();
    	}

		baseUriInitialize(jenkinsJobURL);
        String dateAsString = NamingHelper.generateRandomValue();
        Response responsePostJob = getWhen(ContentType.URLENC)
				.body(String.format(JenkinsConfigProperties.jenkinsJobStartBody,
                        awsAccessKeyId, awsSecretAccessKey, dateAsString,
                        ConfigPropertyValue.getClusterOsUser(), ConfigPropertyValue.getClusterOsFamily(),
                        awsAccessKeyId, awsSecretAccessKey, dateAsString,
                        ConfigPropertyValue.getClusterOsUser(), ConfigPropertyValue.getClusterOsFamily()))
        		.post(jenkinsJobURL + "build");
        Assert.assertEquals(responsePostJob.statusCode(), HttpStatusCode.OK);
        
        waitForJenkinsStartup(ConfigPropertyValue.getTimeoutJenkinsAutotest());
        
        setBuildNumber();
        checkBuildResult();
        setJenkinsURLServiceBaseName();
        
        return ConfigPropertyValue.getJenkinsBuildNumber();
    }

	public String getJenkinsJob() throws InterruptedException {
    	if (!ConfigPropertyValue.isUseJenkins()) {
    		return ConfigPropertyValue.getJenkinsBuildNumber();
    	}

		baseUriInitialize(ConfigPropertyValue.getJenkinsJobURL());

        setBuildNumber();
        checkBuildResult();
        setJenkinsURLServiceBaseName();

        return ConfigPropertyValue.getJenkinsBuildNumber();
    }

	private static void baseUriInitialize(String value) {
		RestAssured.baseURI = value;
	}

	private void setBuildNumber() {
        if (ConfigPropertyValue.getJenkinsBuildNumber() != null) {
            LOGGER.info("Jenkins build number is {}", ConfigPropertyValue.getJenkinsBuildNumber());
        	return;
    	}

        String buildName = getWhen(ContentType.URLENC)
                .get(JenkinsUrls.LAST_BUILD).getBody().htmlPath().getString(JenkinsResponseElements.HTML_TITLE);
        
        Pattern pattern = Pattern.compile("\\s#\\d+(?!\\d+)\\s");      
        Matcher matcher = pattern.matcher(buildName);
        if(matcher.find()) {
        	ConfigPropertyValue.setJenkinsBuildNumber(matcher.group().substring(2).trim());
        } else {
			throw new JenkinsException("Jenkins job was failed. There is no buildNumber");
        }
        LOGGER.info("Jenkins build number is {}", ConfigPropertyValue.getJenkinsBuildNumber());
    }


	private void checkBuildResult() throws InterruptedException {
    	String buildResult;
    	long timeout = ConfigPropertyValue.getTimeoutJenkinsAutotest().toMillis();
    	long expiredTime = System.currentTimeMillis() + timeout;
        
        do {
        	buildResult = getWhen(ContentType.JSON)
        			.get(ConfigPropertyValue.getJenkinsBuildNumber() + JenkinsUrls.JSON_PRETTY)
        			.getBody()
                    .jsonPath()
                    .getString(JenkinsResponseElements.RESULT);
            if (buildResult == null) {
            	if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
					throw new JenkinsException("Timeout has been expired for Jenkins build. Timeout is " +
							ConfigPropertyValue.getTimeoutJenkinsAutotest());
            	}
            	Thread.sleep(JenkinsConfigProperties.JENKINS_REQUEST_TIMEOUT);
            }
        } while (buildResult == null);
        
        if(!buildResult.equals("SUCCESS")) {
			throw new JenkinsException("Jenkins job was failed. Build result is not success");
        }
    }

	private void setJenkinsURLServiceBaseName() {
        String jenkinsHoleURL = getWhen(ContentType.TEXT)
        		.get(ConfigPropertyValue.getJenkinsBuildNumber() + JenkinsUrls.LOG_TEXT)
        		.getBody()
                .prettyPrint();
        Pattern pattern = Pattern.compile("Jenkins URL:(.+)");      
        Matcher matcher = pattern.matcher(jenkinsHoleURL);
        if(matcher.find()) {
        	ssnURL = matcher.group(1).replaceAll("/jenkins", "");         
        }
            
        pattern = Pattern.compile("Service base name:(.+)");      
        matcher = pattern.matcher(jenkinsHoleURL);
        if(matcher.find()) {
        	serviceBaseName = matcher.group(1);         
        } else {
			throw new JenkinsException("SSN URL in Jenkins job not found");
        }
    }

    private RequestSpecification getWhen(ContentType contentType) {
        return given()
                .header(JenkinsConfigProperties.AUTHORIZATION,
						String.format(JenkinsConfigProperties.AUTHORIZATION_KEY, base64CredentialDecode
								(ConfigPropertyValue.get(ConfigPropertyValue.JENKINS_USERNAME), ConfigPropertyValue
										.get(ConfigPropertyValue.JENKINS_PASS))))
        		.auth()
                .form(ConfigPropertyValue.getJenkinsUsername(), ConfigPropertyValue.getJenkinsPassword(), config)
        		.contentType(contentType).when();
    }

    private static String base64CredentialDecode(String user, String password) {
        byte[] bytesEncoded = Base64.encodeBase64(String.format("%s:%s", user, password).getBytes());
        return new String(bytesEncoded);
    }
}

package com.epam.dlab.backendapi.domain;

import com.epam.dlab.dto.aws.status.AwsEnvResource;
import com.epam.dlab.utils.UsernameUtils;

/** Store info for the requests about the environment status of user. 
 */
public class EnvStatusListenerUserInfo {
	/** Time for the next check in milliseconds. */
    private long nextCheckTimeMillis;
    /** Name of user. */
    private String username;
    /** Access token for request provisioning service. */
    private String accessToken;

    //TODO @dto
    private AwsEnvResource dto;

    public EnvStatusListenerUserInfo(String username, String accessToken) {
        this.nextCheckTimeMillis = System.currentTimeMillis();
        this.accessToken = accessToken;
        this.username = username;
    }

    /** Instantiate the user info.
     * @param username the name of user.
     * @param accessToken the access token for requests to Provisioning Service.
     * @param awsRegion the name of region in Amazon. 
     */
    public EnvStatusListenerUserInfo(String username, String accessToken, String awsRegion) {
		this.nextCheckTimeMillis = System.currentTimeMillis();
    	this.accessToken = accessToken;
    	this.username = username;
    	dto = new AwsEnvResource()
    			.withAwsRegion(awsRegion)
    			.withEdgeUserName(UsernameUtils.removeDomain(username))
    			.withAwsIamUser(username);
	}

    /** Return the time for next check of environment statuses.
     */
    public long getNextCheckTimeMillis() {
    	return nextCheckTimeMillis;
    }
    
    /** Set the time for next check of environment statuses.
     * @param nextCheckTimeMillis the time for next check.
     */
	public void setNextCheckTimeMillis(long nextCheckTimeMillis) {
		this.nextCheckTimeMillis = nextCheckTimeMillis;
	}

	/** Return the name of user.
     */
    public String getUsername() {
    	return username;
    }
    
    /** Return the access token for requests to Provisioning Service.
     */
    public String getAccessToken() {
    	return accessToken;
    }
    
    /** Return the DTO object for check of environment statuses.
     */
    public AwsEnvResource getDTO() {
    	return dto;
    }
}
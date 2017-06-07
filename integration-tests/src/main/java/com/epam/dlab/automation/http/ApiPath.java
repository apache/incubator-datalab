package com.epam.dlab.automation.http;

public class ApiPath {

    public static final String LOGIN = "/api/user/login";
    public static final String LOGOUT = "/api/user/logout";
    public static final String UPLOAD_KEY = "/api/user/access_key"; 
    public static final String AUTHORIZE_USER = "/api/user/authorize";    
    public static final String EXP_ENVIRONMENT = "/api/infrastructure_provision/exploratory_environment";
    public static final String PROVISIONED_RES = "/api/infrastructure_provision/provisioned_user_resources";
    public static final String COMPUTATIONAL_RES = "/api/infrastructure_provision/computational_resources";
    public static final String STOP_NOTEBOOK = EXP_ENVIRONMENT + "/%s/stop";
    public static final String TERMINATE_EMR = COMPUTATIONAL_RES + "/%s/%s/terminate";
    public static final String TERMINATE_NOTEBOOK = EXP_ENVIRONMENT + "/%s/terminate";
          
    public static String ConfigureURL(String url, Object... args) {        
        return String.format(url, args);        
    }
    
    public static String getStopNotebookUrl(String serviceBaseName) {
        return ConfigureURL(STOP_NOTEBOOK, serviceBaseName);
    }
    
    public static String getTerminateEMRUrl(String notebookName, String emrName) {
        return ConfigureURL(TERMINATE_EMR, notebookName, emrName);
    }
    
    public static String getTerminateNotebookUrl(String serviceBaseName) {
        return ConfigureURL(TERMINATE_NOTEBOOK, serviceBaseName);
    }
}

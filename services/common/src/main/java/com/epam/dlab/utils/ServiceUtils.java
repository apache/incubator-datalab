package com.epam.dlab.utils;

import com.epam.dlab.constants.ServiceConsts;

public class ServiceUtils {
	
	private static String includePath = null;
	
	static {
        includePath = System.getenv(ServiceConsts.DLAB_CONF_DIR_NAME);
        if ( includePath == null || includePath.isEmpty() ) {
        	includePath = getUserDir();
        }
	}
	
	public static String getUserDir() {
		return System.getProperty("user.dir");
	}
	
	public static String getConfPath() {
        return includePath;
	}

}

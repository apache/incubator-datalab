package com.epam.dlab.automation.helper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.epam.dlab.automation.aws.AmazonHelper;

public class TestNamingHelper {
    private static AtomicInteger idCounter = new AtomicInteger(0);

    public static String generateRandomValue(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMddhmmss");
        return "AutoTest_" + simpleDateFormat.format(new Date()) + "_" + idCounter.addAndGet(1);
    }

    public static String generateRandomValue(String notebokTemplateName){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMddhmmss");
        return "AutoTest_" + notebokTemplateName + "_" +  simpleDateFormat.format(new Date())+ "_" + + idCounter.addAndGet(1);
    }
    
    public static String getSelfServiceURL(String ssnURL, String path) {
        return ssnURL + path;
    }
    
    public static String getAmazonNodePrefix(String serviceBaseName, String nodePrefix) {
        return serviceBaseName + "-" + nodePrefix;
    }
    
    public static String getBucketName(String serviceBaseName) {
    	return String.format("%s-%s-bucket", serviceBaseName, ConfigPropertyValue.getUsernameSimple()).replace('_', '-').toLowerCase();
    }
    
    public static String getEmrClusterName(String emrName) throws Exception {
        Instance instance = AmazonHelper.getInstance(emrName);
        for (Tag tag : instance.getTags()) {
			if (tag.getKey().equals("Name")) {
		        return tag.getValue();
			}
		}
        throw new Exception("Could not detect cluster name for EMR " + emrName);
    }
}

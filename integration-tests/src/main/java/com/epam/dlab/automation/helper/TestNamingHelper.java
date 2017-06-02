package com.epam.dlab.automation.helper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

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
}

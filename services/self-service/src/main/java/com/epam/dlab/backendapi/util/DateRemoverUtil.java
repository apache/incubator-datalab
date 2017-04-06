package com.epam.dlab.backendapi.util;

/**
 * Created on 3/15/2017.
 */
public class DateRemoverUtil {

    public static final String ERROR_DATE_FORMAT = "\\[Error-\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\]:";
    public static final String ERROR_WITHOUT_DATE_FORMAT = "\\[Error\\]:";

    public static String removeDateFormErrorMessage(String errorMessage, String errorDateFormat, String replaceWith) {
        return errorMessage.replaceAll(errorDateFormat, replaceWith);
    }
}

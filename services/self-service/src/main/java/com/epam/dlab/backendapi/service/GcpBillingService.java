package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.aws.AwsBillingFilter;
import org.bson.Document;

import java.text.ParseException;
import java.util.List;

public class GcpBillingService implements BillingService<Object> {
    @Override
    public Document getReport(UserInfo userInfo, Object filter) {
        return null;
    }

    @Override
    public String getFirstLine(Document document) throws ParseException {
        return null;
    }

    @Override
    public List<String> getHeadersList(boolean full) {
        return null;
    }

    @Override
    public String getLine(boolean full, Document document) {
        return null;
    }

    @Override
    public String getTotal(boolean full, Document document) {
        return null;
    }

    @Override
    public String getReportFileName(UserInfo userInfo, Object filter) {
        return null;
    }
}

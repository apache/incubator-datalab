package com.epam.dlab.automation.test.libs.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * Created by yu on 7/3/17.
 */
public class LibInstallRequest {
    @JsonProperty
    private List<Lib> libs;
    @JsonProperty("exploratory_name")
    private String notebookName;

    public LibInstallRequest(List<Lib> libs, String notebookName) {
        this.libs = libs;
        this.notebookName = notebookName;
    }

    public List<Lib> getLibs() {
        return libs;
    }

    public String getNotebookName() {
        return notebookName;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("libs", libs)
                .add("notebookName", notebookName)
                .toString();
    }
}

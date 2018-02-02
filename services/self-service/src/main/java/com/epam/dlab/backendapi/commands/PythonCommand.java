package com.epam.dlab.backendapi.commands;

import com.epam.dlab.command.CmdCommand;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PythonCommand implements CmdCommand {
    private static final String PYTHON = "python ";
    private final String fileName;
    private List<String> options = new ArrayList<>();

    public PythonCommand(String fileName) {
        this.fileName = fileName;
    }

    public PythonCommand withOption(String option) {
        options.add(option);
        return this;
    }

    public PythonCommand withOption(String key, String value) {
        options.add(key + " " + value);
        return this;
    }

    @Override
    public String toCMD() {
        return PYTHON + fileName + StringUtils.SPACE + String.join(StringUtils.SPACE, options);
    }
}

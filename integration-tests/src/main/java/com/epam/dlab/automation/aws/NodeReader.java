package com.epam.dlab.automation.aws;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.IOException;

public class NodeReader {

    @SuppressWarnings("unchecked")
	public static <T> T readNode(String pathToJson, Class<T> clasz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        try (FileInputStream in = new FileInputStream(pathToJson);){
          return (T) mapper.readerFor(clasz).readValue(in);
//                  readTree(in);
        }
    }
}

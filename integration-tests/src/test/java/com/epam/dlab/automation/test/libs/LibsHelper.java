package com.epam.dlab.automation.test.libs;

public class LibsHelper {

    public static String getLibGroupsPath(String notebookName){
        if(notebookName.contains("deeplearning")){
            return "deeplearning/lib_groups.json";
        }
        else if(notebookName.contains("jupyter")){
            return "jupyter/lib_groups.json";
        }
        else if(notebookName.contains("rstudio")){
            return "rstudio/lib_groups.json";
        }
        else if(notebookName.contains("tensor")){
            return "tensor/lib_groups.json";
        }
        else if(notebookName.contains("zeppelin")){
            return "zeppelin/lib_groups.json";
        }
        else return "lib_groups.json";
    }

    public static String getLibListPath(String notebookName){
        if(notebookName.contains("deeplearning")){
            return "deeplearning/lib_list.json";
        }
        else if(notebookName.contains("jupyter")){
            return "jupyter/lib_list.json";
        }
        else if(notebookName.contains("rstudio")){
            return "rstudio/lib_list.json";
        }
        else if(notebookName.contains("tensor")){
            return "tensor/lib_list.json";
        }
        else if(notebookName.contains("zeppelin")){
            return "zeppelin/lib_list.json";
        }
        else return "lib_list.json";
    }
}

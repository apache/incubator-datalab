/***************************************************************************

 Copyright (c) 2018, EPAM SYSTEMS INC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 ****************************************************************************/

package com.epam.dlab.automation.test.libs;

public class LibsHelper {

    private static int currentQuantityOfLibInstallErrorsToFailTest;
    private static int maxQuantityOfLibInstallErrorsToFailTest;


    public static int getCurrentQuantityOfLibInstallErrorsToFailTest() {
        return currentQuantityOfLibInstallErrorsToFailTest;
    }

    public static void setCurrentQuantityOfLibInstallErrorsToFailTest(int currentQuantityOfLibInstallErrorsToFailTest) {
        LibsHelper.currentQuantityOfLibInstallErrorsToFailTest = currentQuantityOfLibInstallErrorsToFailTest;
    }

    public static int getMaxQuantityOfLibInstallErrorsToFailTest() {
        return maxQuantityOfLibInstallErrorsToFailTest;
    }

    public static void setMaxQuantityOfLibInstallErrorsToFailTest(int maxQuantityOfLibInstallErrorsToFailTest) {
        LibsHelper.maxQuantityOfLibInstallErrorsToFailTest = maxQuantityOfLibInstallErrorsToFailTest;
    }

    public static void incrementByOneCurrentQuantityOfLibInstallErrorsToFailTest(){
        currentQuantityOfLibInstallErrorsToFailTest++;
    }

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

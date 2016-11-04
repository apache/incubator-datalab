/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.auth.ldap;

import java.util.*;
import javax.script.*;

import com.epam.dlab.auth.UserInfo;

public class ScriptList {

    public static void main( String[] args ) throws ScriptException, NoSuchMethodException {

        ScriptEngineManager mgr = new ScriptEngineManager();
        List<ScriptEngineFactory> factories = mgr.getEngineFactories();

        for (ScriptEngineFactory factory : factories) {

            System.out.println("ScriptEngineFactory Info");

            String engName = factory.getEngineName();
            String engVersion = factory.getEngineVersion();
            String langName = factory.getLanguageName();
            String langVersion = factory.getLanguageVersion();

            System.out.printf("\tScript Engine: %s (%s)%n", engName, engVersion);

            List<String> engNames = factory.getNames();
            for(String name : engNames) {
                System.out.printf("\tEngine Alias: %s%n", name);
            }

            System.out.printf("\tLanguage: %s (%s)%n", langName, langVersion);

        }
        
        ScriptEngine python = mgr.getEngineByName("python");
        ScriptEngine js = mgr.getEngineByName("javascript");
        python.eval("print \"Hello Python!\"");

        js.eval("print('Hello JavaScript!');");

        Invocable ijs = (Invocable) js;
        Invocable ipy = (Invocable) python;
        
        js.eval("var f=function(ui){print(ui);ui.setFirstName(\"Mike\");return ui;};");
        
        Object res = ijs.invokeFunction("f", new UserInfo("test", "pass"));
        System.out.println(res);
        
        python.eval("def f(ui):\n   print ui\n   ui.setLastName(\"Teplitskiy\")\n   return ui\n");
        Object res2 = ipy.invokeFunction("f", new UserInfo("test", "pass"));
        System.out.println(res2);        
     
        
        
    }

}
/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.auth.script;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.epam.dlab.auth.UserInfo;

public class ScriptHolder {
	
	private final static String FUNCTION    = "enrichUserInfo";
	
	private final ScriptEngineManager   mgr = new ScriptEngineManager();
	private final Map<String,Invocable> map = new HashMap<>();
	
	public ScriptHolder() {
		
	}
	
	public BiFunction<UserInfo,Map<String,?>,UserInfo> evalOnce(String name, String language, String code) throws ScriptException {
		if( ! map.containsKey(name)) {
			ScriptEngine engine = mgr.getEngineByName( language );
			engine.eval(code);
			map.put(name, (Invocable) engine);
		}
		return (ui,context)->{
			try {
				return (UserInfo) map.get(name).invokeFunction(FUNCTION, ui,context);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}
	
	
}

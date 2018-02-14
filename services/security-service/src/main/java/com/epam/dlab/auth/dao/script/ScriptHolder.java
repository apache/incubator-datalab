/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

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

package com.epam.dlab.auth.dao.script;

import com.epam.dlab.auth.UserInfo;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ScriptHolder {
	
	private static final String FUNCTION    = "enrichUserInfo";
	
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

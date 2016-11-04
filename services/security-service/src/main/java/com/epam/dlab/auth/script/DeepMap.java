/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.auth.script;

import java.util.HashMap;
import java.util.Map;

public class DeepMap {
	
	private final Map<String, Object> root;
	
	public DeepMap(Map<String, Object> parent) {
		super();
		this.root = parent;
	}

	public DeepMap() {
		super();
		this.root = new HashMap<>();
	}

	public Map<String, Object> getRoot() {
		return root;
	}
	
	public DeepMap getBranch(String branchName) {
		Map<String, Object> branch = (Map<String, Object>) root.get(branchName);
		if( branch == null ) {
			branch = new HashMap<>();
			root.put(branchName, branch);
		}
		return new DeepMap(branch);
	}
	
	public void put(String key,Object val) {
		root.put(key, val);
	}
	
}

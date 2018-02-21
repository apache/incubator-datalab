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

package com.epam.dlab.auth.dao.filter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SearchResultProcessor {
	private String language;
	private String code;
	private String path;

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCode() {
		if (code == null || "".equals(code)) {
			try {
				code = new String(Files.readAllBytes(Paths.get(path)));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return "SearchResultProcessor [language=" + language + ", path=" + path + ", code=" + code + "]";
	}


}

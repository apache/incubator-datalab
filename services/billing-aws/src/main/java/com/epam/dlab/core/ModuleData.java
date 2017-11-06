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

package com.epam.dlab.core;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.epam.dlab.exception.InitializationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/** Provides loading and storing the working data of modules.
 */
public class ModuleData {
	
	/** Date formatter. */ 
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	
	/** Entries of data. */
	private Map<String, String> entries = new HashMap<>();
	
	/** Data file name. */
	private final String filename;

	/** Flag modification of entries. */
	private boolean modified;
	
	/** Return the name of data file. */
	public String getFilename() {
		return filename;
	}
	
	/** Return <b>true</b> if any entries was modify. */
	public boolean isModified() {
		return modified;
	}
	
	/** Instantiate module data.
	 * @param filename the name of data file.
	 * @throws InitializationException
	 */
	public ModuleData(String filename) throws InitializationException {
		this.filename = filename;
		if (filename == null) {
			entries = new HashMap<>();
		} else {
			load();
		}
	}
	
	/** Return the value for given key or <b>null</b> if value not found.
	 * @param key the key of entry.
	 */
	public String getString(String key) {
		return entries.get(key);
	}

	/** Return the date value for given key or <b>null</b> if value not found.
	 * @param key the key of entry.
	 * @throws ParseException 
	 */
	public Date getDate(String key) throws ParseException {
		String value = entries.get(key);
		return (value == null ? null : dateFormat.parse(value));
	}

	/** Set value for given key or delete entry if the value is <b>null</b>.
	 * @param key the key of entry.
	 * @param value the value.
	 */
	public void set(String key, String value) {
		if (StringUtils.equals(entries.get(key), value)) {
			return;
		} else if (value == null) {
			entries.remove(key);
		} else {
			entries.put(key, value);
		}
		modified = true;
	}
	
	/** Set value for given key or delete entry if the value is <b>null</b>.
	 * @param key the key of entry.
	 * @param value the date.
	 */
	public void set(String key, Date value) {
		set(key, dateFormat.format(value));
	}
	
	/** Load the data entries from data file.
	 * @throws InitializationException
	 */
	@SuppressWarnings("unchecked")
	public void load() throws InitializationException {
		modified = false;
		
		File file = new File(filename);
		if (!file.exists()) {
			entries = new HashMap<>();
			return;
		}
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			entries = (HashMap<String, String>) mapper.readValue(file, HashMap.class);
		} catch (Exception e) {
			throw new InitializationException("Cannot load module data from file \"" +
					filename + "\". " + e.getLocalizedMessage(), e);
		}
	}

	/** Store the data entries to data file if entries was modify.
	 * @throws InitializationException
	 */
	public void store() throws InitializationException {
		if (filename == null) {
			throw new InitializationException("Cannot to store working data. " +
					"Property \"workingFile\" in billing configuration is not set.");
		} 
		if (!isModified()) {
			return;
		}
			
		File file = new File(filename);
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(file, entries);
		} catch (Exception e) {
			throw new InitializationException("Cannot to store working data file \"" +
					filename + "\". " + e.getLocalizedMessage(), e);
		}
		
		modified = false;
	}
	
	
	/** Returns a string representation of the object.
	 * @param self the object to generate the string for (typically this), used only for its class name.
	 */
	public ToStringHelper toStringHelper(Object self) {
    	return MoreObjects.toStringHelper(self)
    			.addValue(entries);
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this)
    			.toString();
    }
}

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

package com.epam.dlab.auth.rest;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpirableContainer<M> {
	
	private static class Holder<M> {
		
		private final M data;
		private final long expTime;
		
		public Holder(M data,long ttl) {
			this.data = data;
			this.expTime = System.currentTimeMillis() + ttl;
		}
		
		public boolean expired() {
			return System.currentTimeMillis() > expTime;
		}
		
		public M get() {
			return data;
		}
	}
	
	private final Map<String,Holder<M>> map = new ConcurrentHashMap<>();
	
	public ExpirableContainer() {
		
	}
	
	public void put(String key, M data, long ttl) {
		if( ttl <= 0 ) {
			return;
		} else {
			map.put(key, new Holder<M>(data, ttl));
		}
	}
	
	public M get(String key) {
		Holder<M> dataHolder = map.get(key);
		if(dataHolder == null) {
			return null;
		} else if(dataHolder.expired()) {
			map.remove(key);
			return null;
		} else {
			return dataHolder.get();
		}
	}
	
	public <X extends M> X get(String key,Class<X> clazz) {
		Holder<M> dataHolder = map.get(key);
		if(dataHolder == null) {
			return null;
		} else if(dataHolder.expired()) {
			map.remove(key);
			return null;
		} else {
			return (X) dataHolder.get();
		}
	}
	
	public void touchKeys() {
		ArrayList<String> allKeys = new ArrayList<>( map.keySet() );
		allKeys.forEach(key->get(key));
	}
	
}

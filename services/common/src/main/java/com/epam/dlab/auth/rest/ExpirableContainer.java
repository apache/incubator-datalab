/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

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

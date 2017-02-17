package com.epam.dlab.auth.core;/*
Copyright 2016 EPAM Systems, Inc.
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import com.aegisql.conveyor.BuilderSupplier;
import com.epam.dlab.auth.UserInfo;

import java.util.function.Supplier;

public class CacheableReference<T> implements Supplier<T> {

    /** The reference. */
    private final T reference;

    /**
     * Instantiates a new immutable reference.
     *
     * @param ref the ref
     */
    private CacheableReference(T ref) {
        this.reference = ref;
    }

    /* (non-Javadoc)
     * @see java.util.function.Supplier#get()
     */
    @Override
    public T get() {
        return reference;
    }

    public static <T> BuilderSupplier<T> newInstance(T ref) {
        return () -> new CacheableReference<>(ref);
    }

    @Override
    public String toString() {
        return ""+reference;
    }
}

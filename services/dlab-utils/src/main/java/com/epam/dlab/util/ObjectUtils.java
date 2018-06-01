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

package com.epam.dlab.util;

import java.util.Objects;
import java.util.function.Function;

public final class ObjectUtils {
    private ObjectUtils() { }

    @SafeVarargs
    public static<T> boolean areObjectsEqual(T thiz, Object that,
                                             Function<T, Object>... getters) {
        if(thiz == that) {
            return true;
        }

        if (that == null || thiz.getClass() != that.getClass()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        T typedThat = (T)that;

        for(Function<T, Object> getter: getters) {
            if(!Objects.equals(getter.apply(thiz), getter.apply(typedThat))) {
                return false;
            }
        }
        return true;
    }
}

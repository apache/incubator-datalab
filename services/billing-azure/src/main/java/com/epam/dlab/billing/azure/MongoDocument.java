/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.billing.azure;

import com.epam.dlab.exceptions.DlabException;
import org.bson.Document;

import java.lang.reflect.Field;

public abstract class MongoDocument<T> {

    protected Document to() {
        Field[] fields = this.getClass().getDeclaredFields();
        Document document = new Document();

        try {
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getType().isEnum()) {
                    document.append(field.getName(), field.get(this).toString());
                } else {
                    document.append(field.getName(), field.get(this));
                }
            }

            return document;

        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new DlabException("", e);
        }
    }

    @SuppressWarnings("unchecked")
    private T from(Document document) {

        Field[] fields = this.getClass().getDeclaredFields();

        try {
            for (Field field : fields) {
                field.setAccessible(true);

                if (field.getType().isEnum()) {
                    field.set(this, Enum.valueOf((Class<Enum>) field.getType(), (String) document.get(field.getName())));
                } else {
                    field.set(this, document.get(field.getName()));
                }
            }
            return (T) this;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new DlabException("", e);
        }
    }
}

/*
 *  Copyright 2019-2020 author
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.easy.reflection.scanners.impl;

import org.easy.reflection.Store;
import org.easy.reflection.scanners.AbstractScanner;
import org.easy.reflection.util.StringUtils;

/**
 * scans fields and methods and stores fqn as key and elements as values
 */
@SuppressWarnings({"unchecked"})
public class TypeElementsScanner extends AbstractScanner {
    private boolean includeFields = true;
    private boolean includeMethods = true;
    private boolean includeAnnotations = true;
    private boolean publicOnly = true;

    @Override
    public void scan(Object cls, Store store) {
        String className = getMetadataAdapter().getClassName(cls);
        if (!acceptFilter(className)) return;

        put(store, className, "");

        if (includeFields) {
            for (Object field : getMetadataAdapter().getFields(cls)) {
                String fieldName = getMetadataAdapter().getFieldName(field);
                put(store, className, fieldName);
            }
        }

        if (includeMethods) {
            for (Object method : getMetadataAdapter().getMethods(cls)) {
                if (!publicOnly || getMetadataAdapter().isPublic(method)) {
                    String methodKey = getMetadataAdapter().getMethodName(method) + "(" +
                           StringUtils.append(getMetadataAdapter().getParameterNames(method), ", ") + ")";
                    put(store, className, methodKey);
                }
            }
        }

        if (includeAnnotations) {
            for (Object annotation : getMetadataAdapter().getClassAnnotationNames(cls)) {
                put(store, className, "@" + annotation);
            }
        }
    }

    //
    public TypeElementsScanner includeFields() {
        return includeFields(true);
    }

    public TypeElementsScanner includeFields(boolean include) {
        includeFields = include;
        return this;
    }

    public TypeElementsScanner includeMethods() {
        return includeMethods(true);
    }

    public TypeElementsScanner includeMethods(boolean include) {
        includeMethods = include;
        return this;
    }

    public TypeElementsScanner includeAnnotations() {
        return includeAnnotations(true);
    }

    public TypeElementsScanner includeAnnotations(boolean include) {
        includeAnnotations = include;
        return this;
    }

    public TypeElementsScanner publicOnly(boolean only) {
        publicOnly = only;
        return this;
    }

    public TypeElementsScanner publicOnly() {
        return publicOnly(true);
    }
}

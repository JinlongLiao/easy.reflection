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

import java.util.List;

/**
 * scans for field's annotations
 */
@SuppressWarnings({"unchecked"})
public class FieldAnnotationsScanner extends AbstractScanner {
    @Override
    public void scan(final Object cls, Store store) {
        final String className = getMetadataAdapter().getClassName(cls);
        List<Object> fields = getMetadataAdapter().getFields(cls);
        for (final Object field : fields) {
            List<String> fieldAnnotations = getMetadataAdapter().getFieldAnnotationNames(field);
            for (String fieldAnnotation : fieldAnnotations) {

                if (acceptFilter(fieldAnnotation)) {
                    String fieldName = getMetadataAdapter().getFieldName(field);
                    put(store, fieldAnnotation, String.format("%s.%s", className, fieldName));
                }
            }
        }
    }
}

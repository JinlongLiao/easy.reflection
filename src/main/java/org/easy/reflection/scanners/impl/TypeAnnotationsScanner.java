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

import java.lang.annotation.Inherited;
import java.util.List;

/**
 * scans for class's annotations, where @Retention(RetentionPolicy.RUNTIME)
 *
 * @author ronmamo
 */
@SuppressWarnings({"unchecked"})
public class TypeAnnotationsScanner extends AbstractScanner {
    @Override
    public void scan(final Object cls, Store store) {
        final String className = getMetadataAdapter().getClassName(cls);

        for (String annotationType : (List<String>) getMetadataAdapter().getClassAnnotationNames(cls)) {
            //as an exception, accept Inherited as well
            if (acceptFilter(annotationType) ||
                    annotationType.equals(Inherited.class.getName())) {
                put(store, annotationType, className);
            }
        }
    }

}

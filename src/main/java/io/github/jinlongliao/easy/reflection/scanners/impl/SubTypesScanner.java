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

package io.github.jinlongliao.easy.reflection.scanners.impl;

import io.github.jinlongliao.easy.reflection.Store;
import io.github.jinlongliao.easy.reflection.scanners.AbstractScanner;
import io.github.jinlongliao.easy.reflection.util.ScannerFilter;

import java.util.List;

/**
 * scans for superclass and interfaces of a class, allowing a reverse lookup for subtypes
 */
public class SubTypesScanner extends AbstractScanner {

    /**
     * created new SubTypesScanner. will exclude direct Object subtypes
     */
    public SubTypesScanner() {
        //exclude direct Object subtypes by default
        this(true);
    }

    /**
     * created new SubTypesScanner.
     *
     * @param excludeObjectClass if false, include direct {@link Object} subtypes in results.
     */
    public SubTypesScanner(boolean excludeObjectClass) {
        if (excludeObjectClass) {
            //exclude direct Object subtypes
            this.setScannerFilter(new ScannerFilter().exclude(Object.class.getName()));
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void scan(final Object cls, Store store) {
        String className = getMetadataAdapter().getClassName(cls);
        String superclass = getMetadataAdapter().getSuperclassName(cls);

        if (acceptFilter(superclass)) {
            put(store, superclass, className);
        }

        for (String anInterface : (List<String>) getMetadataAdapter().getInterfacesNames(cls)) {
            if (acceptFilter(anInterface)) {
                put(store, anInterface, className);
            }
        }
    }
}

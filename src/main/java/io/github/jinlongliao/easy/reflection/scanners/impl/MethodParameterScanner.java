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
import io.github.jinlongliao.easy.reflection.adapters.MetadataAdapter;
import io.github.jinlongliao.easy.reflection.scanners.AbstractScanner;

import java.util.List;

/**
 * scans methods/constructors and indexes parameters, return type and parameter annotations
 *
 * @author ronma
 * @date 20180324
 */
public class MethodParameterScanner extends AbstractScanner {

    @Override
    public void scan(Object cls, Store store) {
        final MetadataAdapter md = getMetadataAdapter();
        for (Object method : md.getMethods(cls)) {

            String signature = md.getParameterNames(method).toString();
            if (acceptFilter(signature)) {
                put(store, signature, md.getMethodFullKey(cls, method));
            }

            String returnTypeName = md.getReturnTypeName(method);
            if (acceptFilter(returnTypeName)) {
                put(store, returnTypeName, md.getMethodFullKey(cls, method));
            }

            List<String> parameterNames = md.getParameterNames(method);
            for (int i = 0; i < parameterNames.size(); i++) {
                for (Object paramAnnotation : md.getParameterAnnotationNames(method, i)) {
                    if (acceptFilter((String) paramAnnotation)) {
                        put(store, (String) paramAnnotation, md.getMethodFullKey(cls, method));
                    }
                }
            }
        }
    }
}

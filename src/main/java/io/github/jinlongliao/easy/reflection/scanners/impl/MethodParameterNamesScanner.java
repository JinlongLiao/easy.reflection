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
import io.github.jinlongliao.easy.reflection.util.StringUtils;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * scans methods/constructors and indexes parameter names
 */
@SuppressWarnings("unchecked")
public class MethodParameterNamesScanner extends AbstractScanner {

    @Override
    public void scan(Object cls, Store store) {
        final MetadataAdapter md = getMetadataAdapter();

        for (Object method : md.getMethods(cls)) {
            String key = md.getMethodFullKey(cls, method);
            if (acceptFilter(key)) {
                CodeAttribute codeAttribute = ((MethodInfo) method).getCodeAttribute();
                LocalVariableAttribute table = codeAttribute != null ? (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag) : null;
                int length = table != null ? table.tableLength() : 0;
                //skip this
                int i = Modifier.isStatic(((MethodInfo) method).getAccessFlags()) ? 0 : 1;
                if (i < length) {
                    List<String> names = new ArrayList<>(length - i);
                    while (i < length) {
                        names.add(((MethodInfo) method).getConstPool().getUtf8Info(table.nameIndex(i++)));
                    }
                    put(store, key, StringUtils.append(names, ", "));
                }
            }
        }
    }
}

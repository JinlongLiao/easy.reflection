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
import io.github.jinlongliao.easy.reflection.vfs.Vfs;

/** collects all resources that are not classes in a collection
 * <p>key: value - {web.xml: WEB-INF/web.xml} */
public class ResourcesScanner extends AbstractScanner {
    @Override
    public boolean support(String file) {
        return !file.endsWith(".class"); //not a class
    }

    @Override public Object scan(Vfs.File file, Object classObject, Store store) {
        put(store, file.getName(), file.getRelativePath());
        return classObject;
    }

    @Override
    public void scan(Object cls, Store store) {
        throw new UnsupportedOperationException(); //shouldn't get here
    }
}

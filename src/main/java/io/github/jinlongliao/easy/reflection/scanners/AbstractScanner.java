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

package io.github.jinlongliao.easy.reflection.scanners;

import io.github.jinlongliao.easy.reflection.exception.ReflectionsException;
import io.github.jinlongliao.easy.reflection.Configuration;
import io.github.jinlongliao.easy.reflection.Store;
import io.github.jinlongliao.easy.reflection.util.ClassUtils;
import io.github.jinlongliao.easy.reflection.vfs.Vfs;
import io.github.jinlongliao.easy.reflection.adapters.MetadataAdapter;

import java.util.function.Predicate;

/**
 * @author ronmamo
 * @author liaojinlong
 */
public abstract class AbstractScanner implements Scanner {

    private Configuration configuration;
    /**
     * support all by default
     */
    private Predicate<String> scannerFilter = s -> true;

    @Override
    public boolean support(String file) {
        return getMetadataAdapter().support(file);
    }

    @Override
    public Object scan(Vfs.File file, Object classObject, Store store) {
        if (classObject == null) {
            try {
                classObject = configuration.getMetadataAdapter().getOrCreateClassObject(file);
            } catch (Exception e) {
                throw new ReflectionsException("could not create class object from file " + file.getRelativePath(), e);
            }
        }
        scan(classObject, store);
        return classObject;
    }

    public abstract void scan(Object cls, Store store);

    protected void put(Store store, String key, String value) {
        store.put(ClassUtils.getClassSimpleName(getClass()), key, value);
    }

    /**
     * @return {@link Configuration}
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return 扫描过滤条件
     */
    public Predicate<String> getScannerFilter() {
        return scannerFilter;
    }

    /**
     * 设置扫描过滤条件
     *
     * @param scannerFilter
     */
    public void setScannerFilter(Predicate<String> scannerFilter) {
        this.scannerFilter = scannerFilter;
    }

    @Override
    public Scanner setFilter(Predicate<String> filter) {
        this.setScannerFilter(filter);
        return this;
    }

    //
    @Override
    public boolean acceptFilter(final String fqn) {
        return fqn != null && scannerFilter.test(fqn);
    }

    /**
     * @return 元数据适配类
     */
    protected MetadataAdapter getMetadataAdapter() {
        return configuration.getMetadataAdapter();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

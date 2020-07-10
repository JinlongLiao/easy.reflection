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

import io.github.jinlongliao.easy.reflection.Configuration;
import io.github.jinlongliao.easy.reflection.Store;
import io.github.jinlongliao.easy.reflection.util.ScannerFilter;
import io.github.jinlongliao.easy.reflection.vfs.Vfs;

import java.util.function.Predicate;

/**
 * @author ronmamo
 * @author liaojinlong
 */
public interface Scanner {

    /**
     * 设置 reflections的扫描 配置
     *
     * @param configuration
     */
    void setConfiguration(Configuration configuration);

    /**
     * 设置扫描的 过滤条件
     *
     * @param filter
     * @return /
     */
    Scanner setFilter(Predicate<String> filter);

    /**
     * 此路径是否支持扫描
     *
     * @param file
     * @return /
     */
    boolean support(String file);

    /**
     * 扫描 {@link Vfs.File},将元信息存储到{@link Store}
     *
     * @param file
     * @param classObject
     * @param store
     * @return /
     */
    Object scan(Vfs.File file, Object classObject, Store store);

    /**
     * 是否通过过滤条件
     *
     * @param fqn
     * @return /
     * @see {@link ScannerFilter}
     * @see {@link  java.util.function.Predicate}
     * @see {@link Scanner#setFilter(java.util.function.Predicate)}
     */
    boolean acceptFilter(String fqn);
}

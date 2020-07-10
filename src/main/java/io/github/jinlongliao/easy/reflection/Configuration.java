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

package io.github.jinlongliao.easy.reflection;

import io.github.jinlongliao.easy.reflection.adapters.MetadataAdapter;
import io.github.jinlongliao.easy.reflection.scanners.Scanner;
import io.github.jinlongliao.easy.reflection.serializers.Serializer;
import io.github.jinlongliao.easy.reflection.util.ConfigurationBuilder;

import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

/**
 * Configuration is used to create a configured instance of {@link Reflections}
 * <p>it is preferred to use {@link ConfigurationBuilder}
 *
 * @author ronmamo
 */
public interface Configuration {

    /**
     * the scanner instances used for scanning different metadata
     * 用于扫描不同的 元数据 eg:类，方法，注解 等
     *
     * @return / {@link Scanner}
     */
    Set<Scanner> getScanners();

    /**
     * the urls to be scanned
     *
     * @return /
     */
    Set<URL> getUrls();

    /**
     * the metadata adapter used to fetch metadata from classes
     *
     * @return /
     */
    MetadataAdapter getMetadataAdapter();

    /**
     * get the fully qualified name filter used to filter types to be scanned
     */
    Predicate<String> getInputsFilter();

    /**
     * executor service used to scan files. if null, scanning is done in a simple for loop
     */
    ExecutorService getExecutorService();

    /**
     * the default serializer to use when saving Reflection
     */
    Serializer getSerializer();

    /**
     * get class loaders, might be used for resolving methods/fields
     */
    ClassLoader[] getClassLoaders();

    /**
     * if true (default), expand super types after scanning, for super types that were not scanned.
     * <p>see {@link Reflections#expandSuperTypes()}
     */
    boolean shouldExpandSuperTypes();
}

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

package org.easy.reflection.serializers;

import org.easy.reflection.Reflections;

import java.io.File;
import java.io.InputStream;

/**
 * Serilizer of a {@link Reflections} instance
 *
 * @author ronmamo ,liaojinlong
 */
public interface Serializer {
    /**
     * reads the input stream into a new Reflections instance, populating it's store
     *
     * @return {@link Reflections}
     */
    Reflections read(InputStream inputStream);

    /**
     * saves a Reflections instance into the given filename
     *
     * @return {@link File}
     */
    File save(Reflections reflections, String filename);

    /**
     * @returns a string serialization of the given Reflections instance
     */
    String toString(Reflections reflections);

    /**
     * 文件后缀名 的名称
     *
     * @return 文件后缀名
     */
    String getSuffixFileName(String fileName);
}

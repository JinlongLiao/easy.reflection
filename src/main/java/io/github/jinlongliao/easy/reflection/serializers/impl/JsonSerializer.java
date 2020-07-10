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

package io.github.jinlongliao.easy.reflection.serializers.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jinlongliao.easy.reflection.exception.ReflectionsException;
import io.github.jinlongliao.easy.reflection.serializers.Serializer;
import io.github.jinlongliao.easy.reflection.Reflections;
import io.github.jinlongliao.easy.reflection.util.ConfigurationBuilder;
import io.github.jinlongliao.easy.reflection.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;

/**
 * serialization of Reflections to json
 *
 * <p>an example of produced json:
 * <pre>
 *    {"org.reflections.scanners.impl.TypeAnnotationsScanner":{
 *       "org.reflections.TestModel$AC1":["org.reflections.TestModel$C1"],
 *       "org.reflections.TestModel$AC2":["org.reflections.TestModel$I3",
 * ...
 * </pre>
 */
public class JsonSerializer implements Serializer {
    private Gson gson;

    @Override
    public Reflections read(InputStream inputStream) {
        final Map<String, Collection<String>> storeMap = getGson().fromJson(new InputStreamReader(inputStream), Map.class);
        return new Reflections(new ConfigurationBuilder());
    }

    @Override
    public File save(Reflections reflections, String filename) {
        try {
            File file = FileUtils.prepareFile(getSuffixFileName(FileUtils.getUnixPath(filename)));
            Files.write(file.toPath(), toString(reflections).getBytes(Charset.defaultCharset()));
            return file;
        } catch (IOException e) {
            throw new ReflectionsException(e);
        }
    }

    @Override
    public String toString(Reflections reflections) {
        return getGson().toJson(reflections.getStore().getStoreMap());
    }

    public static final String SUFFIX = ".json";

    /**
     * 文件后缀名
     *
     * @return 文件后缀名
     */
    @Override
    public String getSuffixFileName(String fileName) {
        if (!fileName.toLowerCase().endsWith(SUFFIX)) {
            fileName += SUFFIX;
        }
        return fileName;
    }

    private Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder().setPrettyPrinting().create();
        }
        return gson;
    }
}

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

import io.github.jinlongliao.easy.reflection.Reflections;
import io.github.jinlongliao.easy.reflection.Store;
import io.github.jinlongliao.easy.reflection.exception.ReflectionsException;
import io.github.jinlongliao.easy.reflection.serializers.Serializer;
import io.github.jinlongliao.easy.reflection.util.ConfigurationBuilder;
import io.github.jinlongliao.easy.reflection.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;

/**
 * serialization of Reflections to xml
 *
 * <p>an example of produced xml:
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *
 * &lt;Reflections&gt;
 *  &lt;SubTypesScanner&gt;
 *      &lt;entry&gt;
 *          &lt;key&gt;com.google.inject.Module&lt;/key&gt;
 *          &lt;values&gt;
 *              &lt;value&gt;fully.qualified.name.1&lt;/value&gt;
 *              &lt;value&gt;fully.qualified.name.2&lt;/value&gt;
 * ...
 * </pre>
 */
public class XmlSerializer implements Serializer {

    @Override
    public Reflections read(InputStream inputStream) {
        Reflections reflections;
        try {
            Constructor<Reflections> constructor = Reflections.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            reflections = constructor.newInstance();
        } catch (Exception e) {
            reflections = new Reflections(new ConfigurationBuilder());
        }

        try {
            Document document = new SAXReader().read(inputStream);
            for (Object e1 : document.getRootElement().elements()) {
                Element index = (Element) e1;
                for (Object e2 : index.elements()) {
                    Element entry = (Element) e2;
                    Element key = entry.element("key");
                    Element values = entry.element("values");
                    for (Object o3 : values.elements()) {
                        Element value = (Element) o3;
                        reflections.getStore().put(index.getName(), key.getText(), value.getText());
                    }
                }
            }
        } catch (DocumentException e) {
            throw new ReflectionsException("could not read.", e);
        } catch (Throwable e) {
            throw new RuntimeException("Could not read. Make sure relevant dependencies exist on classpath.", e);
        }

        return reflections;
    }

    @Override
    public File save(final Reflections reflections, final String filename) {
        File file = FileUtils.prepareFile(getSuffixFileName(FileUtils.getUnixPath(filename)));
        try {
            Document document = createDocument(reflections);
            XMLWriter xmlWriter = new XMLWriter(new FileOutputStream(file), OutputFormat.createPrettyPrint());
            xmlWriter.write(document);
            xmlWriter.close();
        } catch (IOException e) {
            throw new ReflectionsException("could not save to file " + filename, e);
        } catch (Throwable e) {
            throw new ReflectionsException("Could not save to file " + filename + ". Make sure relevant dependencies exist on classpath.", e);
        }

        return file;
    }

    @Override
    public String toString(final Reflections reflections) {
        Document document = createDocument(reflections);

        try {
            StringWriter writer = new StringWriter();
            XMLWriter xmlWriter = new XMLWriter(writer, OutputFormat.createPrettyPrint());
            xmlWriter.write(document);
            xmlWriter.close();
            return writer.toString();
        } catch (IOException e) {
            throw new ReflectionsException(e);
        }
    }

    public static final String SUFFIX = ".xml";

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

    private Document createDocument(final Reflections reflections) {
        Store map = reflections.getStore();
        Document document = DocumentFactory.getInstance().createDocument();
        Element root = document.addElement("Reflections");
        for (String indexName : map.keySet()) {
            Element indexElement = root.addElement(indexName);
            for (String key : map.keys(indexName)) {
                Element entryElement = indexElement.addElement("entry");
                entryElement.addElement("key").setText(key);
                Element valuesElement = entryElement.addElement("values");
                for (String value : map.get(indexName, key)) {
                    valuesElement.addElement("value").setText(value);
                }
            }
        }
        return document;
    }
}

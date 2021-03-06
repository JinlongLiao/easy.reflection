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

import io.github.jinlongliao.easy.reflection.exception.ReflectionsException;
import io.github.jinlongliao.easy.reflection.serializers.Serializer;
import io.github.jinlongliao.easy.reflection.ReflectionUtils;
import io.github.jinlongliao.easy.reflection.Reflections;
import io.github.jinlongliao.easy.reflection.scanners.impl.TypeElementsScanner;
import io.github.jinlongliao.easy.reflection.util.ClassUtils;
import io.github.jinlongliao.easy.reflection.util.FileUtils;
import io.github.jinlongliao.easy.reflection.util.LogUtil;
import io.github.jinlongliao.easy.reflection.util.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Serialization of Reflections to java code
 * <p> Serializes types and types elements into interfaces respectively to fully qualified name,
 * <p> For example, after saving with JavaCodeSerializer:
 * <pre>
 *   reflections.save(filename, new JavaCodeSerializer());
 * </pre>
 * <p>Saved file should look like:
 * <pre>
 *     public interface MyModel {
 *      public interface my {
 *       public interface package1 {
 *        public interface MyClass1 {
 *         public interface fields {
 *          public interface f1 {}
 *          public interface f2 {}
 *         }
 *         public interface methods {
 *          public interface m1 {}
 *          public interface m2 {}
 *         }
 * 	...
 * }
 * </pre>
 * <p> Use the different resolve methods to resolve the serialized element into Class, Field or Method. for example:
 * <pre>
 *  Class m1Ref = MyModel.my.package1.MyClass1.methods.m1.class;
 *  Method method = JavaCodeSerializer.resolve(m1Ref);
 * </pre>
 * <p>The {@link #save(Reflections, String)} method filename should be in the pattern: path/path/path/package.package.classname
 * <p>depends on Reflections configured with {@link TypeElementsScanner}
 */
public class JavaCodeSerializer implements Serializer {
    private static Logger log = LogUtil.findLogger(JavaCodeSerializer.class);

    private static final String pathSeparator = "_";
    private static final String doubleSeparator = "__";
    private static final String dotSeparator = ".";
    private static final String arrayDescriptor = "$$";
    private static final String tokenSeparator = "_";
    public static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public Reflections read(InputStream inputStream) {
        throw new UnsupportedOperationException("read is not implemented on JavaCodeSerializer");
    }

    /**
     * name should be in the pattern: path/path/path/package.package.classname,
     * for example <pre>/data/projects/my/src/main/java/org.my.project.MyStore</pre>
     * would create class MyStore in package org.my.project in the path /data/projects/my/src/main/java
     */
    @Override
    public File save(Reflections reflections, String name) {
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1); //trim / at the end
        }

        //prepare file
        String filename = getSuffixFileName(FileUtils.classPath2SystemPath(name));
        File file = FileUtils.prepareFile(filename);

        //get package and class names
        String packageName;
        String className;
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            packageName = "";
            className = name.substring(name.lastIndexOf('/') + 1);
        } else {
            packageName = name.substring(name.lastIndexOf('/') + 1, lastDot);
            className = name.substring(lastDot + 1);
        }

        //generate
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("/**\n *generated using easy-reflection JavaCodeSerializer\n")
                    .append(" *使用 easy-reflection JavaCodeSerializer 生成\n")
                    .append(" *[").append(DTF.format(LocalDateTime.now())).append("]\n")
                    .append(" **/\n");
            if (packageName.length() != 0) {
                sb.append("package ").append(packageName).append(";\n");
                sb.append("\n");
            }
            sb.append("public interface ").append(className).append(" {\n\n");
            sb.append(toString(reflections));
            sb.append("}\n");

            Files.write(new File(filename).toPath(), sb.toString().getBytes(Charset.defaultCharset()));

        } catch (IOException e) {
            throw new RuntimeException();
        }

        return file;
    }

    @Override
    public String toString(Reflections reflections) {
        if (reflections.getStore().keys(ClassUtils.getClassSimpleName(TypeElementsScanner.class)).isEmpty()) {
            log.warn("JavaCodeSerializer needs TypeElementsScanner configured");
        }

        StringBuilder sb = new StringBuilder();

        List<String> prevPaths = new ArrayList<>();
        int indent = 1;

        List<String> keys = new ArrayList<>(reflections.getStore().keys(ClassUtils.getClassSimpleName(TypeElementsScanner.class)));
        Collections.sort(keys);
        for (String fqn : keys) {
            List<String> typePaths = Arrays.asList(fqn.split("\\."));

            //skip indention
            int i = 0;
            while (i < Math.min(typePaths.size(), prevPaths.size()) && typePaths.get(i).equals(prevPaths.get(i))) {
                i++;
            }

            //indent left
            for (int j = prevPaths.size(); j > i; j--) {
                sb.append(StringUtils.repeat("\t", --indent)).append("}\n");
            }

            //indent right - add packages
            for (int j = i; j < typePaths.size() - 1; j++) {
                sb.append(StringUtils.repeat("\t", indent++)).append("public interface ").append(getNonDuplicateName(typePaths.get(j), typePaths, j)).append(" {\n");
            }

            //indent right - add class
            String className = typePaths.get(typePaths.size() - 1);

            //get fields and methods
            List<String> annotations = new ArrayList<>();
            List<String> fields = new ArrayList<>();
            List<String> methods = new ArrayList<>();

            Iterable<String> members = reflections.getStore().get(ClassUtils.getClassSimpleName(TypeElementsScanner.class), fqn);
            List<String> sorted = StreamSupport.stream(members.spliterator(), false).sorted().collect(Collectors.toList());
            for (String element : sorted) {
                if (element.startsWith("@")) {
                    annotations.add(element.substring(1));
                } else if (element.contains("(")) {
                    //method
                    if (!element.startsWith("<")) {
                        int i1 = element.indexOf('(');
                        String name = element.substring(0, i1);
                        String params = element.substring(i1 + 1, element.indexOf(")"));

                        String paramsDescriptor = "";
                        if (params.length() != 0) {
                            paramsDescriptor = tokenSeparator + params.replace(dotSeparator, tokenSeparator).replace(", ", doubleSeparator).replace("[]", arrayDescriptor);
                        }
                        String normalized = name + paramsDescriptor;
                        if (!methods.contains(name)) {
                            methods.add(name);
                        } else {
                            methods.add(normalized);
                        }
                    }
                } else if (!StringUtils.isBlank(element)) {
                    //field
                    fields.add(element);
                }
            }

            //add class and it's fields and methods
            sb.append(StringUtils.repeat("\t", indent++)).append("public interface ").append(getNonDuplicateName(className, typePaths, typePaths.size() - 1)).append(" {\n");

            //add fields
            if (!fields.isEmpty()) {
                sb.append(StringUtils.repeat("\t", indent++)).append("public interface fields {\n");
                for (String field : fields) {
                    sb.append(StringUtils.repeat("\t", indent)).append("public interface ").append(getNonDuplicateName(field, typePaths)).append(" {}\n");
                }
                sb.append(StringUtils.repeat("\t", --indent)).append("}\n");
            }

            //add methods
            if (!methods.isEmpty()) {
                sb.append(StringUtils.repeat("\t", indent++)).append("public interface methods {\n");
                for (String method : methods) {
                    String methodName = getNonDuplicateName(method, fields);

                    sb.append(StringUtils.repeat("\t", indent)).append("public interface ").append(getNonDuplicateName(methodName, typePaths)).append(" {}\n");
                }
                sb.append(StringUtils.repeat("\t", --indent)).append("}\n");
            }

            //add annotations
            if (!annotations.isEmpty()) {
                sb.append(StringUtils.repeat("\t", indent++)).append("public interface annotations {\n");
                for (String annotation : annotations) {
                    String nonDuplicateName = annotation;
                    nonDuplicateName = getNonDuplicateName(nonDuplicateName, typePaths);
                    sb.append(StringUtils.repeat("\t", indent)).append("public interface ").append(nonDuplicateName).append(" {}\n");
                }
                sb.append(StringUtils.repeat("\t", --indent)).append("}\n");
            }

            prevPaths = typePaths;
        }


        //close indention
        for (int j = prevPaths.size(); j >= 1; j--) {
            sb.append(StringUtils.repeat("\t", j)).append("}\n");
        }

        return sb.toString();
    }

    public static final String SUFFIX = ".java";

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

    private String getNonDuplicateName(String candidate, List<String> prev, int offset) {
        String normalized = normalize(candidate);
        for (int i = 0; i < offset; i++) {
            if (normalized.equals(prev.get(i))) {
                return getNonDuplicateName(normalized + tokenSeparator, prev, offset);
            }
        }

        return normalized;
    }

    private String normalize(String candidate) {
        return candidate.replace(dotSeparator, pathSeparator);
    }

    private String getNonDuplicateName(String candidate, List<String> prev) {
        return getNonDuplicateName(candidate, prev, prev.size());
    }

    //
    public static Class<?> resolveClassOf(final Class element) throws ClassNotFoundException {
        Class<?> cursor = element;
        LinkedList<String> ognl = new LinkedList<>();

        while (cursor != null) {
            ognl.addFirst(cursor.getSimpleName());
            cursor = cursor.getDeclaringClass();
        }

        String classOgnl = StringUtils.append(ognl.subList(1, ognl.size()), ".").replace(".$", "$");
        return Class.forName(classOgnl);
    }

    /**
     * get Class instance
     *
     * @param aClass
     * @return /
     */
    public static Class<?> resolveClass(final Class aClass) {
        try {
            return resolveClassOf(aClass);
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to class " + aClass.getName(), e);
        }
    }

    public static Field resolveField(final Class aField) {
        try {
            String name = aField.getSimpleName();
            Class<?> declaringClass = aField.getDeclaringClass().getDeclaringClass();
            return resolveClassOf(declaringClass).getDeclaredField(name);
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to field " + aField.getName(), e);
        }
    }

    public static Annotation resolveAnnotation(Class annotation) {
        try {
            String name = annotation.getSimpleName().replace(pathSeparator, dotSeparator);
            Class<?> declaringClass = annotation.getDeclaringClass().getDeclaringClass();
            Class<?> aClass = resolveClassOf(declaringClass);
            Class<? extends Annotation> aClass1 = (Class<? extends Annotation>) ReflectionUtils.forName(name);
            Annotation annotation1 = aClass.getAnnotation(aClass1);
            return annotation1;
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to annotation " + annotation.getName(), e);
        }
    }

    public static Method resolveMethod(final Class aMethod) {
        String methodOgnl = aMethod.getSimpleName();

        try {
            String methodName;
            Class<?>[] paramTypes;
            if (methodOgnl.contains(tokenSeparator)) {
                methodName = methodOgnl.substring(0, methodOgnl.indexOf(tokenSeparator));
                String[] params = methodOgnl.substring(methodOgnl.indexOf(tokenSeparator) + 1).split(doubleSeparator);
                paramTypes = new Class<?>[params.length];
                for (int i = 0; i < params.length; i++) {
                    String typeName = params[i].replace(arrayDescriptor, "[]").replace(pathSeparator, dotSeparator);
                    paramTypes[i] = ReflectionUtils.forName(typeName);
                }
            } else {
                methodName = methodOgnl;
                paramTypes = null;
            }

            Class<?> declaringClass = aMethod.getDeclaringClass().getDeclaringClass();
            return resolveClassOf(declaringClass).getDeclaredMethod(methodName, paramTypes);
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to method " + aMethod.getName(), e);
        }
    }
}

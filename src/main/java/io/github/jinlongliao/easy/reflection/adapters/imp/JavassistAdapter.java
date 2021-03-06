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

package io.github.jinlongliao.easy.reflection.adapters.imp;

import io.github.jinlongliao.easy.reflection.adapters.MetadataAdapter;
import io.github.jinlongliao.easy.reflection.exception.ReflectionsException;
import io.github.jinlongliao.easy.reflection.util.FileUtils;
import io.github.jinlongliao.easy.reflection.vfs.Vfs;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javassist.bytecode.AccessFlag.isPrivate;
import static javassist.bytecode.AccessFlag.isProtected;

/**
 * 依赖 javassist 获取Java 信息
 *
 * @author ronmamo
 */
public class JavassistAdapter implements MetadataAdapter<ClassFile, FieldInfo, MethodInfo> {

    /**
     * setting this to false will result in returning only visible annotations from the relevant methods here (only {@link java.lang.annotation.RetentionPolicy#RUNTIME})
     */
    public static boolean includeInvisibleTag = true;

    @Override
    public List<FieldInfo> getFields(final ClassFile cls) {
        return cls.getFields();
    }

    @Override
    public List<MethodInfo> getMethods(final ClassFile cls) {
        return cls.getMethods();
    }

    @Override
    public String getMethodName(final MethodInfo method) {
        return method.getName();
    }

    @Override
    public List<String> getParameterNames(final MethodInfo method) {
        String descriptor = method.getDescriptor();
        descriptor = descriptor.substring(descriptor.indexOf("(") + 1, descriptor.lastIndexOf(")"));
        return splitDescriptorToTypeNames(descriptor);
    }

    @Override
    public List<String> getClassAnnotationNames(final ClassFile aClass) {
        return getAnnotationNames((AnnotationsAttribute) aClass.getAttribute(AnnotationsAttribute.visibleTag),
                includeInvisibleTag ? (AnnotationsAttribute) aClass.getAttribute(AnnotationsAttribute.invisibleTag) : null);
    }

    @Override
    public List<String> getFieldAnnotationNames(final FieldInfo field) {
        return getAnnotationNames((AnnotationsAttribute) field.getAttribute(AnnotationsAttribute.visibleTag),
                includeInvisibleTag ? (AnnotationsAttribute) field.getAttribute(AnnotationsAttribute.invisibleTag) : null);
    }

    @Override
    public List<String> getMethodAnnotationNames(final MethodInfo method) {
        return getAnnotationNames((AnnotationsAttribute) method.getAttribute(AnnotationsAttribute.visibleTag),
                includeInvisibleTag ? (AnnotationsAttribute) method.getAttribute(AnnotationsAttribute.invisibleTag) : null);
    }

    @Override
    public List<String> getParameterAnnotationNames(final MethodInfo method, final int parameterIndex) {
        List<String> result = new ArrayList<>();

        List<ParameterAnnotationsAttribute> parameterAnnotationsAttributes = Arrays.asList(
                (ParameterAnnotationsAttribute) method.getAttribute(ParameterAnnotationsAttribute.visibleTag),
                (ParameterAnnotationsAttribute) method.getAttribute(ParameterAnnotationsAttribute.invisibleTag));

        for (ParameterAnnotationsAttribute parameterAnnotationsAttribute : parameterAnnotationsAttributes) {
            if (parameterAnnotationsAttribute != null) {
                Annotation[][] annotations = parameterAnnotationsAttribute.getAnnotations();
                if (parameterIndex < annotations.length) {
                    Annotation[] annotation = annotations[parameterIndex];
                    result.addAll(getAnnotationNames(annotation));
                }
            }
        }

        return result;
    }

    @Override
    public String getReturnTypeName(final MethodInfo method) {
        String descriptor = method.getDescriptor();
        descriptor = descriptor.substring(descriptor.lastIndexOf(")") + 1);
        return splitDescriptorToTypeNames(descriptor).get(0);
    }

    @Override
    public String getFieldName(final FieldInfo field) {
        return field.getName();
    }

    @Override
    public ClassFile getOrCreateClassObject(final Vfs.File file) {
        InputStream inputStream = null;
        try {
            inputStream = file.openInputStream();
            DataInputStream dis = new DataInputStream(new BufferedInputStream(inputStream));
            return new ClassFile(dis);
        } catch (IOException e) {
            throw new ReflectionsException("could not create class file from " + file.getName(), e);
        } finally {
            FileUtils.close(inputStream);
        }
    }

    @Override
    public String getMethodModifier(MethodInfo method) {
        int accessFlags = method.getAccessFlags();
        return isPrivate(accessFlags) ? "private" :
                isProtected(accessFlags) ? "protected" :
                        isPublic(accessFlags) ? "public" : "";
    }

    @Override
    public boolean isPublic(Object o) {
        Integer accessFlags =
                o instanceof ClassFile ? ((ClassFile) o).getAccessFlags() :
                        o instanceof FieldInfo ? ((FieldInfo) o).getAccessFlags() :
                                o instanceof MethodInfo ? ((MethodInfo) o).getAccessFlags() : null;

        return accessFlags != null && AccessFlag.isPublic(accessFlags);
    }

    @Override
    public String getClassName(final ClassFile cls) {
        return cls.getName();
    }

    @Override
    public String getSuperclassName(final ClassFile cls) {
        return cls.getSuperclass();
    }

    @Override
    public List<String> getInterfacesNames(final ClassFile cls) {
        return Arrays.asList(cls.getInterfaces());
    }

    private List<String> getAnnotationNames(final AnnotationsAttribute... annotationsAttributes) {
        if (annotationsAttributes != null) {
            return Arrays.stream(annotationsAttributes)
                    .filter(Objects::nonNull)
                    .flatMap(annotationsAttribute -> Arrays.stream(annotationsAttribute.getAnnotations()))
                    .map(Annotation::getTypeName)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private List<String> getAnnotationNames(final Annotation[] annotations) {
        return Arrays.stream(annotations).map(Annotation::getTypeName).collect(Collectors.toList());
    }

    private List<String> splitDescriptorToTypeNames(final String descriptors) {
        List<String> result = new ArrayList<>();

        if (descriptors != null && descriptors.length() != 0) {

            List<Integer> indices = new ArrayList<>();
            Descriptor.Iterator iterator = new Descriptor.Iterator(descriptors);
            while (iterator.hasNext()) {
                indices.add(iterator.next());
            }
            indices.add(descriptors.length());

            result = IntStream.range(0, indices.size() - 1)
                    .mapToObj(i -> Descriptor.toString(descriptors.substring(indices.get(i), indices.get(i + 1))))
                    .collect(Collectors.toList());
        }
        return result;
    }
}

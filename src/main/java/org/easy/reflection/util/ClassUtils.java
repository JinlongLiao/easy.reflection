/*
 *  Copyright 2019-2020
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

package org.easy.reflection.util;

import org.easy.reflection.ReflectionUtils;
import org.easy.reflection.exception.ReflectionsException;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Java metaData 相关操作 工具类
 *
 * @author liaojinlong
 * @date 2020/6/25 01:10
 */
public class ClassUtils {
    private static Logger log = LogUtil.findLogger(ClassUtils.class);
    public static final int LP = 40;
    public static final int RP = 41;
    public static final int $ = 36;
    public static final int SP = 32;
    public static final int DOT_INT = 46;
    public static final String COMMA = ",";

    /**
     * 依据方法的属性全路径 ，反射得到 java Field 属性
     * eg: java.lang.String.hash<br>
     * package:java.lang<br>
     * class:String<br>
     * field:hash<br>
     *
     * @param field
     * @param classLoaders
     * @return / {@link  java.lang.reflect.Field}
     */
    public static Field getFieldFromString(String field, ClassLoader... classLoaders) {
        int endIndex = field.lastIndexOf(DOT_INT);
        String className = field.substring(0, endIndex);
        String fieldName = field.substring(endIndex + 1);

        try {
            return ReflectionUtils.forName(className, classLoaders).getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new ReflectionsException("Can't resolve field named " + fieldName, e);
        }
    }

    /**
     * 依据class 描述字段  获取相应Class 属性<br>
     * <li>Field ：org.xx.xx.Test.test :获取 Test类的 test属性 </li>
     * <li>Method：org.xx.xx.Test.test(**) :获取 Test类的 test(**) 方法</li>
     * <li>Construct：org.xx.xx.Test.Test(**) :获取 Test类的Test(**)  构造函数</li>
     *
     * @param descriptor
     * @param classLoaders
     * @return / class 属性
     * @throws ReflectionsException
     */
    public static Member getMemberFromDescriptor(String descriptor, ClassLoader... classLoaders) throws ReflectionsException {
        /**
         * 筛选参数名称
         */
        int p0 = descriptor.lastIndexOf(LP);
        boolean isMethod = p0 != -1;
        String memberKey = isMethod ? descriptor.substring(0, p0) : descriptor;
        String methodParameters = isMethod ?
                descriptor.substring(p0 + 1, descriptor.lastIndexOf(RP)) : "";
        /**
         * 筛选类名与方法名
         */
        int p1 = Math.max(memberKey.lastIndexOf(DOT_INT), memberKey.lastIndexOf($));
        String className = memberKey.substring(memberKey.lastIndexOf(SP) + 1, p1);
        String memberName = memberKey.substring(p1 + 1);
        /**
         * 参数Class
         */
        Class<?>[] parameterTypes = null;
        if (!StringUtils.isBlank(methodParameters)) {
            String[] parameterNames = methodParameters.split(COMMA);
            parameterTypes = Arrays.stream(parameterNames)
                    .map(parameterName ->
                            ReflectionUtils.forName(parameterName.trim(), classLoaders))
                    .toArray(Class<?>[]::new);
        }
        /**
         * 注解类Class
         */
        Class<?> aClass = ReflectionUtils.forName(className, classLoaders);
        while (aClass != null) {
            try {
                /**
                 * 不包含括号，表示获取属性
                 */
                if (!isMethod) {
                    return aClass.isInterface() ?
                            aClass.getField(memberName) :
                            aClass.getDeclaredField(memberName);
                } else if (isConstructor(descriptor)) {
                    /**
                     * 为函数 ，确认是否为构造函数
                     */
                    return aClass.isInterface() ?
                            aClass.getConstructor(parameterTypes) :
                            aClass.getDeclaredConstructor(parameterTypes);
                } else {
                    /**
                     * 普通函数
                     */
                    return aClass.isInterface() ?
                            aClass.getMethod(memberName, parameterTypes) :
                            aClass.getDeclaredMethod(memberName, parameterTypes);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                aClass = aClass.getSuperclass();
            }
        }
        throw new ReflectionsException("Can't resolve member named " + memberName + " for class " + className);
    }

    /**
     * @param annotatedWith
     * @param classLoaders
     * @return / /
     * @see {@link ClassUtils#getMemberFromDescriptor(java.lang.String, java.lang.ClassLoader...)}
     * 为其批量获取{@link java.lang.reflect.Method}包装
     */
    public static Set<Method> getMethodsFromDescriptors(Iterable<String> annotatedWith, ClassLoader... classLoaders) {
        Set<Method> result = new HashSet<>();
        for (String annotated : annotatedWith) {
            if (!isConstructor(annotated)) {
                Method member = (Method) getMemberFromDescriptor(annotated, classLoaders);
                if (member != null) {
                    result.add(member);
                }
            }
        }
        return result;
    }

    /**
     * 获取 {@link java.lang.reflect.Constructor} 构造函数 ，
     * 但必须确认 class descriptor 为类的构造函数描述
     *
     * @param classDescriptors
     * @param classLoaders
     * @return /
     * @see {@link ClassUtils#getMemberFromDescriptor(java.lang.String, java.lang.ClassLoader...)}
     */
    public static Set<Constructor> getConstructorsFromDescriptors(Iterable<String> classDescriptors, ClassLoader... classLoaders) {
        Set<Constructor> result = new HashSet<>();
        for (String classDescriptor : classDescriptors) {
            if (isConstructor(classDescriptor)) {
                Constructor member = (Constructor) getMemberFromDescriptor(classDescriptor, classLoaders);
                if (member != null) {
                    result.add(member);
                }
            }
        }
        return result;
    }

    /**
     * @param descriptions
     * @param classLoaders
     * @return / /
     * @see {@link ClassUtils#getMemberFromDescriptor(java.lang.String, java.lang.ClassLoader...)}
     * 为其批量获取{@link java.lang.reflect.Method}
     * or {@link java.lang.reflect.Field}
     * or {@link java.lang.reflect.Constructor}   包装
     */
    public static Set<Member> getMembersFromDescriptors(Iterable<String> descriptions, ClassLoader... classLoaders) {
        Set<Member> result = new HashSet<>();
        for (String description : descriptions) {
            try {
                result.add(getMemberFromDescriptor(description, classLoaders));
            } catch (ReflectionsException e) {
                throw new ReflectionsException("Can't resolve member named " + description, e);
            }
        }
        return result;
    }

    /**
     * class 中的 函数名称是否为 构造函数
     *
     * @param fqn
     * @return / 是否为构造函数
     */
    public static boolean isConstructor(String fqn) {
        return fqn.contains("init>");
    }

    /**
     * 获取类名
     * <li>普通Object 直接为名称 eg: java.lang.String</li>
     * <li>数组 返回 数组标识 eg: java.lang.String[][]</li>
     *
     * @param type
     * @return /
     */
    public static String getFieldName(Class type) {
        if (!type.isArray()) {
            return type.getName();
        } else {
            int dim = 0;
            while (type.isArray()) {
                dim++;
                type = type.getComponentType();
            }
            return type.getName() + StringUtils.repeat("[]", dim);
        }
    }

    /**
     * 批量获取 获取类名
     *
     * @param types
     * @return / 获取集合中的各个类的全类名
     * @see {@link ClassUtils#getFieldName(java.lang.Class)}
     */
    public static List<String> getClassNames(Collection<Class<?>> types) {
        return types.stream().map(ClassUtils::getFieldName).collect(Collectors.toList());
    }

    /**
     * 批量获取 获取类名
     *
     * @param types
     * @return / 获取数组中的各个类的全类名
     * @see {@link ClassUtils#getFieldName(java.lang.Class)}
     */
    public static List<String> getClassNames(Class<?>... types) {
        return getClassNames(Arrays.asList(types));
    }

    /**
     * @param constructor
     * @return / 获取构造函数 Class表示名称
     */
    public static String getConstructorName(Constructor constructor) {
        return StringUtils.append(constructor.getName(),
                ".<init>(",
                StringUtils.append(
                        getClassNames(constructor.getParameterTypes()),
                        ", "),
                ")");
    }

    /**
     * 获取构造函数名称
     *
     * @param method
     * @return / 构造函数名称
     */
    public static String getConstructorName(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName() + "(" + StringUtils.append(getClassNames(method.getParameterTypes()), ", ") + ")";
    }

    /**
     * 获取构造Class 字段 名称
     *
     * @param field
     * @return / 字段 名称
     */
    public static String getFieldName(Field field) {
        return field.getDeclaringClass().getName() + "." + field.getName();
    }

    /**
     * 获取类的简名称
     *
     * @param scannerClass
     * @return / 简名称
     */
    public static String getClassSimpleName(Class<?> scannerClass) {
        return scannerClass.getSimpleName();
    }

}

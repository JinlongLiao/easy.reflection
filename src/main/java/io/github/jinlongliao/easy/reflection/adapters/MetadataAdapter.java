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

package io.github.jinlongliao.easy.reflection.adapters;

import io.github.jinlongliao.easy.reflection.util.StringUtils;
import io.github.jinlongliao.easy.reflection.vfs.Vfs;

import java.util.List;

/**
 * @author liaojinlong
 * @since 2020/6/18 20:48
 */
public interface MetadataAdapter<C, F, M> {
    String SUFFIX = ".class";

    /**
     * 获取Class 的名称
     *
     * @param cls class 信息
     * @return /
     */
    String getClassName(final C cls);

    /**
     * 获取Class 的父类名称
     *
     * @param cls class 信息
     * @return /
     */
    String getSuperclassName(final C cls);

    /**
     * 获取 此类的全部接口信息
     *
     * @param cls class
     * @return /
     */
    List<String> getInterfacesNames(final C cls);

    /**
     * 获取此类中的 Field 字段
     *
     * @param cls
     * @return /
     */
    List<F> getFields(final C cls);

    /**
     * 获取此类的 M 方法
     *
     * @param cls 类
     * @return /
     */
    List<M> getMethods(final C cls);

    /**
     * 依据 Method 获取 名称
     *
     * @param method
     * @return /
     */
    String getMethodName(final M method);

    /**
     * 获取此方法的 全部参数名称
     *
     * @param method
     * @return /
     */
    List<String> getParameterNames(final M method);

    /**
     * 获取类上的 注解信息
     *
     * @param aClass
     * @return /
     */
    List<String> getClassAnnotationNames(final C aClass);

    /**
     * 获取某些字段上的 注解信息
     *
     * @param field
     * @return /
     */
    List<String> getFieldAnnotationNames(final F field);

    /**
     * 获取方法上的注解信息
     *
     * @param method
     * @return /
     */
    List<String> getMethodAnnotationNames(final M method);

    /**
     * 获取参数上的注解信息
     *
     * @param method
     * @param parameterIndex
     * @return /
     */
    List<String> getParameterAnnotationNames(final M method, final int parameterIndex);

    /**
     * 获取此函数的 返回值类型名称
     *
     * @param method
     * @return /
     */
    String getReturnTypeName(final M method);

    /**
     * 依据字段 获取名称
     *
     * @param field
     * @return /
     */
    String getFieldName(final F field);

    /**
     * 依据{Vfs.File} 获取元数据文件
     *
     * @param file
     * @return /
     * @throws Exception
     */
    C getOrCreateClassObject(Vfs.File file) throws Exception;

    /**
     * 获取 方法的修饰类型
     *
     * @param method
     * @return /
     * @see {@link java.lang.reflect.Modifier}
     */
    String getMethodModifier(M method);


    /**
     * 判断是否公开类型
     *
     * @param o
     * @return /
     */
    boolean isPublic(Object o);

    /**
     * 方法描述 eg:
     * <code>
     * getMethodModifier(M method);
     * </code>
     *
     * @param cls
     * @param method
     * @return /
     */
    default String getMethodKey(C cls, M method) {
        return StringUtils.append(
                getMethodName(method),
                "(",
                StringUtils.append(getParameterNames(method), ", "),
                ")"
        );
    }


    /**
     * 方法全限定名称 eg:
     * <code>
     * org.reflections.adapters.MetadataAdapter.getMethodKey(java.lang.Object, java.lang.Object)
     * </code>
     *
     * @param cls
     * @param method
     * @return /
     */
    default String getMethodFullKey(C cls, M method) {
        return StringUtils.append(getClassName(cls), ".", getMethodKey(cls, method));
    }

    /**
     * 是否支持扫描
     *
     * @param file
     * @return 是否支持扫描
     */
    default boolean support(String file) {
        return file.endsWith(SUFFIX);
    }
}

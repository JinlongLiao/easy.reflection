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

package io.github.jinlongliao.easy.reflection.annotation;

import io.github.jinlongliao.easy.reflection.scanners.impl.*;
import io.github.jinlongliao.easy.reflection.scanners.impl.*;
import io.github.jinlongliao.easy.reflection.util.ConfigurationBuilder;

import io.github.jinlongliao.easy.reflection.Reflections;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author LiaoJL
 * @description TODO
 * @file AnnotationTest.java
 * @email jinlongliao@foxmail.com
 * @date 2020/4/20 16:42
 */
public class AnnotationTest {
    static Reflections reflections;

    @BeforeClass
    public static void init() {
        reflections = new Reflections(new ConfigurationBuilder()
                .forJavaPackages(AnnotationTest.class)
                .forPackages("package org.reflections.annotation;\n")
                .setScanners(
                        new SubTypesScanner(true),
                        new TypeAnnotationsScanner(),
                        new FieldAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner()));
    }

    /**
     * 根据@InterceptorPassController 获取不拦截路径
     */
    @Test
    public void getNotInterceptor() {
        List<String> notInterceptorList = new ArrayList<String>();
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(TestController.class,true);
        Set<Method> methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(TestController.class);
        for (Class<?> el : typesAnnotatedWith) {
            //取到标签对象
            TestController annotation = el.getAnnotation(TestController.class);
            notInterceptorList.add(annotation.value());
        }
        Assert.assertTrue(!notInterceptorList.isEmpty());
        for (Method el : methodsAnnotatedWith) {
            //取到标签对象
            TestController annotation = el.getAnnotation(TestController.class);
            notInterceptorList.add(annotation.value());
        }
        Assert.assertTrue(!notInterceptorList.isEmpty());
    }
}

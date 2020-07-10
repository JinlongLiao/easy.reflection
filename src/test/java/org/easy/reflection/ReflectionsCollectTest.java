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

package org.easy.reflection;

import org.easy.reflection.serializers.impl.JsonSerializer;
import org.easy.reflection.util.ClassUtils;
import org.easy.reflection.util.ClasspathHelper;
import org.easy.reflection.util.ConfigurationBuilder;
import org.easy.reflection.util.ScannerFilter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.easy.reflection.scanners.impl.MemberUsageScanner;
import org.easy.reflection.scanners.impl.MethodAnnotationsScanner;
import org.easy.reflection.scanners.impl.MethodParameterNamesScanner;
import org.easy.reflection.scanners.impl.MethodParameterScanner;
import org.easy.reflection.scanners.impl.ResourcesScanner;
import org.easy.reflection.scanners.impl.SubTypesScanner;
import org.easy.reflection.scanners.impl.TypeAnnotationsScanner;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.easy.reflection.ReflectionsTest.are;
import static org.easy.reflection.ReflectionsTest.getUserDir;
import static org.junit.Assert.assertThat;

/**
 *
 */
public class ReflectionsCollectTest {
    public static final ScannerFilter TestModelFilter = new ScannerFilter().include("org.easy.reflection.TestModel\\$.*");
    private static Reflections reflections;

    @BeforeClass
    public static void init() {
        Reflections ref = new Reflections(new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forClass(TestModel.class))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner()));

        ref.save(getUserDir() + "/target/test-classes" + "/META-INF/reflections/testModel-reflections.xml");

        ref = new Reflections(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new MethodParameterScanner()));

        final JsonSerializer serializer = new JsonSerializer();
        ref.save(getUserDir() + "/target/test-classes" + "/META-INF/reflections/testModel-reflections.json", serializer);

        reflections = Reflections
                .collect()
                .merge(Reflections.collect("META-INF/reflections",
                        new ScannerFilter().include(".*-reflections.json"),
                        serializer));
    }

    @Test
    public void testResourcesScanner() {
        Predicate<String> filter = new ScannerFilter().include(".*\\.xml").include(".*\\.json");
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(new ResourcesScanner())
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class))));

        Set<String> resolved = reflections.getResources(Pattern.compile(".*resource1-reflections\\.xml"));
        assertThat(resolved, are("META-INF/reflections/resource1-reflections.xml"));

        Set<String> resources = reflections.getStore().keys(ClassUtils.getClassSimpleName(ResourcesScanner.class));
        assertThat(resources, are("resource1-reflections.xml", "resource2-reflections.xml",
                "testModel-reflections.xml", "testModel-reflections.json"));
    }
}

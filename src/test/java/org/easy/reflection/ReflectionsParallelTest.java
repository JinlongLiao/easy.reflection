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

import org.easy.reflection.util.ClasspathHelper;
import org.easy.reflection.util.ConfigurationBuilder;
import org.junit.BeforeClass;
import org.easy.reflection.scanners.impl.FieldAnnotationsScanner;
import org.easy.reflection.scanners.impl.MemberUsageScanner;
import org.easy.reflection.scanners.impl.MethodAnnotationsScanner;
import org.easy.reflection.scanners.impl.MethodParameterNamesScanner;
import org.easy.reflection.scanners.impl.MethodParameterScanner;
import org.easy.reflection.scanners.impl.SubTypesScanner;
import org.easy.reflection.scanners.impl.TypeAnnotationsScanner;

import java.util.Collections;

/** */
public class ReflectionsParallelTest extends ReflectionsTest {

    @BeforeClass
    public static void init() {
        reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new FieldAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner())
                .useParallelExecutor());
    }
}

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

import org.easy.reflection.scanners.impl.TypeElementsScanner;
import org.easy.reflection.serializers.impl.JavaCodeSerializer;
import org.easy.reflection.util.ClasspathHelper;
import org.easy.reflection.util.ConfigurationBuilder;
import org.easy.reflection.util.ScannerFilter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class JavaCodeSerializerTest {

    @BeforeClass
    public static void generateAndSave() {
        Predicate<String> filter = new ScannerFilter().include("org.easy.reflection.TestModel\\$.*");

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(new TypeElementsScanner().includeFields().publicOnly(false))
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class))));

        //save
        String filename = ReflectionsTest.getUserDir() + "/src/test/java/org.easy.reflection.MyTestModelStore";
        reflections.save(filename, new JavaCodeSerializer());
    }

    @Test
    public void resolve() throws NoSuchMethodException, NoSuchFieldException {
        //class
        final String name = TestModel.C1.class.getName();
        assertEquals(TestModel.C1.class,
                JavaCodeSerializer.resolveClass(MyTestModelStore.org.easy.reflection.TestModel$C1.class));

        //method
        assertEquals(TestModel.C4.class.getDeclaredMethod("m1"),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.org.easy.reflection.TestModel$C4.methods.m1.class));

        //overloaded method with parameters
        assertEquals(TestModel.C4.class.getDeclaredMethod("m1", int.class, String[].class),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.org.easy.reflection.TestModel$C4.methods.m1_int__java_lang_String$$.class));

        //overloaded method with parameters and multi dimensional array
        assertEquals(TestModel.C4.class.getDeclaredMethod("m1", int[][].class, String[][].class),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.org.easy.reflection.TestModel$C4.methods.m1_int$$$$__java_lang_String$$$$.class));

        //field
        assertEquals(TestModel.C4.class.getDeclaredField("f1"),
                JavaCodeSerializer.resolveField(MyTestModelStore.org.easy.reflection.TestModel$C4.fields.f1.class));
    }
}
